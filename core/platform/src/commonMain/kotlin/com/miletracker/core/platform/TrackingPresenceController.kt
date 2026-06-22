package com.miletracker.core.platform

/**
 * P-D.2: platform-specific live presence surface for an active tracking session.
 *
 * Android: updates an ongoing foreground notification (wraps [TrackingNotificationContent])
 * and keeps the floating bubble in sync.
 * iOS: starts / updates / ends an ActivityKit Live Activity + Dynamic Island.
 *
 * Both platforms show the shared in-app [TrackingMiniPill] CMP overlay while the app is in
 * the foreground; the platform presence surface is the out-of-app representation.
 */
interface TrackingPresenceController {
    /** Called when a new tracking session begins. [snapshot] carries the initial state. */
    fun start(snapshot: TrackingPresenceSnapshot)

    /** Called on every significant state change (distance tick, pause/resume, speed update). */
    fun update(snapshot: TrackingPresenceSnapshot)

    /** Called when the session ends (trip complete, discard, or stop). */
    fun stop()
}

/**
 * Platform-neutral summary of the tracking session's live state — feeds both the ongoing
 * notification copy and the ActivityKit Live Activity attributes.
 */
data class TrackingPresenceSnapshot(
    val distanceKm: Double,
    val durationMs: Long,
    val speedKmh: Double,
    val activityLabel: String,
    val isPaused: Boolean,
    /** P-D.3: non-null during the auto-discard countdown; drives the notification/Live Activity chip. */
    val autoDiscardRemainingMs: Long? = null,
)
