@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.mileway.feature.tracking.manager

import com.mileway.feature.tracking.detection.DetectedDriveRecorder
import com.mileway.feature.tracking.detection.DriveDeparture
import com.mileway.feature.tracking.detection.DriveStartSource
import com.mileway.feature.tracking.detection.IosDriveWakePolicy
import com.mileway.feature.tracking.detection.PendingDrive
import com.mileway.feature.tracking.detection.SignificantDriveFixPolicy
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationStateActive
import platform.darwin.NSObject

/** Lives with tracker wiring in tracking iosMain; restored SLC waits never request permission. */
class SignificantLocationSource(
    private val recorder: DetectedDriveRecorder,
) : NSObject(),
    CLLocationManagerDelegateProtocol,
    DriveStartSource {
    private val manager = CLLocationManager()
    private val defaults = NSUserDefaults.standardUserDefaults
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pending = defaults.stringForKey(PENDING)?.let { runCatching { Json.decodeFromString<PendingDrive>(it) }.getOrNull() }
    private val mutableWaiting = MutableStateFlow(false)
    override val waiting = mutableWaiting.asStateFlow()

    init {
        manager.delegate = this
        // Eager Koin creation reattaches the significant-change delegate on a location relaunch.
        if (pending != null && IosDriveWakePolicy.mayMonitorSignificantChanges(hasAlways())) beginMonitoring()
    }

    override suspend fun arm(
        drive: PendingDrive,
        departure: DriveDeparture?,
    ): Boolean =
        withContext(Dispatchers.Main) {
            if (UIApplication.sharedApplication.applicationState != UIApplicationStateActive || departure != null) {
                return@withContext false
            }
            val status = manager.authorizationStatus
            if (IosDriveWakePolicy.mayRequestAlways(true, status == kCLAuthorizationStatusAuthorizedWhenInUse)) {
                // The trip screen invokes arm while visible. SLC is never used to obtain Always.
                manager.requestAlwaysAuthorization()
                return@withContext false
            }
            if (!IosDriveWakePolicy.mayMonitorSignificantChanges(hasAlways()) ||
                !CLLocationManager.significantLocationChangeMonitoringAvailable()
            ) {
                return@withContext false
            }
            disarm()
            defaults.setObject(Json.encodeToString(drive), PENDING)
            defaults.setDouble(NSDate().timeIntervalSince1970, ARMED_AT)
            pending = drive
            beginMonitoring()
            true
        }

    private fun hasAlways(): Boolean = manager.authorizationStatus == kCLAuthorizationStatusAuthorizedAlways

    private fun beginMonitoring() {
        manager.startMonitoringSignificantLocationChanges()
        mutableWaiting.value = true
    }

    override suspend fun disarm() =
        withContext(Dispatchers.Main) {
            manager.stopMonitoringSignificantLocationChanges()
            pending = null
            defaults.removeObjectForKey(PENDING)
            defaults.removeObjectForKey(ARMED_AT)
            mutableWaiting.value = false
        }

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val drive = pending ?: return
        val fix = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        val timestamp = fix.timestamp.timeIntervalSince1970
        if (timestamp <= defaults.doubleForKey(ARMED_AT) ||
            !SignificantDriveFixPolicy.mayWake(
                hasAlways = hasAlways(),
                ageSeconds = NSDate().timeIntervalSince1970 - timestamp,
                accuracyMeters = fix.horizontalAccuracy,
                speedMetersPerSecond = fix.speed,
            )
        ) {
            return
        }
        // Clear synchronously before launching so a second callback cannot start another trip.
        pending = null
        scope.launch {
            disarm()
            runCatching { recorder.start(drive) }.onFailure {
                Napier.w("Significant-location recording failed", it, tag = "DriveWake")
            }
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        if (!hasAlways()) {
            manager.stopMonitoringSignificantLocationChanges()
            mutableWaiting.value = false
        }
    }

    private companion object {
        const val PENDING = "drive_wake_pending"
        const val ARMED_AT = "drive_wake_armed_at"
    }
}
