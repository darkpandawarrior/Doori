package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.favourite.FavouriteRoute
import com.mileway.core.data.favourite.FavouriteRoutesRepository
import com.mileway.core.data.favourite.PinnableTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P12.8 — drives favourite routes: the pinned list plus the completed trips that can still
 * be pinned. Pin/rename/unpin all persist over the shared [FavouriteRoutesRepository] (Room). Local.
 */
data class FavouriteRoutesUiState(
    val favourites: List<FavouriteRoute> = emptyList(),
    val pinnable: List<PinnableTrack> = emptyList(),
)

class FavouriteRoutesViewModel(
    private val repository: FavouriteRoutesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FavouriteRoutesUiState())
    val state: StateFlow<FavouriteRoutesUiState> = _state.asStateFlow()

    init {
        combine(
            repository.observeFavourites(),
            repository.observePinnableTracks(),
        ) { favourites, pinnable -> FavouriteRoutesUiState(favourites = favourites, pinnable = pinnable) }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    fun pin(
        track: PinnableTrack,
        name: String,
    ) {
        viewModelScope.launch { repository.pin(track, name) }
    }

    fun rename(
        id: String,
        name: String,
    ) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun remove(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }
}
