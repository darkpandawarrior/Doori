package com.miletracker.core.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS local notifications via UNUserNotificationCenter (F), the counterpart to Android's
 * NotificationManager + channels. [ensurePermission] bridges requestAuthorization's completion handler
 * into a suspend call; [notify] posts an immediate ([trigger]=null) request keyed by the int id.
 */
class IosNotificationScheduler : NotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun ensurePermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ -> continuation.resume(granted) }
        }

    override fun notify(
        id: Int,
        title: String,
        body: String,
    ) {
        val content =
            UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
            }
        val request = UNNotificationRequest.requestWithIdentifier(id.toString(), content, null)
        center.addNotificationRequest(request, null)
    }

    override fun cancel(id: Int) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id.toString()))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id.toString()))
    }
}
