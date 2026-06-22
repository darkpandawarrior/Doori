package com.miletracker.core.platform

import android.app.Notification
import android.app.NotificationManager
import android.content.Context

private const val CHANNEL_ID = "miletracker_tracking"
private const val NOTIFICATION_ID = 1001

/**
 * P-D.2 Android: updates the ongoing foreground notification from the live [TrackingPresenceSnapshot].
 * The service owns the FGS notification lifecycle; this controller issues incremental notify() calls
 * so the system-tray live-tile always matches the in-app gauge without restarting the service.
 *
 * Constants mirror LocationTrackingConstants (in :feature:tracking) — kept as literals here to
 * avoid a circular dependency (core:platform ← feature:tracking).
 */
class AndroidTrackingPresenceController(private val context: Context) : TrackingPresenceController {
    private val notifManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun start(snapshot: TrackingPresenceSnapshot) = post(snapshot)

    override fun update(snapshot: TrackingPresenceSnapshot) = post(snapshot)

    override fun stop() {
        notifManager.cancel(NOTIFICATION_ID)
    }

    private fun post(snapshot: TrackingPresenceSnapshot) {
        val title = if (snapshot.isPaused) "Tracking paused" else "Tracking active"
        val text =
            if (snapshot.isPaused) {
                "${snapshot.distanceKm.fmt2()} km recorded · tap to resume"
            } else {
                "${snapshot.distanceKm.fmt2()} km · ${snapshot.speedKmh.toLong()} km/h · ${snapshot.activityLabel}"
            }
        val notification =
            Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(!snapshot.isPaused)
                .build()
        notifManager.notify(NOTIFICATION_ID, notification)
    }

    private fun Double.fmt2(): String {
        val scaled = (this * 100).toLong()
        val i = scaled / 100
        val f = (scaled % 100).let { if (it < 0) -it else it }
        return "$i.${f.toString().padStart(2, '0')}"
    }
}
