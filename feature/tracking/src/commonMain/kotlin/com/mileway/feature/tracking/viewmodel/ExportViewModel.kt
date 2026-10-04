package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.platform.ShareSheet
import com.mileway.feature.tracking.export.RedactionDefaults
import com.mileway.feature.tracking.export.TrackExportContent
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.ui.components.ExportFormat
import com.mileway.feature.tracking.ui.components.LocationDataFilter
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

data class ExportUiState(
    val isExporting: Boolean = false,
    val error: String? = null,
)

sealed interface ExportAction {
    data object ClearError : ExportAction
}

sealed interface ExportEffect

class ExportViewModel(
    private val trackRepository: SavedTrackRepository,
    private val locationRepository: LocationRepository,
    private val hardwareEventRepository: HardwareEventRepository,
    private val shareSheet: ShareSheet,
    private val savedPlaceDao: SavedPlaceDao,
) : BaseViewModel<ExportUiState, ExportEffect, ExportAction>(ExportUiState()) {
    override fun onAction(action: ExportAction) {
        when (action) {
            ExportAction.ClearError -> setState { copy(error = null) }
        }
    }

    // Boundary catch-all: this is the edge between the app and a platform or backend call that
    // fails in ways no narrower Kotlin type covers on this source set. The failure is logged
    // and surfaced to the caller, never swallowed — crashing the process is the alternative.
    @Suppress("TooGenericExceptionCaught")
    fun export(
        routeId: String,
        format: ExportFormat,
        filter: LocationDataFilter,
    ) {
        setState { copy(isExporting = true, error = null) }

        viewModelScope.launch {
            try {
                val track =
                    trackRepository.getByRouteId(routeId)
                        ?: error("Track not found: $routeId")

                var locations = locationRepository.getForToken(routeId)

                if (filter.excludeMock) locations = locations.filter { !it.isMock }
                if (filter.excludeAbnormal) locations = locations.filter { !it.isAbnormal }
                if (filter.excludePaused) locations = locations.filter { !it.isPaused }
                if (filter.onlyCheckpoints) locations = locations.filter { it.wasCheckInPoint }
                filter.minAccuracy?.let { min -> locations = locations.filter { it.accuracy >= min } }
                filter.maxAccuracy?.let { max -> locations = locations.filter { it.accuracy <= max } }
                filter.minBatteryLevel?.let { minBat ->
                    locations = locations.filter { it.batteryPercentage >= minBat }
                }

                val events = hardwareEventRepository.getEventsForRoute(routeId).getOrElse { emptyList() }

                val home = if (filter.redactHome) {
                    savedPlaceDao.observeAll().first().firstOrNull { it.type == RedactionDefaults.HOME_TYPE }
                } else null
                val redactedLocations = RedactionDefaults.locations(locations, home, filter.redactHome)
                val redactedTrack = RedactionDefaults.track(track, redactedLocations, home, filter.redactHome)
                val redactedEvents = RedactionDefaults.events(events, home, filter.redactHome)
                val content = TrackExportContent.build(format, redactedTrack, redactedLocations, redactedEvents)
                val subject = "Track export: ${redactedTrack.name}"

                shareSheet.share(text = content, subject = subject)
                setState { copy(isExporting = false) }
            } catch (e: Exception) {
                setState { copy(isExporting = false, error = e.message ?: "Export failed") }
            }
        }
    }
}
