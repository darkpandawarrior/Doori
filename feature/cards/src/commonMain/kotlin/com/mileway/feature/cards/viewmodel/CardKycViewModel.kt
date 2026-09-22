package com.mileway.feature.cards.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.data.otp.OtpVerifyResult
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Digits the KYC personal-info step requires for the phone number (India, matching P1.1/P3.1). */
private const val PHONE_LENGTH = 10

/** Simulated "back-office is processing your KYC" spinner duration (source: the reference app, ~2s). */
private const val KYC_PROCESSING_MILLIS = 2_000L

/** Wizard step 2: the CARD_KYC one-time code. */
private const val STEP_OTP = 2

/** Wizard step 3: attaching the identity document. */
private const val STEP_DOCUMENT_UPLOAD = 3

/** Wizard step 4: the selfie capture, after which "Next" submits. */
private const val STEP_SELFIE = 4

/** The CARD_KYC one-time code is six digits, as issued by [LocalOtpEngine]. */
private const val OTP_LENGTH = 6

/**
 * PLAN_V24 P4.3: the 5-step Card-KYC wizard (per the reference app's KYC wizard) — (0) Intro,
 * (1) Personal info, (2) OTP, (3) Document upload, (4) Selfie → Success. OTP goes through the
 * shared [LocalOtpEngine] (purpose CARD_KYC). Per-step [CardKycUiState.isCurrentStepValid] gates
 * "Next"; the final submit shows a simulated processing spinner before success.
 *
 * V26 P26.SITE.4: the document step now attaches a real picked uri (via `core:media`'s shared
 * `rememberMediaCaptureLauncher` in `CardKycScreen`) instead of a tap-only state flip — see
 * [documentUri]. The selfie step (`AttachSelfie`) stays a simulated capture; this task's scope is
 * `AttachDocument` only.
 */
data class CardKycUiState(
    val step: Int = 0,
    val fullName: String = "",
    val idNumber: String = "",
    val phoneNumber: String = "",
    val otpCode: String = "",
    val otpError: Boolean = false,
    val otpSentTo: String? = null,
    val demoCode: String? = null,
    val documentUri: String? = null,
    val selfieAttached: Boolean = false,
    val isProcessing: Boolean = false,
    val done: Boolean = false,
) {
    val totalSteps: Int get() = 5
    val isLastStep: Boolean get() = step == totalSteps - 1
    val documentAttached: Boolean get() = documentUri != null

    val isCurrentStepValid: Boolean
        get() =
            when (step) {
                1 -> fullName.isNotBlank() && idNumber.isNotBlank() && phoneNumber.filter { it.isDigit() }.length == PHONE_LENGTH
                STEP_OTP -> otpCode.length == OTP_LENGTH
                STEP_DOCUMENT_UPLOAD -> documentAttached
                STEP_SELFIE -> selfieAttached
                else -> true
            }
}

sealed interface CardKycAction {
    data object Next : CardKycAction

    data object Back : CardKycAction

    data class SetFullName(
        val value: String,
    ) : CardKycAction

    data class SetIdNumber(
        val value: String,
    ) : CardKycAction

    data class SetPhone(
        val value: String,
    ) : CardKycAction

    data class SetOtp(
        val value: String,
    ) : CardKycAction

    data class AttachDocument(
        val uri: String,
    ) : CardKycAction

    data object AttachSelfie : CardKycAction
}

sealed interface CardKycEffect {
    data object Finished : CardKycEffect
}

class CardKycViewModel(
    private val otpEngine: LocalOtpEngine,
) : BaseViewModel<CardKycUiState, CardKycEffect, CardKycAction>(CardKycUiState()) {
    override fun onAction(action: CardKycAction) {
        when (action) {
            CardKycAction.Next -> advance()
            CardKycAction.Back -> setState { copy(step = (step - 1).coerceAtLeast(0), otpError = false) }
            is CardKycAction.SetFullName -> setState { copy(fullName = action.value) }
            is CardKycAction.SetIdNumber -> setState { copy(idNumber = action.value) }
            is CardKycAction.SetPhone -> setState { copy(phoneNumber = action.value) }
            is CardKycAction.SetOtp -> setState { copy(otpCode = action.value.filter { it.isDigit() }.take(OTP_LENGTH), otpError = false) }
            is CardKycAction.AttachDocument -> setState { copy(documentUri = action.uri) }
            CardKycAction.AttachSelfie -> setState { copy(selfieAttached = true) }
        }
    }

    private fun advance() {
        val s = currentState
        if (!s.isCurrentStepValid) return
        when (s.step) {
            1 -> {
                // Entering the OTP step: dispatch a CARD_KYC code to the entered number.
                val target = s.phoneNumber.filter { it.isDigit() }
                otpEngine.send(OtpPurpose.CARD_KYC, target)
                setState { copy(step = 2, otpSentTo = target, demoCode = otpEngine.codeFor(OtpPurpose.CARD_KYC, target), otpError = false) }
            }
            2 -> {
                // Verify before leaving the OTP step.
                val target = s.otpSentTo ?: s.phoneNumber.filter { it.isDigit() }
                if (otpEngine.verify(OtpPurpose.CARD_KYC, target, s.otpCode) == OtpVerifyResult.Success) {
                    setState { copy(step = STEP_DOCUMENT_UPLOAD, otpError = false) }
                } else {
                    setState { copy(otpError = true) }
                }
            }
            STEP_SELFIE -> submit()
            else -> setState { copy(step = (step + 1).coerceAtMost(totalSteps - 1)) }
        }
    }

    private fun submit() {
        setState { copy(isProcessing = true) }
        viewModelScope.launch {
            delay(KYC_PROCESSING_MILLIS)
            setState { copy(isProcessing = false, done = true) }
            emitEffect(CardKycEffect.Finished)
        }
    }
}
