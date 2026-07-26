package com.mileway.core.platform

/**
 * iOS live presence surface for an active tracking session (P-D.2).
 *
 * Left as a documented no-op: the ActivityKit Live Activity + Dynamic Island are already driven
 * end-to-end from Swift. The `MilewayWidgets` extension registers `TrackingLiveActivity()` and the
 * host app's `TrackingLiveActivityController` starts/updates/ends the Activity off the same
 * `MilewaySyncPayload` the watch bridge pushes on every tracking change — so ActivityKit sees the
 * snapshot before it would ever reach shared Kotlin, and a second driver here could only fight it
 * for ownership of `Activity.request`. Kept as a no-op so the Koin binding + the
 * [TrackingPresenceController] contract stay uniform across platforms.
 */
class IosTrackingPresenceController : TrackingPresenceController {
    override fun start(snapshot: TrackingPresenceSnapshot) = Unit

    override fun update(snapshot: TrackingPresenceSnapshot) = Unit

    override fun stop() = Unit
}
