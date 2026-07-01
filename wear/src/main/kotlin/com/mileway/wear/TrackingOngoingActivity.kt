package com.mileway.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

object TrackingOngoingActivity {

    private const val CHANNEL_ID = "mileway_tracking"
    private const val NOTIFICATION_ID = 1001

    fun buildNotification(context: Context, distanceKm: Double): Notification {
        ensureChannel(context)

        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_MAIN).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val status = Status.Builder()
            .addTemplate("${"%.2f".format(distanceKm)} km tracked")
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Tracking active")
            .setContentText("${"%.2f".format(distanceKm)} km")
            .setOngoing(true)
            .setContentIntent(tapIntent)

        OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
            .setStaticIcon(android.R.drawable.ic_media_play)
            .setTouchIntent(tapIntent)
            .setStatus(status)
            .build()
            .apply(context)

        return builder.build()
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Tracking", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }
}
