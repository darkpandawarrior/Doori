package com.mileway.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.SessionRepository
import com.mileway.feature.profile.admin.RateTables
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Which onboarding field failed validation (config-driven required/optional). */
enum class OnboardingField { FIRST_NAME, LAST_NAME, EMAIL, GENDER, DOB }

/**
 * PLAN_V24 P2.1 — the gating flags from the signup-onboarding plugins (per the reference app's
 * signup onboarding). The screen resolves these from the registry and passes them via
 * [SignupOnboardingViewModel.configure] so the validation stays a pure function of config + input.
 */
data class OnboardingFormConfig(
    val lastNameOptional: Boolean = true,
    val emailOptional: Boolean = true,
    val genderRequired: Boolean = false,
    val dobRequired: Boolean = false,
    val showPromo: Boolean = false,
    val showSkip: Boolean = true,
)

data class OnboardingUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val gender: String = "",
    val dateOfBirthMillis: Long? = null,
    val referralCode: String = "",
    val config: OnboardingFormConfig = OnboardingFormConfig(),
    val errors: Set<OnboardingField> = emptySet(),
    val reviewingPolicy: Boolean = false,
    val skipProfile: Boolean = false,
    val policySummary: List<String>? = null,
    val policyLoading: Boolean = false,
    val policyError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val done: Boolean = false,
)

/**
 * PLAN_V24 P2.1 — drives the config-driven signup onboarding form. First name is always required;
 * last name/email are required unless their optional flag is set (email is format-checked when
 * present); gender/DOB are required only when their flag is set. Submit persists via
 * [SessionRepository.saveOnboarding] only after policy review. Skip bypasses the profile fields,
 * but still shows the policy summary before marking onboarding done.
 */
class SignupOnboardingViewModel(
    private val sessionRepository: SessionRepository,
    private val readRateTables: suspend () -> RateTables,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun configure(config: OnboardingFormConfig) {
        _state.value = _state.value.copy(config = config)
    }

    fun onFirstNameChange(v: String) = clearAnd { copy(firstName = v) }

    fun onLastNameChange(v: String) = clearAnd { copy(lastName = v) }

    fun onEmailChange(v: String) = clearAnd { copy(email = v) }

    fun onGenderChange(v: String) = clearAnd { copy(gender = v) }

    fun onDobChange(v: Long?) = clearAnd { copy(dateOfBirthMillis = v) }

    fun onReferralChange(v: String) = clearAnd { copy(referralCode = v) }

    /** The set of fields that fail validation for the current config (empty = valid). */
    fun validate(state: OnboardingUiState = _state.value): Set<OnboardingField> {
        val errors = mutableSetOf<OnboardingField>()
        val c = state.config
        if (state.firstName.isBlank()) errors += OnboardingField.FIRST_NAME
        if (!c.lastNameOptional && state.lastName.isBlank()) errors += OnboardingField.LAST_NAME
        if (!c.emailOptional && state.email.isBlank()) {
            errors += OnboardingField.EMAIL
        } else if (state.email.isNotBlank() && !isEmail(state.email)) {
            errors += OnboardingField.EMAIL
        }
        if (c.genderRequired && state.gender.isBlank()) errors += OnboardingField.GENDER
        if (c.dobRequired && state.dateOfBirthMillis == null) errors += OnboardingField.DOB
        return errors
    }

    fun submit() {
        val current = _state.value
        if (current.saving || current.done || current.reviewingPolicy) return
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _state.value = current.copy(errors = errors)
            return
        }
        _state.value = current.copy(errors = emptySet(), reviewingPolicy = true, skipProfile = false, saveError = null)
        loadPolicy()
    }

    fun skip() {
        val current = _state.value
        if (!current.config.showSkip) return
        if (current.saving || current.done || current.reviewingPolicy) return
        _state.value = current.copy(errors = emptySet(), reviewingPolicy = true, skipProfile = true, saveError = null)
        loadPolicy()
    }

    /** Reads the existing local rate store; a failed read can be retried without completing signup. */
    @Suppress("TooGenericExceptionCaught")
    fun loadPolicy() {
        val current = _state.value
        if (!current.reviewingPolicy || current.done) return
        if (current.policyLoading || current.saving) return
        _state.value = _state.value.copy(policyLoading = true, policySummary = null, policyError = null)
        viewModelScope.launch {
            try {
                val summary = signupPolicySummary(readRateTables(), nowMillis())
                _state.value = _state.value.copy(policySummary = summary)
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                _state.value = _state.value.copy(policyError = "Unable to read local policy rates. Please retry.")
            } finally {
                _state.value = _state.value.copy(policyLoading = false)
            }
        }
    }

    /** Returns to the unsaved profile without losing entered fields. */
    fun backToProfile() {
        if (_state.value.saving || _state.value.policyLoading || _state.value.done) return
        _state.value = _state.value.copy(reviewingPolicy = false, skipProfile = false, saveError = null)
    }

    /** Persists completion only after the user continues from the policy summary. */
    @Suppress("TooGenericExceptionCaught")
    fun confirmPolicy() {
        val current = _state.value
        if (!current.reviewingPolicy || current.done) return
        if (current.policySummary == null || current.policyLoading || current.saving) return
        _state.value = current.copy(saving = true, saveError = null)
        viewModelScope.launch {
            try {
                if (current.skipProfile) {
                    sessionRepository.skipOnboarding()
                } else {
                    val displayName = listOf(current.firstName, current.lastName).filter { it.isNotBlank() }.joinToString(" ")
                    sessionRepository.saveOnboarding(
                        displayName = displayName,
                        email = current.email.ifBlank { null },
                        gender = current.gender,
                        dateOfBirthMillis = current.dateOfBirthMillis,
                    )
                }
                _state.value = current.copy(done = true, saving = false, saveError = null)
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                _state.value = current.copy(saving = false, saveError = "Unable to save onboarding. Please retry.")
            }
        }
    }

    private fun clearAnd(reducer: OnboardingUiState.() -> OnboardingUiState) {
        _state.value = _state.value.reducer().copy(errors = emptySet())
    }

    private fun isEmail(value: String): Boolean = value.contains('@') && value.substringAfter('@').contains('.')
}
