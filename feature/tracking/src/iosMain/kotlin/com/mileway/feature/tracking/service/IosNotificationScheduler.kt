package com.mileway.feature.tracking.service

import io.github.aakira.napier.Napier
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionAuthenticationRequired
import platform.UserNotifications.UNNotificationActionOptionDestructive
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

private const val TAG = "IosNotifScheduler"
private const val TRACKING_NOTIF_ID = "mileway_tracking"
private const val CATEGORY_ACTIVE = "TRACKING_ACTIVE"
private const val CATEGORY_PAUSED = "TRACKING_PAUSED"
private const val CATEGORY_GPS_DISABLED = "TRACKING_GPS"
private const val CATEGORY_TERMINAL = "TRACKING_TERMINAL"

/**
 * P-D.1: iOS delivery layer. Registers UNUserNotificationCenter categories/actions on startup and
 * schedules/updates the live tracking notification from [TrackingNotificationContent].
 *
 * Swift/Xcode step (manual): enable the "Push Notifications" + "Background Modes" (Remote notifications)
 * capabilities in Xcode → Signing & Capabilities. Add AppDelegate.userNotificationCenter(_:didReceive:)
 * to forward tapped action identifiers to KMP via [handleNotificationAction].
 */
class IosNotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    fun requestPermissionAndRegisterCategories() {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, error ->
            if (error != null) Napier.w("Notification permission denied: $error", tag = TAG)
        }
        center.setNotificationCategories(buildCategories())
    }

    fun schedule(content: TrackingNotificationContent) {
        val notifContent = UNMutableNotificationContent()
        notifContent.setTitle(content.title)
        notifContent.setBody(content.text)
        notifContent.setCategoryIdentifier(categoryFor(content.type))
        val request =
            UNNotificationRequest.requestWithIdentifier(
                TRACKING_NOTIF_ID,
                notifContent,
                null,
            )
        center.addNotificationRequest(request) { error ->
            if (error != null) Napier.e("Failed to schedule notification: $error", tag = TAG)
        }
    }

    fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(TRACKING_NOTIF_ID))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(TRACKING_NOTIF_ID))
    }

    /** Forward from AppDelegate.userNotificationCenter(_:didReceive:withCompletionHandler:). */
    fun handleNotificationAction(
        actionIdentifier: String,
        onAction: (TrackingNotificationAction) -> Unit,
    ) {
        when (actionIdentifier) {
            "PAUSE" -> onAction(TrackingNotificationAction.PAUSE)
            "RESUME" -> onAction(TrackingNotificationAction.RESUME)
            "STOP" -> onAction(TrackingNotificationAction.STOP)
            "FIX_GPS" -> onAction(TrackingNotificationAction.FIX_GPS)
        }
    }

    private fun categoryFor(type: TrackingNotificationType): String =
        when (type) {
            TrackingNotificationType.ACTIVE -> CATEGORY_ACTIVE
            TrackingNotificationType.PAUSED -> CATEGORY_PAUSED
            TrackingNotificationType.GPS_DISABLED -> CATEGORY_GPS_DISABLED
            else -> CATEGORY_TERMINAL
        }

    private fun buildCategories(): Set<*> {
        val pauseAction =
            UNNotificationAction.actionWithIdentifier(
                "PAUSE",
                "Pause",
                UNNotificationActionOptionNone,
            )
        val resumeAction =
            UNNotificationAction.actionWithIdentifier(
                "RESUME",
                "Resume",
                UNNotificationActionOptionNone,
            )
        val stopAction =
            UNNotificationAction.actionWithIdentifier(
                "STOP",
                "Stop",
                UNNotificationActionOptionDestructive,
            )
        val fixGpsAction =
            UNNotificationAction.actionWithIdentifier(
                "FIX_GPS",
                "Fix GPS",
                UNNotificationActionOptionAuthenticationRequired,
            )
        val activeCategory =
            UNNotificationCategory.categoryWithIdentifier(
                CATEGORY_ACTIVE,
                listOf(pauseAction, stopAction),
                emptyList<Any>(),
                UNNotificationCategoryOptionNone,
            )
        val pausedCategory =
            UNNotificationCategory.categoryWithIdentifier(
                CATEGORY_PAUSED,
                listOf(resumeAction, stopAction),
                emptyList<Any>(),
                UNNotificationCategoryOptionNone,
            )
        val gpsCategory =
            UNNotificationCategory.categoryWithIdentifier(
                CATEGORY_GPS_DISABLED,
                listOf(fixGpsAction, stopAction),
                emptyList<Any>(),
                UNNotificationCategoryOptionNone,
            )
        val terminalCategory =
            UNNotificationCategory.categoryWithIdentifier(
                CATEGORY_TERMINAL,
                emptyList<Any>(),
                emptyList<Any>(),
                UNNotificationCategoryOptionNone,
            )
        return setOf(activeCategory, pausedCategory, gpsCategory, terminalCategory)
    }
}
