package com.miletracker.platform.gms

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.miletracker.core.platform.NotificationChannels
import com.miletracker.core.platform.PushTokenStore
import org.koin.mp.KoinPlatform

/**
 * FCM.2 — Android Firebase messaging service (gms flavor only).
 *
 * - [onNewToken] → the shared [PushTokenStore] (the app + future backend read it from there).
 * - [onMessageReceived] handles both `notification` and data-only payloads, builds a notification, and maps
 *   `data["path"]` to a deep link so the tap opens the right screen via [com.miletracker.LauncherActivity]
 *   (which routes through the shared DeepLinkRouter). PendingIntent uses FLAG_IMMUTABLE (API 31+ requirement).
 */
class MileTrackerFirebaseMessagingService : FirebaseMessagingService() {
    private val tokenStore: PushTokenStore? by lazy {
        runCatching { KoinPlatform.getKoin().getOrNull<PushTokenStore>() }.getOrNull()
    }

    override fun onNewToken(token: String) {
        tokenStore?.setToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Mileway"
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        showNotification(title, body, message.data["path"])
    }

    private fun showNotification(
        title: String,
        body: String,
        path: String?,
    ) {
        NotificationChannels.ensureChannels(this)
        val uri = path?.let { if (it.contains("://")) it else "miletracker://$it" }
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setPackage(packageName)
                if (uri != null) data = Uri.parse(uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pending =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat.Builder(this, NotificationChannels.GENERAL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(NOTIF_ID, notification)
        }
    }

    private companion object {
        const val NOTIF_ID = 2001
    }
}
