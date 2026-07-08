package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.referral.ReferralActivity
import com.mileway.core.data.referral.ReferralLeaderboardEntry
import com.mileway.core.data.referral.ReferralStatus
import com.mileway.core.data.referral.ReferralTxn
import com.mileway.core.platform.ReferralManager
import com.mileway.feature.profile.data.ReferralMockData
import com.mileway.feature.profile.repository.ReferralProgramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P5.1: drives `ReferralHubScreen` — the referral code + share, the transactions split
 * into pending/completed buckets (with each referee's progress toward its next target), the seeded
 * leaderboard, and the activity feed. PENDING referees resolve to SUCCESS/FAILED through the
 * [ReferralProgramRepository]'s SimulatedReviewEngine on load (so they progress across sessions).
 */
data class ReferralHubUiState(
    val code: String = "",
    val pending: List<ReferralTxn> = emptyList(),
    val completed: List<ReferralTxn> = emptyList(),
    val leaderboard: List<ReferralLeaderboardEntry> = ReferralMockData.leaderboard,
    val activity: List<ReferralActivity> = ReferralMockData.activity,
    val totalCredits: Int = 0,
    val totalMoney: Double = 0.0,
) {
    val userRank: Int? get() = leaderboard.firstOrNull { it.isCurrentUser }?.rank
}

class ReferralHubViewModel(
    private val repository: ReferralProgramRepository,
    private val referralManager: ReferralManager,
) : ViewModel() {
    private val _state = MutableStateFlow(ReferralHubUiState())
    val state: StateFlow<ReferralHubUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            repository.resolveReviewablePending()
            _state.update { it.copy(code = referralManager.myReferralCode()) }
        }
        repository.observeAll()
            .onEach { txns ->
                _state.update { current ->
                    current.copy(
                        completed = txns.filter { it.status == ReferralStatus.SUCCESS },
                        pending = txns.filter { it.status != ReferralStatus.SUCCESS },
                        totalCredits = txns.sumOf { it.processedCredits },
                        totalMoney = txns.sumOf { it.processedMoney },
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
