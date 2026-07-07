package com.mileway.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.PIN_GATE_ACCOUNT_ID
import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.sha256Hex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** PIN length `SetPinScreen`/`CheckPinScreen` accept; matches P2.3's switch-account PIN length. */
const val LOGIN_PIN_LENGTH: Int = 4

/** How many wrong verify attempts are allowed before [PinUiState.isLockedOut] latches on. */
const val LOGIN_PIN_MAX_ATTEMPTS: Int = 3

/**
 * PLAN_V22 P7.4: state for [SetPinScreen] and [CheckPinScreen]. [digits] is the entry-in-progress
 * digit string; [confirmDigits] is only used by `SetPinScreen`'s second ("confirm it") step.
 */
data class PinUiState(
    val digits: String = "",
    val confirmDigits: String = "",
    val isConfirmStep: Boolean = false,
    val error: String? = null,
    val attemptsRemaining: Int = LOGIN_PIN_MAX_ATTEMPTS,
    val isLockedOut: Boolean = false,
    /** Set once a set-PIN or verify-PIN attempt succeeds; the screen advances to `AppStage.APP` on this. */
    val completed: Boolean = false,
)

/**
 * Drives both [SetPinScreen] (first sign-in — no PIN exists yet for this session) and
 * [CheckPinScreen] (every subsequent launch, until sign-out) via the same digit-entry/lockout
 * state machine [SwitchAccountViewModel][com.mileway.feature.profile.viewmodel.SwitchAccountViewModel]
 * (P2.3) already established — reused here rather than re-invented, keyed by
 * [PIN_GATE_ACCOUNT_ID] instead of a persona id since this PIN gates the whole session, not a
 * single account switch.
 *
 * `BiometricPrompt`'s own success/failure path (`BiometricGuard`, Android-only) is orchestrated by
 * `LauncherActivity` before this ViewModel is even shown — this class only owns the PIN fallback.
 */
class PinViewModel(
    private val pinHashSource: PinHashSource,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state.asStateFlow()

    fun onDigitEntered(digit: Char) {
        val current = _state.value
        val activeLength = if (current.isConfirmStep) current.confirmDigits.length else current.digits.length
        val canAppend = !current.isLockedOut && activeLength < LOGIN_PIN_LENGTH
        if (!canAppend) return
        _state.value =
            if (current.isConfirmStep) {
                current.copy(confirmDigits = current.confirmDigits + digit, error = null)
            } else {
                current.copy(digits = current.digits + digit, error = null)
            }
    }

    fun onBackspace() {
        val current = _state.value
        val activeDigits = if (current.isConfirmStep) current.confirmDigits else current.digits
        val canRemove = !current.isLockedOut && activeDigits.isNotEmpty()
        if (!canRemove) return
        _state.value =
            if (current.isConfirmStep) {
                current.copy(confirmDigits = current.confirmDigits.dropLast(1), error = null)
            } else {
                current.copy(digits = current.digits.dropLast(1), error = null)
            }
    }

    /**
     * `SetPinScreen`: advances from the first 4-digit entry to the confirm step. No-op unless
     * exactly [LOGIN_PIN_LENGTH] digits have been entered.
     */
    fun onProceedToConfirm() {
        val current = _state.value
        if (current.digits.length != LOGIN_PIN_LENGTH) return
        _state.value = current.copy(isConfirmStep = true, error = null)
    }

    /**
     * `SetPinScreen`: checks [PinUiState.confirmDigits] against [PinUiState.digits]. On a match,
     * hashes and persists the PIN via [PinHashSource] and marks [SessionRepository.markPinSet],
     * then sets [PinUiState.completed]. On a mismatch, resets the confirm step so the reviewer
     * re-enters it rather than locking out a first-time typo.
     */
    fun confirmSetPin() {
        val current = _state.value
        if (current.digits.length != LOGIN_PIN_LENGTH || current.confirmDigits.length != LOGIN_PIN_LENGTH) return
        if (current.digits != current.confirmDigits) {
            _state.value = current.copy(confirmDigits = "", error = "PINs didn't match — try again")
            return
        }
        viewModelScope.launch {
            pinHashSource.setPinHash(PIN_GATE_ACCOUNT_ID, sha256Hex(current.digits))
            sessionRepository.markPinSet()
            _state.value = current.copy(error = null, completed = true)
        }
    }

    /**
     * `CheckPinScreen`: checks [PinUiState.digits] against the stored hash. On success, sets
     * [PinUiState.completed]; on failure, decrements [PinUiState.attemptsRemaining] and latches
     * [PinUiState.isLockedOut] once it reaches zero.
     */
    fun verify() {
        val current = _state.value
        if (current.isLockedOut || current.digits.length != LOGIN_PIN_LENGTH) return
        viewModelScope.launch {
            val storedHash = pinHashSource.getPinHash(PIN_GATE_ACCOUNT_ID)
            val enteredHash = sha256Hex(current.digits)
            if (storedHash != null && enteredHash == storedHash) {
                _state.value = current.copy(completed = true, error = null)
            } else {
                val remaining = (current.attemptsRemaining - 1).coerceAtLeast(0)
                _state.value =
                    current.copy(
                        digits = "",
                        attemptsRemaining = remaining,
                        error = if (remaining > 0) "Incorrect PIN — $remaining attempt(s) left" else "Too many attempts",
                        isLockedOut = remaining <= 0,
                    )
            }
        }
    }

    /** Resets entry/lockout state — call when the screen is (re)opened for a fresh attempt sequence. */
    fun reset() {
        _state.value = PinUiState()
    }
}

/** Koin module for [PinViewModel] — registered alongside [authModule]'s own entries. */
val pinModule =
    module {
        viewModelOf(::PinViewModel)
    }
