package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.data.otp.OtpVerifyResult
import com.mileway.core.data.session.SessionRepository
import com.mileway.feature.profile.data.CorporateDomains
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Steps of the corporate-email verification flow. */
enum class CorporateStep { ENTER_EMAIL, VERIFY }

/** Distinct validation errors surfaced by the corporate-email flow. */
enum class CorporateError { UNRECOGNISED_DOMAIN, WRONG_CODE }

data class CorporateVerificationUiState(
    val step: CorporateStep = CorporateStep.ENTER_EMAIL,
    val email: String = "",
    val code: String = "",
    val demoCode: String? = null,
    val error: CorporateError? = null,
    val verifiedEmail: String? = null,
) {
    val isVerified: Boolean get() = !verifiedEmail.isNullOrBlank()
}

/**
 * PLAN_V24 P4.4 — corporate email verification (per the reference app's corporate verification). Enter a company
 * email whose domain is on the recognised [CorporateDomains] list → an OTP is dispatched to that
 * email (purpose CORPORATE_EMAIL, via the shared [LocalOtpEngine]) → a correct code marks the
 * session corporate-verified. Offline — no real email is sent. The document-upload fallback is
 * intentionally skipped (P4.2 covers document upload generally — noted in PROGRESS).
 */
class CorporateVerificationViewModel(
    private val sessionRepository: SessionRepository,
    private val otpEngine: LocalOtpEngine,
) : ViewModel() {
    private val _state = MutableStateFlow(CorporateVerificationUiState())
    val state: StateFlow<CorporateVerificationUiState> = _state.asStateFlow()

    init {
        sessionRepository.sessionState
            .onEach { session -> _state.update { it.copy(verifiedEmail = session.corporateEmail) } }
            .launchIn(viewModelScope)
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _state.update { it.copy(code = digits, error = null) }
        if (digits.length == 6) verify()
    }

    /** Validates the domain, then dispatches the OTP and moves to the verify step. */
    fun requestOtp() {
        val email = _state.value.email.trim()
        if (!CorporateDomains.isRecognised(email)) {
            _state.update { it.copy(error = CorporateError.UNRECOGNISED_DOMAIN) }
            return
        }
        otpEngine.send(OtpPurpose.CORPORATE_EMAIL, email)
        _state.update {
            it.copy(step = CorporateStep.VERIFY, demoCode = otpEngine.codeFor(OtpPurpose.CORPORATE_EMAIL, email), error = null)
        }
    }

    fun verify() {
        val email = _state.value.email.trim()
        if (otpEngine.verify(OtpPurpose.CORPORATE_EMAIL, email, _state.value.code) == OtpVerifyResult.Success) {
            viewModelScope.launch { sessionRepository.markCorporateVerified(email) }
            _state.update { it.copy(error = null) }
        } else {
            _state.update { it.copy(error = CorporateError.WRONG_CODE) }
        }
    }

    /** Resets to the entry step (e.g. wrong email) without touching a prior verification. */
    fun reset() {
        _state.update { it.copy(step = CorporateStep.ENTER_EMAIL, code = "", demoCode = null, error = null) }
    }
}
