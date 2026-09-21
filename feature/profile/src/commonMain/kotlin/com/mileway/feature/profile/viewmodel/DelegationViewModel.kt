package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.profile.model.Delegation
import com.mileway.feature.profile.repository.DelegationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Same day/month/year format `AccountDetailsSheet` already uses for persisted timestamps. */
fun formatDelegationExpiry(ms: Long): String =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
        "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
    }

/**
 * PLAN_V22 P6.3: state for [DelegationScreen][com.mileway.feature.profile.ui.screens.DelegationScreen]'s
 * "My Delegations" list — real Room-backed persistence replacing the screen's previous
 * `mutableStateListOf` seed. [submitError] surfaces the blank-delegate/blank-scope validation gate
 * (matching the reference app's blank-reason guard); it is cleared on the next successful submit
 * attempt or when [DelegationViewModel.clearSubmitError] is called.
 */
data class DelegationUiState(
    val delegations: List<Delegation> = emptyList(),
    val submitError: String? = null,
)

class DelegationViewModel(
    private val repository: DelegationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DelegationUiState())
    val state: StateFlow<DelegationUiState> = _state.asStateFlow()

    init {
        repository.observeAll().onEach { list -> _state.update { it.copy(delegations = list) } }.launchIn(viewModelScope)
    }

    /**
     * Adds a new delegation. Both [delegateName] and [scope] must be non-blank — matching the
     * reference app's blank-reason guard — otherwise [DelegationUiState.submitError] is set and
     * nothing is persisted.
     */
    fun add(
        delegateName: String,
        scope: String,
        expiresAtMillis: Long,
    ) {
        if (delegateName.isBlank() || scope.isBlank()) {
            _state.update { it.copy(submitError = "Select a team member and a delegation scope before submitting.") }
            return
        }
        viewModelScope.launch {
            repository.add(delegateName = delegateName, scope = scope, expiresAtMillis = expiresAtMillis)
        }
        _state.update { it.copy(submitError = null) }
    }

    /** Revokes (deletes) [id] outright — irreversible. */
    fun revoke(id: String) {
        viewModelScope.launch { repository.revoke(id) }
    }

    /** Pauses/resumes [id] without revoking it. */
    fun setActive(
        id: String,
        isActive: Boolean,
    ) {
        viewModelScope.launch { repository.setActive(id, isActive) }
    }

    /** Dismisses [DelegationUiState.submitError] without changing any persisted state. */
    fun clearSubmitError() {
        _state.update { it.copy(submitError = null) }
    }
}
