package com.mileway.feature.tracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mileway.feature.tracking.TrackMilesActivity

class TrackingNotificationManager(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel =
            NotificationChannel(
                LocationTrackingConstants.NOTIFICATION_CHANNEL_ID,
                LocationTrackingConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows live GPS tracking progress"
                setShowBadge(false)
            }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildInitialNotification(): Notification =
        buildNotification(
            title = "Mileway Active",
            text = "Starting GPS tracking…",
            distanceKm = 0.0,
            speedKmh = 0.0,
            isPaused = false,
        )

    fun buildTrackingNotification(
        distanceKm: Double,
        speedKmh: Double,
        isPaused: Boolean,
    ): Notification =
        buildNotification(
            title = if (isPaused) "Tracking Paused" else "Tracking Active",
            text =
                if (isPaused) {
                    "%.2f km recorded: tap to resume".format(distanceKm)
                } else {
                    "%.2f km  ·  %.1f km/h".format(distanceKm, speedKmh)
                },
            distanceKm = distanceKm,
            speedKmh = speedKmh,
            isPaused = isPaused,
        )

    fun buildPausedNotification(distanceKm: Double): Notification =
        buildNotification(
            title = "Tracking Paused",
            text = "%.2f km recorded: tap to resume".format(distanceKm),
            distanceKm = distanceKm,
            speedKmh = 0.0,
            isPaused = true,
        )

    fun update(notification: Notification) {
        notificationManager.notify(LocationTrackingConstants.NOTIFICATION_ID, notification)
    }

    fun cancel() {
        notificationManager.cancel(LocationTrackingConstants.NOTIFICATION_ID)
    }

    private fun buildNotification(
        title: String,
        text: String,
        distanceKm: Double,
        speedKmh: Double,
        isPaused: Boolean,
    ): Notification {
        val openIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, TrackMilesActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val pauseResumeIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                Intent(if (isPaused) LocationTrackingConstants.ACTION_RESUME else LocationTrackingConstants.ACTION_PAUSE)
                    .setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent =
            PendingIntent.getBroadcast(
                context,
                2,
                Intent(LocationTrackingConstants.ACTION_STOP).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return Notification.Builder(context, LocationTrackingConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openIntent)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_pause),
                    if (isPaused) "Resume" else "Pause",
                    pauseResumeIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_delete),
                    "Stop",
                    stopIntent,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
