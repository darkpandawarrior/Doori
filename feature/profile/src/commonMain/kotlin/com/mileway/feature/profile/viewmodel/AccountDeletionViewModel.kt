package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.lifecycle.DeletionRequestRepository
import com.mileway.core.data.lifecycle.DeletionStatus
import com.mileway.core.data.session.SessionRepository
import com.mileway.feature.profile.repository.MockAccountRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ADVANCE_TICK_MS = 2_000L

/**
 * PLAN_V24 P7.1: drives the account-deletion lifecycle UI. Requests are cancelable while REQUESTED;
 * a background tick asks the repository to [DeletionRequestRepository.advance] REQUESTED→PROCESSING
 * once the sim review delay elapses. On PROCESSING the ViewModel performs the destructive
 * completion — wipes the active persona (reuses [MockAccountRepository.remove], scoped to that
 * persona's rows) and signs out — then flips [DeletionUiState.completed] so the host routes to login.
 */
data class DeletionUiState(
    val status: DeletionStatus = DeletionStatus.NONE,
    val reason: String? = null,
    val completed: Boolean = false,
)

class AccountDeletionViewModel(
    private val repository: DeletionRequestRepository,
    private val mockAccountRepository: MockAccountRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DeletionUiState())
    val state: StateFlow<DeletionUiState> = _state.asStateFlow()

    private var completing = false

    init {
        repository.observe()
            .onEach { st ->
                _state.update { it.copy(status = st.status, reason = st.reason) }
                if (st.status == DeletionStatus.PROCESSING) complete()
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            while (isActive) {
                delay(ADVANCE_TICK_MS)
                // No-op unless a REQUESTED row's sim-review delay has elapsed.
                repository.advance()
            }
        }
    }

    /** Submit a deletion request (typed-confirmation + optional reason handled in the UI). */
    fun requestDeletion(reason: String?) {
        viewModelScope.launch { repository.request(reason) }
    }

    /** Cancel a pending request — only meaningful while REQUESTED. */
    fun cancel() {
        viewModelScope.launch { repository.cancel() }
    }

    private fun complete() {
        if (completing) return
        completing = true
        viewModelScope.launch {
            val active = mockAccountRepository.observeAll().first().firstOrNull { it.isActive }
            active?.let { mockAccountRepository.remove(it.id) }
            repository.clear()
            sessionRepository.signOut()
            _state.update { it.copy(completed = true) }
        }
    }
}
