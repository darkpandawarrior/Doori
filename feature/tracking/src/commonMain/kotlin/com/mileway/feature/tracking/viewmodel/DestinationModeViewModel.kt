package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.data.location.DESTINATION_GUEST_KEY
import com.mileway.core.data.location.DESTINATION_REGIONS
import com.mileway.core.data.location.DestinationModeRepository
import com.mileway.core.data.location.destinationRemainingMs
import com.mileway.core.data.location.isDestinationActive
import com.mileway.core.data.location.parseSelectedRegions
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** One saved place the head-home picker can choose from. */
data class PlacePick(
    val id: String,
    val label: String,
    val address: String,
    val lat: Double?,
    val lng: Double?,
)

/** One region-preference chip (id → localizable name key + selected flag). */
data class RegionChip(
    val id: String,
    val name: String,
    val selected: Boolean,
)

/** Immutable UI model for the "Head home" panel on the tracking screen. */
data class DestinationUiState(
    val places: List<PlacePick> = emptyList(),
    val active: Boolean = false,
    val address: String = "",
    val remainingMs: Long = 0L,
    val taggedTrips: Int = 0,
    val regions: List<RegionChip> = emptyList(),
)

/**
 * PLAN_V24 P11.3 — drives the "Head home" destination panel. Reads the per-account destination
 * store (address + live countdown), the saved places to pick from, the count of trips already
 * auto-classified toward home, and the region-preference set. Pure/offline over Room.
 *
 * The countdown ticks once a second by combining the persisted row with a 1-second tick flow and
 * recomputing the remaining time with the pure [destinationRemainingMs] rule.
 */
class DestinationModeViewModel(
    private val repository: DestinationModeRepository,
    private val savedPlaceDao: SavedPlaceDao,
    private val savedTrackRepo: SavedTrackRepository,
    private val activeAccountSource: ActiveAccountSource,
) : ViewModel() {
    private val _state = MutableStateFlow(DestinationUiState())
    val state: StateFlow<DestinationUiState> = _state.asStateFlow()

    private val tick =
        flow {
            while (true) {
                emit(Unit)
                delay(1_000L)
            }
        }

    init {
        observeState()
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeState(): Flow<DestinationUiState> =
        activeAccountSource.activeAccountId
            .flatMapLatest { accountId ->
                val acct = accountId ?: DESTINATION_GUEST_KEY
                combine(
                    repository.observeEntity(acct),
                    savedPlaceDao.observeAll(),
                    savedTrackRepo.rawTracksFlow(),
                    tick,
                ) { row, places, tracks, _ ->
                    val taggedCount = tracks.count { it.destinationTag != null }
                    val now = Clock.System.now().toEpochMilliseconds()
                    val active = isDestinationActive(row?.expiresAt, now)
                    val selected = parseSelectedRegions(row?.selectedRegionsCsv ?: "")
                    DestinationUiState(
                        places =
                            places.map {
                                PlacePick(it.id, it.label, it.address, it.latitude, it.longitude)
                            },
                        active = active,
                        address = if (active) row?.address.orEmpty() else "",
                        remainingMs = destinationRemainingMs(row?.expiresAt, now),
                        taggedTrips = taggedCount,
                        regions =
                            DESTINATION_REGIONS.map {
                                RegionChip(it.id, it.name, it.id in selected)
                            },
                    )
                }
            }

    private suspend fun account(): String = activeAccountSource.activeAccountId.first() ?: DESTINATION_GUEST_KEY

    fun pickDestination(place: PlacePick) {
        viewModelScope.launch {
            repository.activate(account(), place.id, place.address.ifBlank { place.label }, place.lat, place.lng)
        }
    }

    fun disable() {
        viewModelScope.launch { repository.disable(account()) }
    }

    fun toggleRegion(id: String) {
        viewModelScope.launch { repository.toggleRegion(account(), id) }
    }
}
