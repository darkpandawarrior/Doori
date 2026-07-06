package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.SavedTrack

/** Wave-4 §2.1: per-session validation outcome for the multi-session restore list. */
enum class SessionValidationStatus {
    /** Ownership pointer matches the currently-active persona — safe to restore directly. */
    VALID,

    /** Ownership pointer belongs to a different persona than the one signed in now. */
    OWNER_MISMATCH,

    /** Journey has zero recorded distance/duration — likely stale or a false start. */
    EMPTY,
}

/** One row in the multi-session restore list. */
data class RestorableSession(
    val routeId: String,
    val distanceKm: Double,
    val durationMs: Long,
    val startedAtMs: Long,
    val isDraft: Boolean,
    val status: SessionValidationStatus,
)

/**
 * Wave-4 §2.1: gathers every LOCAL restorable session — local-multi first, no server session API
 * (see class doc on [com.mileway.feature.tracking.viewmodel.MultiSessionRestoreViewModel]).
 *
 * Pure function over already-fetched local sources so it's trivially testable:
 * - [inProgressTracks] — every non-completed [SavedTrack] row (ongoing or paused), from Room.
 * - [activeAccountId] — the persona currently signed in (`null` if unknown), from DataStore.
 *
 * A session validates as [SessionValidationStatus.OWNER_MISMATCH] when its `started_by_account_id`
 * doesn't match [activeAccountId], and [SessionValidationStatus.EMPTY] when it has no recorded
 * distance or duration yet (nothing meaningful to restore), otherwise [SessionValidationStatus.VALID].
 * Newest-first by [RestorableSession.startedAtMs].
 */
object RestorableSessionsGatherer {
    fun gather(
        inProgressTracks: List<SavedTrack>,
        activeAccountId: String?,
    ): List<RestorableSession> =
        inProgressTracks
            .filter { !it.isCompleted && !it.isDiscarded }
            .map { track ->
                val status =
                    when {
                        track.startedByAccountId != null && track.startedByAccountId != activeAccountId ->
                            SessionValidationStatus.OWNER_MISMATCH
                        track.distance <= 0.0 && track.duration <= 0L -> SessionValidationStatus.EMPTY
                        else -> SessionValidationStatus.VALID
                    }
                RestorableSession(
                    routeId = track.routeId,
                    distanceKm = track.distance / 1000.0,
                    durationMs = track.duration,
                    startedAtMs = track.startedAtTimestamp,
                    isDraft = track.isDraft,
                    status = status,
                )
            }
            .sortedByDescending { it.startedAtMs }
}
