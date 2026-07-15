package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.sha256Hex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Demo default so a fresh persona (never had a PIN set) is still switchable without a setup flow. */
const val DEFAULT_SWITCH_ACCOUNT_PIN: String = "1234"

/** PIN length the sheet accepts; matches the reference app's 4-digit lock-screen PIN. */
const val SWITCH_ACCOUNT_PIN_LENGTH: Int = 4

/** How many wrong attempts are allowed before [SwitchAccountUiState.isLockedOut] latches on. */
const val SWITCH_ACCOUNT_MAX_ATTEMPTS: Int = 3

/**
 * PLAN_V22 P2.3: state for [SwitchAccountPinSheet][com.mileway.feature.profile.ui.screens.SwitchAccountPinSheet].
 * Kept as local, in-memory VM state per the plan's own scope note — no Room-level lockout
 * timestamps, this is a demo-app friction gate, not a security boundary. The lockout resets only
 * when the sheet is dismissed and reopened (`reset()`), never automatically, mirroring a simple
 * "come back and try again" UX rather than a server-validated cooldown timer.
 */
data class SwitchAccountUiState(
    val enteredDigits: String = "",
    val attemptsRemaining: Int = SWITCH_ACCOUNT_MAX_ATTEMPTS,
    val error: String? = null,
    val isLockedOut: Boolean = false,
    /** Set once [PinHashSource] confirms the correct PIN; the caller commits the switch on this. */
    val verified: Boolean = false,
)

/**
 * Holds the PIN-entry state for [SwitchAccountPinSheet][com.mileway.feature.profile.ui.screens.SwitchAccountPinSheet]
 * and checks entries against [PinHashSource] (falling back to [DEFAULT_SWITCH_ACCOUNT_PIN] for a
 * persona that has never had a PIN set — matching [MockAccountRepository][com.mileway.feature.profile.repository.MockAccountRepository]'s
 * seeded demo personas, which ship with no PIN of their own). Biometric gating
 * ([BiometricGuard][com.siddharth.kmp.security.BiometricGuard]) is orchestrated by the Android-only
 * `ProfileScreen`/`ProfileViewModel` layer instead of here, since `BiometricPrompt` needs a
 * `FragmentActivity` this `commonMain` class can't reference.
 */
class SwitchAccountViewModel(private val pinHashSource: PinHashSource) : ViewModel() {
    private val _state = MutableStateFlow(SwitchAccountUiState())
    val state: StateFlow<SwitchAccountUiState> = _state.asStateFlow()

    /** Resets entry/lockout state — call when the sheet opens for a fresh attempt sequence. */
    fun reset() {
        _state.value = SwitchAccountUiState()
    }

    fun onDigitEntered(digit: Char) {
        val current = _state.value
        if (current.isLockedOut || current.enteredDigits.length >= SWITCH_ACCOUNT_PIN_LENGTH) return
        _state.value = current.copy(enteredDigits = current.enteredDigits + digit, error = null)
    }

    fun onBackspace() {
        val current = _state.value
        if (current.isLockedOut || current.enteredDigits.isEmpty()) return
        _state.value = current.copy(enteredDigits = current.enteredDigits.dropLast(1), error = null)
    }

    /** Hashes and persists a fresh PIN for [accountId], replacing whatever it had (or the default). */
    fun setPin(
        accountId: String,
        pin: String,
    ) {
        viewModelScope.launch { pinHashSource.setPinHash(accountId, sha256Hex(pin)) }
    }

    /**
     * Checks [SwitchAccountUiState.enteredDigits] against [accountId]'s stored hash. On success,
     * sets [SwitchAccountUiState.verified]; on failure, decrements [SwitchAccountUiState.attemptsRemaining]
     * and latches [SwitchAccountUiState.isLockedOut] once it reaches zero.
     */
    fun verify(accountId: String) {
        val current = _state.value
        if (current.isLockedOut) return
        viewModelScope.launch {
            val storedHash = pinHashSource.getPinHash(accountId) ?: sha256Hex(DEFAULT_SWITCH_ACCOUNT_PIN)
            val enteredHash = sha256Hex(current.enteredDigits)
            if (enteredHash == storedHash) {
                _state.value = current.copy(verified = true, error = null)
            } else {
                val remaining = (current.attemptsRemaining - 1).coerceAtLeast(0)
                _state.value =
                    current.copy(
                        enteredDigits = "",
                        attemptsRemaining = remaining,
                        error = if (remaining > 0) "Incorrect PIN — $remaining attempt(s) left" else "Too many attempts",
                        isLockedOut = remaining <= 0,
                    )
            }
        }
    }
}
