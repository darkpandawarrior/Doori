package com.miletracker.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.miletracker.core.platform.ShareSheet
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.tracking.export.TrackExportContent
import com.miletracker.feature.tracking.repository.HardwareEventRepository
import com.miletracker.feature.tracking.repository.LocationRepository
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import com.miletracker.feature.tracking.ui.components.ExportFormat
import com.miletracker.feature.tracking.ui.components.LocationDataFilter
import kotlinx.coroutines.launch

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
) : BaseViewModel<ExportUiState, ExportEffect, ExportAction>(ExportUiState()) {
    override fun onAction(action: ExportAction) {
        when (action) {
            ExportAction.ClearError -> setState { copy(error = null) }
        }
    }

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

                val content = TrackExportContent.build(format, track, locations, events)
                val subject = "Track export: ${track.name}"

                shareSheet.share(text = content, subject = subject)
                setState { copy(isExporting = false) }
            } catch (e: Exception) {
                setState { copy(isExporting = false, error = e.message ?: "Export failed") }
            }
        }
    }
}
