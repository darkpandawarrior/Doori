package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EmailVerificationUiState(
    val email: String = "",
    val isVerified: Boolean = false,
    val linkSent: Boolean = false,
)

/**
 * PLAN_V24 P3.2 — email verification status (per the reference app's email verification).
 * Offline: [sendLink] flips to a "link sent" state whose demo action
 * ([confirmClicked]) marks the email verified in the session. No real link is sent.
 */
class EmailVerificationViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val linkSentFlow = MutableStateFlow(false)

    val uiState: StateFlow<EmailVerificationUiState> =
        combine(sessionRepository.sessionState, linkSentFlow) { session, linkSent ->
            EmailVerificationUiState(
                email = session.email ?: "",
                isVerified = session.emailVerified,
                linkSent = linkSent && !session.emailVerified,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, EmailVerificationUiState())

    /** "Send verification link" — offline demo, just surfaces the simulate-click affordance. */
    fun sendLink() {
        linkSentFlow.value = true
    }

    /** The demo "Simulate clicking the link" action — marks the email verified. */
    fun confirmClicked() {
        viewModelScope.launch { sessionRepository.markEmailVerified() }
        linkSentFlow.value = false
    }
}
