package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpDelivery
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.data.otp.OtpVerifyResult
import com.mileway.core.data.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class PhoneChangeStep { ENTER_PHONE, VERIFY }

enum class PhoneChangeError { INVALID_PHONE, WRONG_CODE, EXPIRED }

data class PhoneChangeUiState(
    val step: PhoneChangeStep = PhoneChangeStep.ENTER_PHONE,
    val currentPhone: String = "",
    val newPhone: String = "",
    val code: String = "",
    val delivery: OtpDelivery? = null,
    val error: PhoneChangeError? = null,
    val done: Boolean = false,
)

/**
 * PLAN_V24 P3.1 — phone change with OTP re-verify (the reference app `verify_my_contact_number` /
 * the reference app `ProfileUpdateMode.PHONE`). Requesting a change dispatches a PHONE_CHANGE OTP to the new
 * number and persists it as pending ([SessionRepository.startPhoneChange]); only a correct OTP
 * commits it. A relaunch with a pending target resumes the verify step (re-sending a fresh code,
 * since the in-memory challenge doesn't survive process death). Cancel leaves the old number.
 *
 * ponytail: minimal inline phone validation (digits, 10-length) rather than pulling
 * shared's PhoneNumberValidator across the module boundary — noted in PROGRESS.
 */
class PhoneChangeViewModel(
    private val sessionRepository: SessionRepository,
    private val otpEngine: LocalOtpEngine,
) : ViewModel() {
    private val _state = MutableStateFlow(PhoneChangeUiState())
    val state: StateFlow<PhoneChangeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.sessionState.first()
            _state.value = _state.value.copy(currentPhone = session.phone)
            // Resume an interrupted verify (pending survived, the challenge did not → re-send).
            session.pendingPhoneChangeTarget?.let { target ->
                val delivery = otpEngine.send(OtpPurpose.PHONE_CHANGE, target)
                _state.value = _state.value.copy(step = PhoneChangeStep.VERIFY, newPhone = target, delivery = delivery)
            }
        }
    }

    fun onNewPhoneChange(value: String) {
        _state.value = _state.value.copy(newPhone = value, error = null)
    }

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _state.value = _state.value.copy(code = digits, error = null)
        if (digits.length == 6) verify()
    }

    fun requestOtp() {
        val target = normalize(_state.value.newPhone)
        if (target.length != NATIONAL_LENGTH) {
            _state.value = _state.value.copy(error = PhoneChangeError.INVALID_PHONE)
            return
        }
        viewModelScope.launch {
            sessionRepository.startPhoneChange(target)
            val delivery = otpEngine.send(OtpPurpose.PHONE_CHANGE, target)
            _state.value = _state.value.copy(step = PhoneChangeStep.VERIFY, newPhone = target, delivery = delivery, error = null)
        }
    }

    fun verify() {
        val current = _state.value
        val target = current.newPhone
        when (otpEngine.verify(OtpPurpose.PHONE_CHANGE, target, current.code)) {
            OtpVerifyResult.Success ->
                viewModelScope.launch {
                    sessionRepository.commitPhoneChange()
                    _state.value = current.copy(done = true, error = null, currentPhone = target)
                }
            OtpVerifyResult.Expired, OtpVerifyResult.NoChallenge ->
                _state.value = current.copy(error = PhoneChangeError.EXPIRED)
            else -> _state.value = current.copy(error = PhoneChangeError.WRONG_CODE)
        }
    }

    fun autofillDemoCode() {
        _state.value.delivery?.code?.let { onCodeChange(it) }
    }

    fun cancel() {
        viewModelScope.launch { sessionRepository.cancelPhoneChange() }
        _state.value = PhoneChangeUiState(currentPhone = _state.value.currentPhone)
    }

    private fun normalize(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.startsWith("0")) digits.trimStart('0') else digits
    }

    private companion object {
        const val NATIONAL_LENGTH = 10
    }
}
