package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.emergency.EmergencyContact
import com.mileway.core.data.emergency.EmergencyContactsRepository
import com.mileway.core.data.emergency.MAX_EMERGENCY_CONTACTS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** National-number length the reference app requires (India, matching P1.1/P3.1 phone rules). */
private const val NATIONAL_LENGTH = 10

/**
 * PLAN_V24 P3.5: state for `EmergencyContactsScreen`. Room-backed list (via
 * [EmergencyContactsRepository]) capped at [MAX_EMERGENCY_CONTACTS]. [submitError] surfaces the
 * blank-name/invalid-phone and at-capacity gates; cleared on the next accepted save or via
 * [clearSubmitError].
 */
data class EmergencyContactsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val submitError: String? = null,
) {
    val isAtCapacity: Boolean get() = contacts.size >= MAX_EMERGENCY_CONTACTS
}

class EmergencyContactsViewModel(private val repository: EmergencyContactsRepository) : ViewModel() {
    private val _state = MutableStateFlow(EmergencyContactsUiState())
    val state: StateFlow<EmergencyContactsUiState> = _state.asStateFlow()

    init {
        repository.observeAll().onEach { list -> _state.update { it.copy(contacts = list) } }.launchIn(viewModelScope)
    }

    /**
     * Adds a new contact (blank [id]) or updates an existing one. [name] must be non-blank and
     * [phone] must normalize to a [NATIONAL_LENGTH]-digit national number; otherwise the save is
     * rejected with a [EmergencyContactsUiState.submitError]. A rejected cap (5 already stored)
     * also surfaces an error. Returns true when the save was accepted.
     */
    fun save(
        id: String,
        name: String,
        phone: String,
        countryCode: String,
    ): Boolean {
        val national = normalizePhone(phone)
        if (name.isBlank()) {
            _state.update { it.copy(submitError = "Enter a contact name.") }
            return false
        }
        if (national.length != NATIONAL_LENGTH) {
            _state.update { it.copy(submitError = "Enter a valid $NATIONAL_LENGTH-digit phone number.") }
            return false
        }
        viewModelScope.launch {
            val accepted = repository.save(id = id, name = name.trim(), phoneNo = national, countryCode = countryCode)
            if (!accepted) {
                _state.update { it.copy(submitError = "You can save at most $MAX_EMERGENCY_CONTACTS emergency contacts.") }
            } else {
                _state.update { it.copy(submitError = null) }
            }
        }
        return true
    }

    /** Deletes [id] outright. */
    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Dismisses [EmergencyContactsUiState.submitError] without changing persisted contacts. */
    fun clearSubmitError() {
        _state.update { it.copy(submitError = null) }
    }
}

/** Digits-only, dropping a single leading 0 — the same inline rule P3.1's phone change uses. */
internal fun normalizePhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.startsWith("0")) digits.drop(1) else digits
}
