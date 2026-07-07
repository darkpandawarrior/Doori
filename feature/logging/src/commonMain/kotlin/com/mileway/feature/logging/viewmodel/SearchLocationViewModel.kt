package com.mileway.feature.logging.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.location.SavedLocationsSource
import com.mileway.core.platform.LocationNameResolver
import com.mileway.core.platform.LocationTracker
import com.mileway.core.platform.OfflineLocationNameResolver
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.logging.ui.model.CityCatalog
import com.mileway.feature.logging.ui.model.LocationEntry
import com.mileway.feature.logging.ui.model.PoiCategory
import com.mileway.feature.logging.ui.model.SavedPlaceUi
import com.mileway.feature.logging.ui.model.toLocationEntry
import com.mileway.feature.logging.ui.model.toSavedPlace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Immutable state for the location-search / switching sheet.
 *
 * When [query] is blank the sheet surfaces [currentLocation], [saved] chips, [favorites] and
 * [recent]; while typing it shows debounced [results] from the offline [CityCatalog].
 * [favoriteNames] lets any row (result/recent) render the correct star state.
 */
data class SearchLocationState(
    val query: String = "",
    val results: List<LocationEntry> = emptyList(),
    val recent: List<LocationEntry> = emptyList(),
    val favorites: List<LocationEntry> = emptyList(),
    val saved: List<SavedPlaceUi> = emptyList(),
    val favoriteNames: Set<String> = emptySet(),
    val currentLocation: LocationEntry? = null,
    val isLoadingCurrent: Boolean = false,
)

sealed interface SearchLocationAction {
    data class QueryChanged(val query: String) : SearchLocationAction

    data class Select(val entry: LocationEntry) : SearchLocationAction

    data class ToggleFavorite(val entry: LocationEntry) : SearchLocationAction

    data class SaveAs(val entry: LocationEntry, val label: String) : SearchLocationAction

    data class RemoveRecent(val entry: LocationEntry) : SearchLocationAction

    data class RemoveSaved(val label: String) : SearchLocationAction

    data object ClearRecent : SearchLocationAction

    data object UseCurrentLocation : SearchLocationAction
}

sealed interface SearchLocationEffect {
    /** A place was chosen — the host screen inserts it into the itinerary and dismisses the sheet. */
    data class Picked(val entry: LocationEntry) : SearchLocationEffect
}

/**
 * Drives the offline location-search sheet: debounced catalogue search plus a DataStore-backed store
 * of recents / favorites / named saved places, and a best-effort current-location fetch via the
 * platform [LocationTracker] + [LocationNameResolver] (never a real Geocoder). Every action is local
 * and offline; selecting a place records it as recent and emits [SearchLocationEffect.Picked].
 */
class SearchLocationViewModel(
    private val savedLocations: SavedLocationsSource,
    // Null in graphs/tests without a platform tracker; then current-location falls back to the
    // catalogue's demo coordinate. Koin injects the bound tracker in production.
    private val locationTracker: LocationTracker? = null,
    // Defaults to the offline resolver so tests and any graph omitting platformModule still resolve
    // real-looking names with no network.
    private val locationNameResolver: LocationNameResolver = OfflineLocationNameResolver(),
) : BaseViewModel<SearchLocationState, SearchLocationEffect, SearchLocationAction>(SearchLocationState()) {
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            savedLocations.data.collect { d ->
                setState {
                    copy(
                        recent = d.recent.map { it.toLocationEntry() },
                        favorites = d.favorites.map { it.toLocationEntry() },
                        saved = d.saved.mapNotNull { p -> p.label?.let { SavedPlaceUi(it, p.toLocationEntry()) } },
                        favoriteNames = d.favorites.map { it.name }.toSet(),
                    )
                }
            }
        }
        refreshCurrentLocation()
    }

    override fun onAction(action: SearchLocationAction) {
        when (action) {
            is SearchLocationAction.QueryChanged -> onQueryChanged(action.query)
            is SearchLocationAction.Select -> select(action.entry)
            is SearchLocationAction.ToggleFavorite ->
                viewModelScope.launch { savedLocations.toggleFavorite(action.entry.toSavedPlace(favorite = true)) }
            is SearchLocationAction.SaveAs ->
                viewModelScope.launch { savedLocations.saveAs(action.entry.toSavedPlace(label = action.label), action.label) }
            is SearchLocationAction.RemoveRecent ->
                viewModelScope.launch { savedLocations.removeRecent(action.entry.name) }
            is SearchLocationAction.RemoveSaved ->
                viewModelScope.launch { savedLocations.removeSaved(action.label) }
            SearchLocationAction.ClearRecent ->
                viewModelScope.launch { savedLocations.clearRecent() }
            SearchLocationAction.UseCurrentLocation -> useCurrentLocation()
        }
    }

    private fun onQueryChanged(query: String) {
        setState { copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            setState { copy(results = emptyList()) }
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                setState { copy(results = CityCatalog.search(query)) }
            }
    }

    private fun select(entry: LocationEntry) {
        viewModelScope.launch { savedLocations.addRecent(entry.toSavedPlace()) }
        emitEffect(SearchLocationEffect.Picked(entry))
    }

    private fun useCurrentLocation() {
        viewModelScope.launch {
            val entry = resolveCurrentLocation()
            setState { copy(currentLocation = entry, isLoadingCurrent = false) }
            select(entry)
        }
    }

    private fun refreshCurrentLocation() {
        viewModelScope.launch {
            setState { copy(isLoadingCurrent = true) }
            val entry = resolveCurrentLocation()
            setState { copy(currentLocation = entry, isLoadingCurrent = false) }
        }
    }

    /** Best-effort current location; any failure/absence falls back to the catalogue's demo point. */
    private suspend fun resolveCurrentLocation(): LocationEntry =
        runCatching {
            val point = locationTracker?.current() ?: return@runCatching CityCatalog.currentLocation
            val place = locationNameResolver.resolve(point.latitude, point.longitude)
            LocationEntry(
                name = place.name ?: "Your current location",
                subtitle = place.coordinates,
                lat = point.latitude,
                lng = point.longitude,
                category = PoiCategory.HOME,
            )
        }.getOrDefault(CityCatalog.currentLocation)

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
