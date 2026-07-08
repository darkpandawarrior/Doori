package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.rewards.RewardCard
import com.mileway.core.data.rewards.RewardStatus
import com.mileway.feature.profile.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P5.3: drives `RewardsScreen` — the earned scratch cards and the reveal action. Scratching
 * a card flips it to SCRATCHED and credits its value; [totalCredits] is derived from revealed cards.
 */
data class RewardsUiState(
    val cards: List<RewardCard> = emptyList(),
) {
    val totalCredits: Int get() = cards.filter { it.status == RewardStatus.SCRATCHED }.sumOf { it.credits }
}

class RewardsViewModel(private val repository: RewardsRepository) : ViewModel() {
    private val _state = MutableStateFlow(RewardsUiState())
    val state: StateFlow<RewardsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        repository.observeAll()
            .onEach { cards -> _state.update { it.copy(cards = cards) } }
            .launchIn(viewModelScope)
    }

    /** Reveals a card (idempotent — already-scratched cards are a no-op in the repository). */
    fun scratch(id: String) {
        viewModelScope.launch { repository.scratch(id) }
    }
}
