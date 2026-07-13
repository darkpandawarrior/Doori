package com.mileway.core.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.support.BugReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** P31.MISC.1: form state for the shake-to-report quick-actions sheet — see [BugReportSheet]. */
data class BugReportUiState(
    val title: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val didSubmit: Boolean = false,
)

class BugReportViewModel(private val repository: BugReportRepository) : ViewModel() {
    private val _state = MutableStateFlow(BugReportUiState())
    val state: StateFlow<BugReportUiState> = _state.asStateFlow()

    fun updateTitle(value: String) {
        _state.update { it.copy(title = value) }
    }

    fun updateDescription(value: String) {
        _state.update { it.copy(description = value) }
    }

    @OptIn(ExperimentalTime::class)
    fun submit(screen: String) {
        if (_state.value.title.isBlank() || _state.value.isSubmitting) return
        viewModelScope.launch {
            val current = _state.value
            _state.update { it.copy(isSubmitting = true) }
            repository.submit(
                title = current.title,
                description = current.description,
                screen = screen,
                timestampMs = Clock.System.now().toEpochMilliseconds(),
            )
            _state.value = BugReportUiState(didSubmit = true)
        }
    }

    /** Resets the one-shot "saved" flag once the sheet has consumed it (e.g. to auto-dismiss). */
    fun clearDidSubmit() {
        _state.update { it.copy(didSubmit = false) }
    }
}
