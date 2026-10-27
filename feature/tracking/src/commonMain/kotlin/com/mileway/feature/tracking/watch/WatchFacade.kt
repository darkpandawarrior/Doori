package com.mileway.feature.tracking.watch

import com.mileway.core.data.model.display.SnapshotPublisher
import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * P1.2: a small, watchos-safe value model for a single completed trip — the subset a watch trip
 * list/detail screen renders. Deliberately pure (no [com.mileway.core.data.model.db.SavedTrack]
 * Room entity, no reference to the phone's richer [com.mileway.core.data.model.display.TrackDisplayData]
 * fields the watch never shows), so it stays cheap over DataLayer/WCSession later if trip lists are
 * ever synced (not in this task's scope — today it's read straight off the phone's own Room via
 * [SavedTrackRepository]).
 */
data class TripSummary(
    val id: String,
    val label: String,
    val km: Double,
    val endMs: Long,
)

/**
 * The single facade both watch UIs (Wear OS Compose, and — via the headless KMP framework — Swift/
 * watchOS) bind to. Deliberately thin: it delegates to the phone's EXISTING [SnapshotPublisher] (the
 * already-shared, already-tested [SurfaceSnapshot] channel — see `core:data`'s `SnapshotPublisher.kt`),
 * [SavedTrackRepository] (read-only here) and [TrackingController] (start/stop), and exposes nothing
 * but plain value types across its public API.
 *
 * Lives in `feature:tracking` (not `core:data`, despite the plan's original file hint) because both
 * of its non-snapshot dependencies — [SavedTrackRepository] and [TrackingController] — are
 * `feature:tracking` types; `core:data` cannot depend on a feature module (feature modules depend on
 * `core:*`, never the reverse — see the project guidelines "Architecture discipline"). `:wear` already depends on
 * `feature:tracking` (P2.1), so this is the correct, boundary-respecting home for the seam.
 */
class WatchFacade(
    private val snapshotPublisher: SnapshotPublisher,
    private val trackRepository: SavedTrackRepository,
    private val trackingController: TrackingController,
) {
    /** Live [SurfaceSnapshot] stream — the same one the phone's Glance/WidgetKit surfaces render. */
    fun observeSnapshot(): Flow<SurfaceSnapshot> = snapshotPublisher.snapshot

    /** The most recent completed trips, newest first, capped at [limit]. */
    fun recentTrips(limit: Int): Flow<List<TripSummary>> =
        trackRepository.completedTracksFlow().map { tracks ->
            tracks
                .sortedByDescending { it.endTime }
                .take(limit)
                .map { it.toTripSummary() }
        }

    /**
     * Starts a new tracking session for a watch-initiated trip. Unlike the phone's
     * `TrackMilesViewModel.startTracking()`, there is no vehicle-selection step or `SavedTrack`
     * insert here — the watch has no vehicle picker UI, and generating a full "route" row belongs
     * to a dedicated watch-initiated-trip task, not this facade seam. This proxies to
     * [TrackingController.start] only, as specified by P1.2's acceptance, generating its own
     * session token the same way the phone's `TrackMilesViewModel` does.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun startTracking() = trackingController.start(Uuid.random().toString())

    /** Stops the currently active session, resolved from [SavedTrackRepository]'s active track. */
    suspend fun stopTracking() {
        trackRepository.getActiveTrack()?.let { trackingController.stop(it.routeId) }
    }

    /**
     * DL.5: pauses the currently active session, resolved from [SavedTrackRepository]'s active
     * track — the deep-link/App-Intent surface's Pause op proxies here rather than duplicating
     * the token lookup.
     */
    suspend fun pauseTracking() {
        trackRepository.getActiveTrack()?.let { trackingController.pause(it.routeId) }
    }

    /**
     * DL.5: stops and discards the currently active session. `TrackingController` has no separate
     * discard op (matching `TrackMilesViewModel.discardTracking()`, which also just calls
     * [TrackingController.stop] and drops the in-memory state) — the "discard" semantics are that
     * the caller never finalizes/submits the trip, unlike a normal stop.
     */
    suspend fun discardTracking() {
        trackRepository.getActiveTrack()?.let { trackingController.stop(it.routeId) }
    }
}

private fun TrackDisplayData.toTripSummary() =
    TripSummary(
        id = token,
        label = name.orEmpty(),
        km = distanceKm,
        endMs = endTime,
    )
