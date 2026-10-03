package com.mileway.feature.tracking.claim

import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.SavedTrack
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Observes persisted completions, including after restart, without depending on location or booking wiring. */
class AutoDraftOnTripCompleteUseCase(
    private val savedTrackDao: SavedTrackDao,
    private val reportRepository: ReportRepository,
    private val policyProvider: MileageClaimPolicyProvider,
) {
    private var observer: Job? = null

    /** Starts one application-scoped observer; duplicate startup calls are harmless. */
    fun start(scope: CoroutineScope) {
        if (observer?.isActive == true) return
        observer =
            scope.launch {
                savedTrackDao.getCompletedTracks().collect { tracks ->
                    for (track in tracks) {
                        runCatching { invoke(track) }.onFailure { failure ->
                            if (failure is CancellationException || failure !is Exception) throw failure
                            // The saved row stays pending for the next emission or app restart.
                            Napier.e("Mileage report auto-draft failed", failure, tag = "MileageAutoDraft")
                        }
                    }
                }
            }
    }

    /** Returns a new draft, or null if the row is ineligible or its trip was already imported. */
    suspend operator fun invoke(track: SavedTrack): Report? {
        if (reportRepository.hasSourceTrip(track.routeId)) return null
        val line = policyProvider.mapper().map(track) ?: return null
        val employeeId = track.startedByEmployeeCode.ifBlank { track.startedByAccountId.orEmpty() }
        return reportRepository.createMileageDraft(employeeId, line)
    }
}
