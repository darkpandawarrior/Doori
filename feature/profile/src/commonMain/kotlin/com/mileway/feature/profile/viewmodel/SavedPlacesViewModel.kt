package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.feature.profile.model.SavedPlace
import com.mileway.feature.profile.model.SavedPlaceType
import com.mileway.feature.profile.repository.SavedPlacesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P3.4: state for `SavedPlacesScreen`. The list is Room-backed (survives navigation and
 * process death). [submitError] surfaces the blank-label/blank-address and bad-coordinate
 * validation gates; it is cleared on the next successful save or via [clearSubmitError].
 */
data class SavedPlacesUiState(
    val places: List<SavedPlace> = emptyList(),
    val submitError: String? = null,
)

class SavedPlacesViewModel(private val repository: SavedPlacesRepository) : ViewModel() {
    private val _state = MutableStateFlow(SavedPlacesUiState())
    val state: StateFlow<SavedPlacesUiState> = _state.asStateFlow()

    init {
        repository.observeAll().onEach { list -> _state.update { it.copy(places = list) } }.launchIn(viewModelScope)
    }

    /**
     * Adds a new place (blank [id]) or updates an existing one. [label] and [address] must be
     * non-blank. [latText]/[lngText] are optional free text — if either is non-blank it must parse
     * to a valid coordinate (and both must then be present), otherwise the save is rejected with a
     * [SavedPlacesUiState.submitError]. Returns true when the save was accepted.
     */
    fun save(
        id: String,
        type: SavedPlaceType,
        label: String,
        address: String,
        latText: String,
        lngText: String,
    ): Boolean {
        if (label.isBlank() || address.isBlank()) {
            _state.update { it.copy(submitError = "Enter a label and address before saving.") }
            return false
        }
        val coords = parseCoordinates(latText.trim(), lngText.trim())
        if (coords == null && (latText.isNotBlank() || lngText.isNotBlank())) {
            _state.update { it.copy(submitError = "Coordinates must be a valid latitude and longitude, or leave both blank.") }
            return false
        }
        viewModelScope.launch {
            repository.save(
                id = id,
                type = type,
                label = label.trim(),
                address = address.trim(),
                lat = coords?.first,
                lng = coords?.second,
            )
        }
        _state.update { it.copy(submitError = null) }
        return true
    }

    /** Deletes [id] outright. */
    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** Dismisses [SavedPlacesUiState.submitError] without changing any persisted state. */
    fun clearSubmitError() {
        _state.update { it.copy(submitError = null) }
    }
}

/**
 * Parses a lat/lng pair. Returns null when either field is blank or unparseable, or when the
 * values fall outside valid geographic ranges (lat ±90, lng ±180). Both must be present together.
 */
internal fun parseCoordinates(
    latText: String,
    lngText: String,
): Pair<Double, Double>? {
    if (latText.isBlank() || lngText.isBlank()) return null
    val lat = latText.toDoubleOrNull() ?: return null
    val lng = lngText.toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return lat to lng
}
