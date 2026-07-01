package com.mileway.core.platform

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Android [NotificationScheduler] backed by [NotificationManagerCompat] + the shared [NotificationChannels].
 *
 * [ensurePermission] only *checks* whether notifications are enabled (POST_NOTIFICATIONS on API 33+);
 * actually requesting the runtime permission is [PermissionsProvider]'s job, since it needs an Activity.
 */
class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {
    init {
        NotificationChannels.ensureChannels(context)
    }

    override suspend fun ensurePermission(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun notify(
        id: Int,
        title: String,
        body: String,
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val notification =
            NotificationCompat.Builder(context, NotificationChannels.GENERAL)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Notifications not granted; silently no-op.
        }
    }

    override fun cancel(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
