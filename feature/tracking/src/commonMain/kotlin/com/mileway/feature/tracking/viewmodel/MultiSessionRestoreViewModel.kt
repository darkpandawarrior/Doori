package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.service.RestorableSession
import com.mileway.feature.tracking.service.RestorableSessionsGatherer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Wave-4 §2.1: local-multi session restore — lists every LOCAL restorable session (Room's
 * non-completed [com.mileway.core.data.model.db.SavedTrack] rows today; DataStore's single
 * current-track record is folded in as the same Room row once a session starts, so it isn't a
 * second source) with a per-session validation status, instead of [TrackMilesViewModel]'s
 * existing single-session [SessionRestoreBottomSheet][com.mileway.feature.tracking.ui.sheets.SessionRestoreBottomSheet]
 * flow, which only ever resolves one active track.
 *
 * Local only — no server session API yet (see the project's "backend deferred" policy); a future
 * phase can widen [RestorableSessionsGatherer] to merge in a server-side session list without
 * changing this ViewModel's shape.
 */
class MultiSessionRestoreViewModel(
    private val trackRepo: SavedTrackRepository,
    private val activeAccountSource: ActiveAccountSource,
) : ViewModel() {
    val sessions: StateFlow<List<RestorableSession>> =
        combine(trackRepo.rawTracksFlow(), activeAccountSource.activeAccountId) { tracks, accountId ->
            RestorableSessionsGatherer.gather(tracks, accountId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
