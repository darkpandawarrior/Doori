package com.miletracker.core.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * FCM.3 — centralized notification channels (multi-channel model). Created once and
 * referenced by [AndroidNotificationScheduler] and the FCM service so every notification lands on a
 * consistent, user-controllable channel.
 */
object NotificationChannels {
    const val URGENT = "miletracker_urgent"
    const val TRACKING = "miletracker_tracking"
    const val GENERAL = "miletracker_general"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        create(manager, URGENT, "Urgent", NotificationManager.IMPORTANCE_HIGH)
        create(manager, TRACKING, "Trip tracking", NotificationManager.IMPORTANCE_LOW)
        create(manager, GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT)
    }

    private fun create(
        manager: NotificationManager,
        id: String,
        name: String,
        importance: Int,
    ) {
        if (manager.getNotificationChannel(id) == null) {
            manager.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }
}
