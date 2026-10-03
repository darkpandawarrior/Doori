package com.mileway

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mileway.feature.tracking.detection.DetectedDriveRecorder
import com.mileway.feature.tracking.detection.DriveDeparture
import com.mileway.feature.tracking.detection.DriveStartSource
import com.mileway.feature.tracking.detection.DriveTransition
import com.mileway.feature.tracking.detection.DriveTransitionState
import com.mileway.feature.tracking.detection.PendingDrive
import com.mileway.feature.tracking.detection.TransitionDriveDetector
import com.siddharth.kmp.appshell.AppPermission
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
private data class PendingWake(
    val drive: PendingDrive,
    val detector: DriveTransitionState,
)

/** GMS idle wait: OS PendingIntents only. GPS and the FGS begin after a vehicle transition. */
class GmsDriveStartSource(
    private val context: Context,
    private val recorder: DetectedDriveRecorder,
) : DriveStartSource {
    private val preferences = context.getSharedPreferences("drive_transition_wait", Context.MODE_PRIVATE)
    private val activityClient = ActivityRecognition.getClient(context)
    private val geofenceClient = LocationServices.getGeofencingClient(context)
    private val mutex = Mutex()
    private val mutableWaiting = MutableStateFlow(readPending() != null)
    override val waiting = mutableWaiting.asStateFlow()
    override val requiredPermissions = listOf(AppPermission.LOCATION, AppPermission.LOCATION_BACKGROUND, AppPermission.ACTIVITY_RECOGNITION)

    private fun pendingIntent(action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GmsDriveTransitionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    private fun hasPermission(permission: String): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    override suspend fun arm(
        drive: PendingDrive,
        departure: DriveDeparture?,
    ): Boolean =
        mutex.withLock {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        (
                            !hasPermission(Manifest.permission.ACTIVITY_RECOGNITION) ||
                                !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        )
                )
            ) {
                return@withLock false
            }
            clearWait()
            val transitions =
                listOf(ActivityTransition.ACTIVITY_TRANSITION_ENTER, ActivityTransition.ACTIVITY_TRANSITION_EXIT).map {
                    ActivityTransition
                        .Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(it)
                        .build()
                }
            val state = DriveTransitionState(requiresDeparture = departure != null, armedAtNanos = SystemClock.elapsedRealtimeNanos())
            savePending(PendingWake(drive, state))
            val result =
                runCatching {
                    activityClient.requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent(TRANSITION)).await()
                    if (departure != null) registerDeparture(drive.routeId, departure)
                }
            if (result.isFailure) {
                clearWait()
                Napier.w("Unable to arm drive transitions", result.exceptionOrNull(), tag = TAG)
                return@withLock false
            }
            mutableWaiting.value = true
            true
        }

    private suspend fun registerDeparture(
        token: String,
        departure: DriveDeparture,
    ) {
        val fence =
            Geofence
                .Builder()
                .setRequestId(token)
                .setCircularRegion(departure.latitude, departure.longitude, departure.radiusMeters)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        val request =
            GeofencingRequest
                .Builder()
                .setInitialTrigger(0)
                .addGeofence(fence)
                .build()
        geofenceClient.addGeofences(request, pendingIntent(DEPARTURE)).await()
    }

    /** Called by the manifest receiver, including when Play Services recreates this process. */
    suspend fun receive(intent: Intent) =
        mutex.withLock {
            val pending = readPending() ?: return@withLock
            val detector = TransitionDriveDetector(pending.detector)
            val shouldStart =
                when (intent.action) {
                    TRANSITION -> {
                        val result = ActivityTransitionResult.extractResult(intent) ?: return@withLock
                        val events =
                            result.transitionEvents.filter { it.activityType == DetectedActivity.IN_VEHICLE }.map { event ->
                                val transition =
                                    if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                        DriveTransition.VEHICLE_ENTER
                                    } else {
                                        DriveTransition.VEHICLE_EXIT
                                    }
                                transition to event.elapsedRealTimeNanos
                            }
                        detector.acceptVehicleTransitions(events)
                    }
                    DEPARTURE -> {
                        val event = GeofencingEvent.fromIntent(intent) ?: return@withLock
                        if (event.hasError() ||
                            event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT ||
                            event.triggeringGeofences?.none { it.requestId == pending.drive.routeId } != false
                        ) {
                            return@withLock
                        }
                        detector.accept(DriveTransition.DEPARTURE, SystemClock.elapsedRealtimeNanos())
                    }
                    else -> return@withLock
                }
            if (shouldStart) {
                consumePending()
                try {
                    // Start promptly while the transition's background-FGS exemption is still valid.
                    recorder.start(pending.drive)
                } finally {
                    clearWait()
                }
            } else {
                savePending(pending.copy(detector = detector.state))
            }
        }

    override suspend fun disarm() = mutex.withLock { clearWait() }

    private fun savePending(pending: PendingWake) {
        check(preferences.edit().putString(PENDING, Json.encodeToString(pending)).commit())
    }

    private fun readPending(): PendingWake? {
        val payload = preferences.getString(PENDING, null) ?: return null
        val pending = runCatching { Json.decodeFromString<PendingWake>(payload) }.getOrNull() ?: return null
        // Monotonic timestamps reset on reboot. Old registrations must be armed again from the screen.
        return pending.takeIf { it.detector.armedAtNanos <= SystemClock.elapsedRealtimeNanos() }
    }

    private fun consumePending() {
        check(preferences.edit().remove(PENDING).commit())
        mutableWaiting.value = false
    }

    private suspend fun clearWait() {
        consumePending()
        runCatching { activityClient.removeActivityTransitionUpdates(pendingIntent(TRANSITION)).await() }
        runCatching { geofenceClient.removeGeofences(pendingIntent(DEPARTURE)).await() }
    }

    private companion object {
        const val TAG = "DriveTransitions"
        const val PENDING = "pending"
        const val TRANSITION = "com.mileway.DRIVE_TRANSITION"
        const val DEPARTURE = "com.mileway.DRIVE_DEPARTURE"
    }
}

/** Explicit non-exported PendingIntent target. Waiting never launches a service. */
class GmsDriveTransitionReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val source: GmsDriveStartSource by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                source.receive(intent)
            } catch (exception: ApiException) {
                Napier.w("Drive transition delivery failed", exception, tag = "DriveTransitions")
            } catch (exception: IllegalStateException) {
                Napier.w("Drive recording start refused", exception, tag = "DriveTransitions")
            } catch (exception: SecurityException) {
                Napier.w("Drive recording permission missing", exception, tag = "DriveTransitions")
            } finally {
                result.finish()
            }
        }
    }
}
