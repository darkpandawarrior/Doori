package com.mileway.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpDelivery
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.data.otp.OtpVerifyResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which failure to surface on the OTP screen. */
enum class OtpError { WRONG, EXPIRED, LOCKED }

data class OtpUiState(
    val code: String = "",
    val resendInSeconds: Int = 0,
    val error: OtpError? = null,
    val attemptsRemaining: Int? = null,
    val delivery: OtpDelivery? = null,
    val verified: Boolean = false,
)

/**
 * PLAN_V24 P1.2 — drives the shared [OtpVerificationScreen] over the single [LocalOtpEngine].
 * Parameterized per use via [start] (Koin can't inject the purpose/target), so one screen serves
 * every OTP flow (LOGIN today; MFA/phone-change/wallet/KYC later). The engine is the source of
 * truth for the code, expiry and resend cooldown; this VM only mirrors its state and the entered
 * digits, and ticks the resend countdown.
 */
class OtpVerificationViewModel(private val engine: LocalOtpEngine) : ViewModel() {
    private var purpose: OtpPurpose = OtpPurpose.LOGIN
    private var target: String = ""

    private val _state = MutableStateFlow(OtpUiState())
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    /**
     * Bind the screen to an already-dispatched challenge. [delivery] is the send result the caller
     * (e.g. [AuthViewModel.sendLoginOtp]) produced — passed in rather than re-sending so attempts
     * and the cooldown aren't reset on entry.
     */
    fun start(
        purpose: OtpPurpose,
        target: String,
        delivery: OtpDelivery?,
    ) {
        this.purpose = purpose
        this.target = target
        _state.value = OtpUiState(delivery = delivery)
        startResendTicker()
    }

    fun onCodeChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(CODE_LENGTH)
        _state.update { it.copy(code = digits, error = null) }
        if (digits.length == CODE_LENGTH) verify()
    }

    /** Autofill the demo code (offline convenience — the delivery already carries it). */
    fun autofillDemoCode() {
        _state.value.delivery?.code?.let { onCodeChange(it) }
    }

    fun verify() {
        val code = _state.value.code
        when (val result = engine.verify(purpose, target, code)) {
            is OtpVerifyResult.Success -> _state.update { it.copy(verified = true, error = null) }
            is OtpVerifyResult.WrongCode ->
                _state.update { it.copy(error = OtpError.WRONG, attemptsRemaining = result.attemptsRemaining) }
            OtpVerifyResult.Expired -> _state.update { it.copy(error = OtpError.EXPIRED) }
            OtpVerifyResult.LockedOut -> _state.update { it.copy(error = OtpError.LOCKED) }
            OtpVerifyResult.NoChallenge -> _state.update { it.copy(error = OtpError.EXPIRED) }
        }
    }

    fun resend() {
        val delivery = engine.send(purpose, target)
        _state.update { OtpUiState(delivery = delivery) }
        startResendTicker()
    }

    fun requestViaCall() {
        val delivery = engine.requestViaCall(purpose, target)
        _state.update { OtpUiState(delivery = delivery) }
        startResendTicker()
    }

    private fun startResendTicker() {
        tickerJob?.cancel()
        tickerJob =
            viewModelScope.launch {
                // Seed from the engine once, then count down locally — re-reading the clock each
                // tick would never terminate under a frozen test clock, and one delay == one second
                // matches the engine's cooldown in production.
                var remaining = engine.resendAvailableInSeconds(purpose, target)
                _state.update { it.copy(resendInSeconds = remaining) }
                while (remaining > 0) {
                    delay(1_000)
                    remaining -= 1
                    _state.update { it.copy(resendInSeconds = remaining) }
                }
            }
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}
