package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.profile.model.ActiveSession
import com.mileway.feature.profile.repository.ActiveSessionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * PLAN_V22 P6.4: state for [ActiveSessionsScreen][com.mileway.feature.profile.ui.screens
 * .ActiveSessionsScreen] — real Room-backed persistence (via [ActiveSessionsRepository])
 * promoting the previous read-only `SessionsDialog` list.
 */
data class ActiveSessionsUiState(
    val sessions: List<ActiveSession> = emptyList(),
)

class ActiveSessionsViewModel(
    private val repository: ActiveSessionsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ActiveSessionsUiState())
    val state: StateFlow<ActiveSessionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            repository.observeAll().onEach { list -> _state.update(list) }.launchIn(viewModelScope)
        }
    }

    private fun MutableStateFlow<ActiveSessionsUiState>.update(list: List<ActiveSession>) {
        value = value.copy(sessions = list)
    }

    /**
     * Revokes [id] outright. A no-op for the current-device row — [ActiveSessionsScreen] never
     * offers a revoke affordance on it, but this is a second, defensive guard against a stray
     * caller (e.g. a future bulk-select flow) revoking the device driving this app instance.
     */
    fun revoke(id: String) {
        val target = _state.value.sessions.find { it.id == id } ?: return
        if (target.isCurrent) return
        viewModelScope.launch { repository.revoke(id) }
    }

    /** "Sign out all other sessions" — leaves only the current device's row. */
    fun revokeAllExceptCurrent() {
        viewModelScope.launch { repository.revokeAllExceptCurrent() }
    }
}
