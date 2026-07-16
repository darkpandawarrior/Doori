package com.mileway.feature.whatsnew.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.whatsnew.data.WhatsNewEngagementRecorder
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import com.mileway.feature.whatsnew.model.WhatsNewEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WhatsNewDetailUiState(
    val entry: WhatsNewEntry? = null,
    val selectedMediaIndex: Int = 0,
    val error: Boolean = false,
)

/**
 * PLAN_V36 P4/P7 — [entryId] is a Koin runtime parameter (`viewModel { params -> ... }`, mirrors
 * `feature:events`' `EventDetailViewModel`), not a nav-graph SavedStateHandle read — the entry is
 * resolved once, at construction, from the bundled [WhatsNewRepository]. Owning the engagement
 * recording here (rather than in the composable) keeps it testable without a Compose harness and
 * matches every other cross-cutting VM-side side effect in this feature.
 */
class WhatsNewDetailViewModel(
    entryId: String,
    private val repository: WhatsNewRepository,
    private val engagement: WhatsNewEngagementRecorder,
) : ViewModel() {
    private val _uiState: MutableStateFlow<WhatsNewDetailUiState>
    val uiState: StateFlow<WhatsNewDetailUiState>

    init {
        val entry = repository.entry(entryId)
        _uiState = MutableStateFlow(WhatsNewDetailUiState(entry = entry, error = entry == null))
        uiState = _uiState.asStateFlow()
        // Once per detail visit, fire-and-forget — never blocks first paint, and this VM is
        // created fresh per nav destination so it can't double-record on recomposition.
        if (entry != null) {
            viewModelScope.launch { engagement.record(WhatsNewEngagementRecorder.TYPE_OPENED, entry.id) }
        }
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

    /** The Share top-bar action was used — the composable still owns the actual [com.mileway.core.platform.ShareSheet] call. */
    fun recordShared() {
        val entryId = _uiState.value.entry?.id ?: return
        viewModelScope.launch { engagement.record(WhatsNewEngagementRecorder.TYPE_SHARED, entryId) }
    }

    /** "Get in touch" (button or the underlined email) was tapped — composable owns the mailto [com.mileway.core.platform.UrlOpener] call. */
    fun recordContact() {
        val entryId = _uiState.value.entry?.id ?: return
        viewModelScope.launch { engagement.record(WhatsNewEngagementRecorder.TYPE_CONTACT, entryId) }
    }
}
