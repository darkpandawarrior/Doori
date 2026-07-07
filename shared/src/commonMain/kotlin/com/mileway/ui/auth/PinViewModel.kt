package com.mileway.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.PIN_GATE_ACCOUNT_ID
import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.PinLockoutPolicy
import com.mileway.core.data.session.PinLockoutSource
import com.mileway.core.data.session.PinLockoutState
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.sha256Hex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

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
    /** PLAN_V24 P1.4: seconds left on the current tiered lockout window (0 when not locked). */
    val lockoutRemainingSeconds: Int = 0,
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
    // PLAN_V24 P1.4: tiered lockout state, persisted per account; defaulted clock for testability.
    private val pinLockoutSource: PinLockoutSource,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state.asStateFlow()

    private var lockoutTicker: Job? = null

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
     * `CheckPinScreen`: checks [PinUiState.digits] against the stored hash. PLAN_V24 P1.4: a wrong
     * entry increments the persisted per-account failed-attempt count and applies
     * [PinLockoutPolicy]'s escalating lockout window; a correct entry clears the counters. The
     * lockout is persisted so a kill can't reset the backoff.
     */
    fun verify() {
        val current = _state.value
        if (current.isLockedOut || current.digits.length != LOGIN_PIN_LENGTH) return
        viewModelScope.launch {
            val now = clock.now().toEpochMilliseconds()
            val lockout = pinLockoutSource.getState(PIN_GATE_ACCOUNT_ID)
            if (lockout.isLocked(now)) {
                enterLockout(lockout.remainingSeconds(now))
                return@launch
            }
            val storedHash = pinHashSource.getPinHash(PIN_GATE_ACCOUNT_ID)
            if (storedHash != null && sha256Hex(current.digits) == storedHash) {
                pinLockoutSource.clear(PIN_GATE_ACCOUNT_ID)
                _state.value = current.copy(completed = true, error = null)
            } else {
                val attempts = lockout.failedAttempts + 1
                val lockMillis = PinLockoutPolicy.lockoutMillisFor(attempts)
                val until = if (lockMillis > 0) now + lockMillis else 0L
                pinLockoutSource.setState(PIN_GATE_ACCOUNT_ID, PinLockoutState(attempts, until))
                if (lockMillis > 0) {
                    enterLockout((lockMillis / 1000).toInt())
                } else {
                    val free = (PinLockoutPolicy.FREE_ATTEMPTS - attempts).coerceAtLeast(0)
                    _state.value =
                        current.copy(
                            digits = "",
                            attemptsRemaining = free,
                            error = "Incorrect PIN — $free attempt(s) before lockout",
                        )
                }
            }
        }
    }

    /** Enters the locked state and ticks the countdown down to 0, then unlocks. Local decrement so a frozen test clock still terminates. */
    private fun enterLockout(seconds: Int) {
        _state.value = _state.value.copy(digits = "", isLockedOut = true, lockoutRemainingSeconds = seconds, error = null)
        lockoutTicker?.cancel()
        lockoutTicker =
            viewModelScope.launch {
                var remaining = seconds
                while (remaining > 0) {
                    delay(1_000)
                    remaining -= 1
                    _state.value = _state.value.copy(lockoutRemainingSeconds = remaining)
                }
                _state.value = _state.value.copy(isLockedOut = false, lockoutRemainingSeconds = 0)
            }
    }

    /**
     * Resets entry state and re-hydrates any persisted lockout — call when the screen is (re)opened.
     * A relaunch while still locked re-enters the lockout with its remaining countdown.
     */
    fun reset() {
        lockoutTicker?.cancel()
        _state.value = PinUiState()
        viewModelScope.launch {
            val now = clock.now().toEpochMilliseconds()
            val lockout = pinLockoutSource.getState(PIN_GATE_ACCOUNT_ID)
            if (lockout.isLocked(now)) enterLockout(lockout.remainingSeconds(now))
        }
    }
}

/** Koin module for [PinViewModel] — registered alongside [authModule]'s own entries. */
val pinModule =
    module {
        viewModelOf(::PinViewModel)
    }
