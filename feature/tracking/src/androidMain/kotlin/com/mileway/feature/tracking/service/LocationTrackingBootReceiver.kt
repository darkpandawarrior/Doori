package com.mileway.feature.tracking.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.feature.tracking.TrackMilesActivity
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Resumes an interrupted tracking session after a reboot or app update.
 *
 * FGS-from-boot rules (verified against Android 16 / API 36): the `location` type is NOT in
 * the list of types banned from BOOT_COMPLETED receivers (that list: dataSync, camera,
 * mediaPlayback, phoneCall, mediaProjection, microphone), so auto-restore is allowed, but
 * only useful when ACCESS_BACKGROUND_LOCATION is granted, because a location FGS started
 * from the background falls under while-in-use restrictions. [BootRestorePolicy] gates on
 * that. If a future Android release does ban location FGS from boot, the
 * startForegroundService call below will throw and we fall back to a notification the user
 * taps to resume manually.
 */
class LocationTrackingBootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TrackingBootReceiver"
        private const val RESUME_NOTIFICATION_ID = 1002
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                // Credential-protected storage (our DataStore) is unavailable before first
                // unlock; BOOT_COMPLETED arrives after unlock and handles the restore.
                Napier.d("Locked boot: deferring session check to BOOT_COMPLETED", tag = TAG)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                Napier.i("Boot/update received: checking for active session", tag = TAG)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        checkAndRestore(context)
                    } catch (e: Exception) {
                        Napier.e("Error checking session on boot", e, tag = TAG)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun checkAndRestore(context: Context) {
        val dataStore = CurrentTrackDataStore(context)
        val session = dataStore.currentTrackFlow.first()
        val action =
            BootRestorePolicy.decide(
                session = session,
                hasFineLocation = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
                hasBackgroundLocation = context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            )
        when (action) {
            BootRestoreAction.RESUME_SERVICE -> {
                Napier.i("Active session found (${session.token.take(8)}…): restarting service", tag = TAG)
                val serviceIntent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        this.action = LocationTrackingService.ACTION_RESTORE
                        putExtra(LocationTrackingService.EXTRA_TOKEN, session.token)
                    }
                try {
                    context.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    // ForegroundServiceStartNotAllowedException (or similar): the OS refused
                    // the background FGS start. Let the user resume from a notification.
                    Napier.w("FGS start from boot disallowed: offering manual resume", e, tag = TAG)
                    postManualResumeNotification(context)
                }
            }
            BootRestoreAction.CLEAR_STALE_SESSION -> {
                Napier.i("Session not resumable: marking it stopped", tag = TAG)
                dataStore.saveSession(
                    session.copy(
                        isTracking = false,
                        isPaused = false,
                        endTime = System.currentTimeMillis(),
                    ),
                )
            }
            BootRestoreAction.NONE -> Napier.d("No active session: nothing to restore", tag = TAG)
        }
    }

    private fun Context.hasPermission(permission: String): Boolean = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    /** Lets the user reopen the app and resume the trip when auto-restore isn't allowed. */
    private fun postManualResumeNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                LocationTrackingConstants.NOTIFICATION_CHANNEL_ID,
                LocationTrackingConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val openIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, TrackMilesActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            Notification.Builder(context, LocationTrackingConstants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Trip tracking was interrupted")
                .setContentText("Tap to reopen Mileway and resume your trip")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .build()
        manager.notify(RESUME_NOTIFICATION_ID, notification)
    }
}
