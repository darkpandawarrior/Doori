package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.profile.model.SupportTicket
import com.mileway.feature.profile.repository.SupportTicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V22 P6.8: state for `HelpScreen`'s "Contact Support" form and its "My Tickets" list — real
 * Room-backed persistence (via [SupportTicketRepository]) replacing the form's previous
 * fire-and-forget snackbar. [submitError] surfaces the blank-subject/blank-body validation gate; it
 * is cleared on the next successful submit attempt or when [SupportTicketViewModel
 * .clearSubmitError] is called.
 */
data class SupportTicketUiState(
    val tickets: List<SupportTicket> = emptyList(),
    val submitError: String? = null,
)

class SupportTicketViewModel(
    private val repository: SupportTicketRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SupportTicketUiState())
    val state: StateFlow<SupportTicketUiState> = _state.asStateFlow()

    init {
        repository.observeAll().onEach { list -> _state.update { it.copy(tickets = list) } }.launchIn(viewModelScope)
    }

    /**
     * Submits a new support ticket. Both [subject] and [body] must be non-blank, otherwise
     * [SupportTicketUiState.submitError] is set and nothing is persisted.
     */
    fun submit(
        subject: String,
        body: String,
    ) {
        if (subject.isBlank() || body.isBlank()) {
            _state.update { it.copy(submitError = "Enter a subject and a description before submitting.") }
            return
        }
        viewModelScope.launch { repository.submit(subject = subject, body = body) }
        _state.update { it.copy(submitError = null) }
    }

    /** Dismisses [SupportTicketUiState.submitError] without changing any persisted state. */
    fun clearSubmitError() {
        _state.update { it.copy(submitError = null) }
    }
}
