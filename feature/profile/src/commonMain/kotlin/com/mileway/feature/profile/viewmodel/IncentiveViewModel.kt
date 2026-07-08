package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.SavedTrackDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * PLAN_V24 P6.3: incentive programs, split active vs expired. The "complete N tracked trips" program's
 * progress is fed by the real completed-trip count (from [SavedTrackDao], shared core:data DAO — no
 * feature-to-feature dependency); the rest are seeded. Building lives in [IncentiveCatalog] (pure).
 */
data class IncentiveUiState(
    val active: List<IncentiveProgram> = emptyList(),
    val expired: List<IncentiveProgram> = emptyList(),
)

class IncentiveViewModel(savedTrackDao: SavedTrackDao) : ViewModel() {
    val state: StateFlow<IncentiveUiState> =
        savedTrackDao.getCompletedTracks()
            .map { completed ->
                val programs = IncentiveCatalog.build(completed.size)
                IncentiveUiState(
                    active = programs.filter { !it.expired },
                    expired = programs.filter { it.expired },
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, IncentiveUiState())
}
