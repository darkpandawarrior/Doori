package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TripAttachmentEntity
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.data.model.display.toDisplayData
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import com.mileway.feature.tracking.repository.isPersonal
import com.siddharth.kmp.mvi.BaseViewModel
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
    // In-flight state for the actions below, so the screen can disable buttons / show a spinner
    // instead of letting a second tap fire a second write while the first is still committing.
    val isSaving: Boolean = false,
)

sealed interface TrackDetailAction {
    data class Load(val routeId: String) : TrackDetailAction

    /** Corrects the recorded distance (e.g. GPS drift added a stray km). Rejects <= 0. */
    data class EditDistance(val distanceKm: Double) : TrackDetailAction

    /** Flips the business/personal classification (see [SavedTrackRepository.setPersonal]). */
    data object TogglePersonal : TrackDetailAction

    /** Permanently removes this journey; emits [TrackDetailEffect.Discarded] on success. */
    data object Discard : TrackDetailAction
}

sealed interface TrackDetailEffect {
    /** The journey was deleted — the screen has nothing left to show, so it should navigate back. */
    data object Discarded : TrackDetailEffect

    data class ActionFailed(val message: String) : TrackDetailEffect
}

class TrackDetailViewModel(
    private val trackRepository: SavedTrackRepository,
    private val locationRepository: LocationRepository,
    private val attachmentRepository: TripAttachmentRepository,
) : BaseViewModel<TrackDetailUiState, TrackDetailEffect, TrackDetailAction>(TrackDetailUiState()) {
    private var routeId: String? = null

    override fun onAction(action: TrackDetailAction) {
        when (action) {
            is TrackDetailAction.Load -> load(action.routeId)
            is TrackDetailAction.EditDistance -> editDistance(action.distanceKm)
            TrackDetailAction.TogglePersonal -> togglePersonal()
            TrackDetailAction.Discard -> discard()
        }
    }

    private fun load(id: String) {
        routeId = id
        viewModelScope.launch {
            val track = trackRepository.getByRouteId(id)
            setState { copy(track = track?.toDisplayData(), rawTrack = track, isLoading = false) }
        }
        locationRepository.locationsForToken(id)
            .onEach { locs -> setState { copy(locations = locs) } }
            .catch { e -> setState { copy(error = e.message) } }
            .launchIn(viewModelScope)

        attachmentRepository.attachmentsForTrack(id)
            .onEach { attachments -> setState { copy(attachments = attachments) } }
            .catch { e -> setState { copy(error = e.message) } }
            .launchIn(viewModelScope)
    }

    private fun editDistance(distanceKm: Double) {
        val id = routeId ?: return
        if (distanceKm <= 0.0) {
            emitEffect(TrackDetailEffect.ActionFailed("Distance must be greater than 0 km"))
            return
        }
        viewModelScope.launch {
            setState { copy(isSaving = true) }
            trackRepository.updateDistance(id, distanceKm)
            val refreshed = trackRepository.getByRouteId(id)
            setState { copy(track = refreshed?.toDisplayData(), rawTrack = refreshed, isSaving = false) }
        }
    }

    private fun togglePersonal() {
        val id = routeId ?: return
        val currentlyPersonal = currentState.rawTrack?.isPersonal ?: false
        viewModelScope.launch {
            setState { copy(isSaving = true) }
            trackRepository.setPersonal(id, !currentlyPersonal)
            val refreshed = trackRepository.getByRouteId(id)
            setState { copy(track = refreshed?.toDisplayData(), rawTrack = refreshed, isSaving = false) }
        }
    }

    private fun discard() {
        val id = routeId ?: return
        viewModelScope.launch {
            setState { copy(isSaving = true) }
            val removed = trackRepository.delete(id)
            setState { copy(isSaving = false) }
            if (removed) {
                emitEffect(TrackDetailEffect.Discarded)
            } else {
                emitEffect(TrackDetailEffect.ActionFailed("This journey was already removed"))
            }
        }
    }
}
