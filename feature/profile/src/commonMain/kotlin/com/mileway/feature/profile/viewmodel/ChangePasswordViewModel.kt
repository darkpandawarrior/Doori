package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.CREDENTIAL_ACCOUNT_ID
import com.mileway.core.data.session.CredentialSource
import com.mileway.core.data.session.PasswordPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which validation failure the change-password sheet should surface. */
enum class ChangePasswordError { WRONG_CURRENT, TOO_SHORT, MISMATCH }

data class ChangePasswordUiState(
    val current: String = "",
    val newPassword: String = "",
    val confirm: String = "",
    val error: ChangePasswordError? = null,
    val strength: PasswordPolicy.Strength = PasswordPolicy.Strength.WEAK,
    val done: Boolean = false,
) {
    val canSubmit: Boolean
        get() = current.isNotEmpty() && newPassword.isNotEmpty() && confirm.isNotEmpty() && !done
}

/**
 * PLAN_V24 P1.5 — the change-password flow (per the reference app's change-password shape):
 * verify the original against the mock [CredentialSource], validate the new one
 * (min length via [PasswordPolicy]) and its confirmation, then persist the new salted hash.
 */
class ChangePasswordViewModel(
    private val credentials: CredentialSource,
) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(value: String) {
        _state.value = _state.value.copy(current = value, error = null)
    }

    fun onNewChange(value: String) {
        _state.value = _state.value.copy(newPassword = value, strength = PasswordPolicy.strength(value), error = null)
    }

    fun onConfirmChange(value: String) {
        _state.value = _state.value.copy(confirm = value, error = null)
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            when {
                !credentials.verify(CREDENTIAL_ACCOUNT_ID, current.current) ->
                    _state.value = current.copy(error = ChangePasswordError.WRONG_CURRENT)
                !PasswordPolicy.isValid(current.newPassword) ->
                    _state.value = current.copy(error = ChangePasswordError.TOO_SHORT)
                current.newPassword != current.confirm ->
                    _state.value = current.copy(error = ChangePasswordError.MISMATCH)
                else -> {
                    credentials.setPassword(CREDENTIAL_ACCOUNT_ID, current.newPassword)
                    _state.value = current.copy(error = null, done = true)
                }
            }
        }
    }
}
