package com.miletracker.feature.tracking.service

import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.display.TrackingState
import kotlin.math.roundToLong

/** C.2d: the seven foreground-notification kinds the tracking session can surface. */
enum class TrackingNotificationType {
    ACTIVE,
    PAUSED,
    GPS_DISABLED,
    PERMISSION_MISSING,
    AUTO_DISCARD,
    POLICY_VIOLATION,
    TRIP_COMPLETE,
}

/** P-D.1: interactive notification action buttons available on each notification type. */
enum class TrackingNotificationAction {
    PAUSE,
    RESUME,
    STOP,
    FIX_GPS,
}

/**
 * Platform-neutral notification copy + behaviour. [ongoing] = a sticky foreground notice (live states);
 * terminal ones (TRIP_COMPLETE, AUTO_DISCARD) are dismissible. [deepLink] is set for taps that should
 * route somewhere specific. [actions] lists the action buttons shown on the notification.
 */
data class TrackingNotificationContent(
    val type: TrackingNotificationType,
    val title: String,
    val text: String,
    val ongoing: Boolean,
    val deepLink: String? = null,
    val actions: List<TrackingNotificationAction> = emptyList(),
)

/**
 * C.2d: pure mapper from the live [TrackingSnapshot] to notification copy — the seven phase-aware messages
 * the foreground service shows. No Android dependency, so it is fully JVM-unit-testable; the service just
 * renders the result. Priority order is "most actionable first": completion → blocking issues → paused → live.
 */
object TrackingNotificationMapper {
    /** miletracker://track resolves cleanly to the tracking section (DeepLinkRouter DL.1). */
    const val TRACK_DEEP_LINK = "miletracker://track"

    fun fromSnapshot(snapshot: TrackingSnapshot): TrackingNotificationContent {
        val km = snapshot.distanceMeters / 1000.0
        val kmh = snapshot.speedMps * 3.6
        val flags = snapshot.systemFlags
        return when {
            snapshot.state == TrackingState.COMPLETED ->
                content(
                    TrackingNotificationType.TRIP_COMPLETE,
                    "Journey complete",
                    "${km.fmt2()} km recorded · tap to review",
                    ongoing = false,
                    deepLink = TRACK_DEEP_LINK,
                )
            flags.permissionMissing || snapshot.lastEvent == EventType.PERMISSION_DENIED ->
                content(
                    TrackingNotificationType.PERMISSION_MISSING,
                    "Location permission needed",
                    "Tracking can't continue without location access",
                    ongoing = true,
                    actions = listOf(TrackingNotificationAction.STOP),
                )
            snapshot.lastEvent == EventType.PERMISSION_VIOLATED ->
                content(
                    TrackingNotificationType.POLICY_VIOLATION,
                    "Tracking policy violation",
                    "A required permission was revoked mid-trip",
                    ongoing = true,
                    actions = listOf(TrackingNotificationAction.STOP),
                )
            flags.gpsDisabled || !snapshot.isGpsAvailable || snapshot.lastEvent == EventType.GPS_LOST ->
                content(
                    TrackingNotificationType.GPS_DISABLED,
                    "GPS unavailable",
                    "Waiting for a GPS signal · ${km.fmt2()} km",
                    ongoing = true,
                    actions = listOf(TrackingNotificationAction.FIX_GPS, TrackingNotificationAction.STOP),
                )
            snapshot.state == TrackingState.PAUSED ->
                content(
                    TrackingNotificationType.PAUSED,
                    "Tracking paused",
                    "${km.fmt2()} km recorded · tap to resume",
                    ongoing = true,
                    actions = listOf(TrackingNotificationAction.RESUME, TrackingNotificationAction.STOP),
                )
            else ->
                content(
                    TrackingNotificationType.ACTIVE,
                    "Tracking active",
                    "${km.fmt2()} km · ${kmh.roundToLong()} km/h",
                    ongoing = true,
                    actions = listOf(TrackingNotificationAction.PAUSE, TrackingNotificationAction.STOP),
                )
        }
    }

    /** Two-decimal string for a Double — KMP-safe, avoids JVM-only String.format. */
    private fun Double.fmt2(): String {
        val scaled = (this * 100).roundToLong()
        val intPart = scaled / 100
        val fracPart = (scaled % 100).let { if (it < 0) -it else it }
        return "$intPart.${fracPart.toString().padStart(2, '0')}"
    }

    /** Standalone copy for the worker-driven auto-discard notice (not derivable from a live snapshot). */
    fun autoDiscard(): TrackingNotificationContent =
        content(
            TrackingNotificationType.AUTO_DISCARD,
            "Incomplete trip cleaned up",
            "An abandoned trip was auto-discarded to keep your history tidy",
            ongoing = false,
            deepLink = TRACK_DEEP_LINK,
        )

    private fun content(
        type: TrackingNotificationType,
        title: String,
        text: String,
        ongoing: Boolean,
        deepLink: String? = null,
        actions: List<TrackingNotificationAction> = emptyList(),
    ) = TrackingNotificationContent(type, title, text, ongoing, deepLink, actions)
}
