package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P6.1: "Mileway Club" membership. Reads the club state from the session DataStore and
 * drives the consent → activate flow + the one-time confetti flag. Eligibility is the `clubEnabled`
 * plugin gate (checked at the call site), so no separate eligible flag is stored.
 */
data class MembershipUiState(
    val isMember: Boolean = false,
    val activatedAtMs: Long? = null,
    val confettiShown: Boolean = false,
)

class MembershipViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    val state: StateFlow<MembershipUiState> =
        sessionRepository.sessionState
            .map { s -> MembershipUiState(isMember = s.isClubMember, activatedAtMs = s.clubActivatedAtMs, confettiShown = s.clubConfettiShown) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, MembershipUiState())

    /** Completes the consent flow and activates the membership (records the join date). */
    fun activate() {
        viewModelScope.launch { sessionRepository.activateClub() }
    }

    /** Marks the one-time activation confetti as shown so it does not replay. */
    fun markConfettiShown() {
        viewModelScope.launch { sessionRepository.markClubConfettiShown() }
    }
}
