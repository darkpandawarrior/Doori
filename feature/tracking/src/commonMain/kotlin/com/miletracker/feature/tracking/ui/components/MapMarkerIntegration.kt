package com.miletracker.feature.tracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.ui.theme.DesignTokens

// ---------------------------------------------------------------------------
// Map-marker domain types for the Compose marker UI. These model marker
// semantics (position, severity, event type) independently of any map SDK;
// the osmdroid binding lives in the map package.
// ---------------------------------------------------------------------------

/** Geographic position used for map markers. */
data class MapPosition(val latitude: Double, val longitude: Double)

/**
 * Severity levels for visual prioritization and filtering.
 */
enum class MarkerSeverity(val label: String, val color: Color) {
    INFO("Info", DesignTokens.StatusColors.info),
    WARNING("Warning", DesignTokens.StatusColors.warning),
    ERROR("Error", DesignTokens.StatusColors.error),
    CRITICAL("Critical", DesignTokens.StatusColors.error),
}

/**
 * Categories of markers that can be displayed on the map.
 */
enum class MarkerType {
    LOCATION_POINT, // Regular GPS location points
    HARDWARE_EVENT, // Hardware/system events from event log
    CHECK_IN, // User check-in points
    PAUSE_EVENT, // Journey pause/resume points
    SYSTEM_STATE, // System state changes
    DATA_QUALITY, // Data quality indicators (mock, abnormal, etc.)
}

/**
 * Unified marker representation for map visualization.
 */
data class MapMarkerData(
    val id: String,
    val position: MapPosition,
    val type: MarkerType,
    val timestamp: Long,
    val title: String,
    val details: Map<String, String>,
    val severity: MarkerSeverity,
    val iconRes: Int,
    val iconTint: Color,
    val isClickable: Boolean = true,
    val eventType: EventType? = null,
)

/**
 * Filter configuration for map markers.
 *
 * Default configuration prioritizes clean map visualization:
 * - Hardware events OFF by default (can be overwhelming)
 * - System state OFF by default (related to hardware events)
 * - Data quality OFF by default (usually indicates issues)
 * - Only check-ins and pause events ON by default for key user actions
 */
data class MarkerFilters(
    // Type filters
    val showCheckIns: Boolean = true,
    val showHardwareEvents: Boolean = false,
    val showLocationPoints: Boolean = false,
    val showPauseEvents: Boolean = true,
    val showSystemState: Boolean = false,
    val showDataQuality: Boolean = false,
    // Severity filters (all on by default)
    val severityFilter: Set<MarkerSeverity> = MarkerSeverity.entries.toSet(),
    // Event type filters (all on by default)
    val eventTypeFilter: Set<EventType> = EventType.entries.toSet(),
    // Time range (default to all time)
    val minTimestamp: Long = 0L,
    val maxTimestamp: Long = Long.MAX_VALUE,
)

/**
 * Marker filter chips component for the Layers tab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkerFilterChips(
    filters: MarkerFilters,
    onFiltersChanged: (MarkerFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Map Markers",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filters.showCheckIns,
                onClick = { onFiltersChanged(filters.copy(showCheckIns = !filters.showCheckIns)) },
                label = { Text("Check-ins") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )

            FilterChip(
                selected = filters.showHardwareEvents,
                onClick = { onFiltersChanged(filters.copy(showHardwareEvents = !filters.showHardwareEvents)) },
                label = { Text("Hardware Events") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            )

            FilterChip(
                selected = filters.showPauseEvents,
                onClick = { onFiltersChanged(filters.copy(showPauseEvents = !filters.showPauseEvents)) },
                label = { Text("Pauses") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
            )

            FilterChip(
                selected = filters.showDataQuality,
                onClick = { onFiltersChanged(filters.copy(showDataQuality = !filters.showDataQuality)) },
                label = { Text("Data Issues") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
            )

            FilterChip(
                selected = filters.showSystemState,
                onClick = { onFiltersChanged(filters.copy(showSystemState = !filters.showSystemState)) },
                label = { Text("System Events") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )

            FilterChip(
                selected = filters.showLocationPoints,
                onClick = { onFiltersChanged(filters.copy(showLocationPoints = !filters.showLocationPoints)) },
                label = { Text("Location Points") },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }

        // Severity filters
        Text(
            text = "Severity Filters",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MarkerSeverity.entries.forEach { severity ->
                val isSelected = filters.severityFilter.contains(severity)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSeverities =
                            if (isSelected) {
                                filters.severityFilter - severity
                            } else {
                                filters.severityFilter + severity
                            }
                        onFiltersChanged(filters.copy(severityFilter = newSeverities))
                    },
                    label = { Text(severity.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = severity.color,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                )
            }
        }
    }
}
