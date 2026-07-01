package com.mileway.feature.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.media.repository.MediaLibraryRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface CloudLibraryAction {
    data class Delete(val entry: MediaLibraryEntry) : CloudLibraryAction
}

/** No one-shot effects. Present to satisfy the MVI contract. */
sealed interface CloudLibraryEffect

class CloudLibraryViewModel(
    private val repository: MediaLibraryRepository,
) : BaseViewModel<List<MediaLibraryEntry>, CloudLibraryEffect, CloudLibraryAction>(emptyList()) {
    /** Backwards-compatible alias; screens read [state]. */
    val entries: StateFlow<List<MediaLibraryEntry>> = state

    init {
        viewModelScope.launch {
            repository.observeLibrary().collect { list -> setState { list } }
        }
    }

    override fun onAction(action: CloudLibraryAction) {
        when (action) {
            is CloudLibraryAction.Delete -> viewModelScope.launch { repository.delete(action.entry) }
        }
    }
}
