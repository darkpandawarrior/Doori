package com.mileway.feature.whatsnew.viewmodel

import androidx.lifecycle.ViewModel
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WhatsNewDetailUiState(
    val entry: WhatsNewEntry? = null,
    val selectedMediaIndex: Int = 0,
    val error: Boolean = false,
)

/**
 * PLAN_V36 P4 — [entryId] is a Koin runtime parameter (`viewModel { params -> ... }`, mirrors
 * `feature:events`' `EventDetailViewModel`), not a nav-graph SavedStateHandle read — the entry is
 * resolved once, at construction, from the bundled [WhatsNewRepository].
 */
class WhatsNewDetailViewModel(
    entryId: String,
    private val repository: WhatsNewRepository,
) : ViewModel() {
    private val _uiState: MutableStateFlow<WhatsNewDetailUiState>
    val uiState: StateFlow<WhatsNewDetailUiState>

    init {
        val entry = repository.entry(entryId)
        _uiState = MutableStateFlow(WhatsNewDetailUiState(entry = entry, error = entry == null))
        uiState = _uiState.asStateFlow()
    }

    /**
     * Called as [com.mileway.feature.whatsnew.ui.components.HeroCarousel]'s pager settles on a
     * page — clamped to the entry's media range so an out-of-bounds index (there shouldn't be one,
     * but the pager is the caller, not us) can never desync the "Step X of N" header.
     */
    fun selectMedia(index: Int) {
        val mediaCount = _uiState.value.entry?.media?.size ?: return
        if (mediaCount == 0) return
        _uiState.update { it.copy(selectedMediaIndex = index.coerceIn(0, mediaCount - 1)) }
    }
}
