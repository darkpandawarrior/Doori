package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.first

/**
 * P-C.4: commonMain policy that classifies a DataStore session vs the Room source of truth,
 * producing one of three outcomes for the UI to act on at app launch.
 *
 * Call this off the main thread (Android: Application.onCreate; iOS: scene-active hook).
 */
class SessionReconciliationPolicy(
    private val currentTrackSource: CurrentTrackDataSource,
    private val savedTrackRepository: SavedTrackRepository,
) {
    sealed class Outcome {
        /** A clean, ongoing session — service should resume without user interaction. */
        data class Resume(val token: String, val session: CurrentTrackData) : Outcome()

        /** Session was interrupted (app-killed / FGS-terminated / shutdown) — show restore sheet. */
        data class NeedsDecision(val token: String, val session: CurrentTrackData, val reason: String) : Outcome()

        /** DataStore says "tracking" but the DB row is completed/discarded/missing — clear ghost. */
        data object DiscardStale : Outcome()

        /** No active session in DataStore. */
        data object NoSession : Outcome()
    }

    suspend fun reconcile(): Outcome {
        val session = currentTrackSource.currentTrackFlow.first()
        if (!session.isTracking || session.token.isEmpty()) return Outcome.NoSession

        val track = savedTrackRepository.getByRouteId(session.token)
        return when {
            track == null -> Outcome.DiscardStale
            track.isCompleted -> Outcome.DiscardStale
            track.isDiscarded -> Outcome.DiscardStale
            track.wasAppKilled || track.foregroundServiceTerminated || track.wasPhoneShutDown -> {
                val reason =
                    buildString {
                        if (track.wasAppKilled) append("app-kill ")
                        if (track.foregroundServiceTerminated) append("fgs-kill ")
                        if (track.wasPhoneShutDown) append("shutdown")
                    }.trim()
                Outcome.NeedsDecision(session.token, session, reason)
            }
            else -> Outcome.Resume(session.token, session)
        }
    }
}
