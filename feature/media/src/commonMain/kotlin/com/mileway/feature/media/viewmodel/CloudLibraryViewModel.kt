package com.mileway.feature.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.feature.media.model.MediaLibraryFilter
import com.mileway.feature.media.model.MediaLibrarySort
import com.mileway.feature.media.model.applyLibraryFilterAndSort
import com.mileway.feature.media.repository.MediaLibraryRepository
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** V26 P26.LIB.1: raw entries plus the active filter/sort selection driving the FilterChip row + sort menu. */
data class CloudLibraryUiState(
    val entries: List<MediaLibraryEntry> = emptyList(),
    val filter: MediaLibraryFilter = MediaLibraryFilter.All,
    val sort: MediaLibrarySort = MediaLibrarySort.NewestFirst,
)

sealed interface CloudLibraryAction {
    data class Delete(val entry: MediaLibraryEntry) : CloudLibraryAction

    data class ToggleFavorite(val entry: MediaLibraryEntry) : CloudLibraryAction

    data class SetFilter(val filter: MediaLibraryFilter) : CloudLibraryAction

    data class SetSort(val sort: MediaLibrarySort) : CloudLibraryAction

    /** Fired when an entry is opened full-screen — updates [MediaLibraryEntry.lastAccessedAt]. */
    data class Viewed(val entry: MediaLibraryEntry) : CloudLibraryAction
}

/** No one-shot effects. Present to satisfy the MVI contract. */
sealed interface CloudLibraryEffect

class CloudLibraryViewModel(
    private val repository: MediaLibraryRepository,
) : BaseViewModel<CloudLibraryUiState, CloudLibraryEffect, CloudLibraryAction>(CloudLibraryUiState()) {
    private val filter = MutableStateFlow(MediaLibraryFilter.All)
    private val sort = MutableStateFlow(MediaLibrarySort.NewestFirst)

    init {
        viewModelScope.launch {
            combine(repository.observeLibrary(), filter, sort) { entries, f, s ->
                CloudLibraryUiState(applyLibraryFilterAndSort(entries, f, s), f, s)
            }.collect { newState -> setState { newState } }
        }
    }

    override fun onAction(action: CloudLibraryAction) {
        when (action) {
            is CloudLibraryAction.Delete -> viewModelScope.launch { repository.softDelete(action.entry) }
            is CloudLibraryAction.ToggleFavorite -> viewModelScope.launch { repository.toggleFavorite(action.entry) }
            is CloudLibraryAction.SetFilter -> filter.value = action.filter
            is CloudLibraryAction.SetSort -> sort.value = action.sort
            is CloudLibraryAction.Viewed -> viewModelScope.launch { repository.touchLastAccessed(action.entry) }
        }
    }
}
