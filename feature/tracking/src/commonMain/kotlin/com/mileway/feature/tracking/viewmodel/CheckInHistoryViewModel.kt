package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.LocationData
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.ui.screens.CheckInHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Backs [CheckInHistoryScreen] with the real check-in trail: every [LocationData] row where
 * `wasCheckInPoint` is true, across every trip. Trivial by design — a single filtered query,
 * mapped straight into the screen's flat UI model.
 */
class CheckInHistoryViewModel(
    locationRepository: LocationRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<CheckInHistoryItem>>(emptyList())
    val items: StateFlow<List<CheckInHistoryItem>> = _items.asStateFlow()

    init {
        locationRepository
            .allCheckInPoints()
            .map { points -> points.map { it.toCheckInHistoryItem() } }
            .onEach { _items.value = it }
            .launchIn(viewModelScope)
    }
}

private fun LocationData.toCheckInHistoryItem(): CheckInHistoryItem =
    CheckInHistoryItem(
        id = id.toString(),
        title = miscellaneous.ifBlank { if (checkInType == "MANUAL") "Manual check-in" else "Geo check-in" },
        subtitle = reason?.takeIf { it.isNotBlank() },
        timestampMillis = date,
        lat = lat,
        lng = lng,
        type = checkInType,
        isManual = checkInType == "MANUAL",
    )
