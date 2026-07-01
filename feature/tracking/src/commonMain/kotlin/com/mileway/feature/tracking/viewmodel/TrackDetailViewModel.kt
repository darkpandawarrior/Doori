package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.data.model.display.toDisplayData
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class TrackDetailUiState(
    val track: TrackDisplayData? = null,
    val rawTrack: SavedTrack? = null,
    val locations: List<LocationData> = emptyList(),
    val attachments: List<TripAttachmentEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface TrackDetailAction {
    data class Load(val routeId: String) : TrackDetailAction
}

sealed interface TrackDetailEffect

class TrackDetailViewModel(
    private val trackRepository: SavedTrackRepository,
    private val locationRepository: LocationRepository,
    private val attachmentRepository: TripAttachmentRepository,
) : BaseViewModel<TrackDetailUiState, TrackDetailEffect, TrackDetailAction>(TrackDetailUiState()) {
    override fun onAction(action: TrackDetailAction) {
        when (action) {
            is TrackDetailAction.Load -> load(action.routeId)
        }
    }

    private fun load(routeId: String) {
        viewModelScope.launch {
            val track = trackRepository.getByRouteId(routeId)
            setState { copy(track = track?.toDisplayData(), rawTrack = track, isLoading = false) }
        }
        locationRepository.locationsForToken(routeId)
            .onEach { locs -> setState { copy(locations = locs) } }
            .catch { e -> setState { copy(error = e.message) } }
            .launchIn(viewModelScope)

        attachmentRepository.attachmentsForTrack(routeId)
            .onEach { attachments -> setState { copy(attachments = attachments) } }
            .catch { e -> setState { copy(error = e.message) } }
            .launchIn(viewModelScope)
    }
}
