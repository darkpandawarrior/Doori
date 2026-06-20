package com.miletracker.feature.tracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.sheet.AppActionSheet

// ---------------------------------------------------------------------------
// Domain types for the export options UI.
// ---------------------------------------------------------------------------

/**
 * Supported export formats for location data.
 */
enum class ExportFormat(
    val displayName: String,
    val fileExtension: String,
    val mimeType: String,
    val description: String,
) {
    CSV(
        displayName = "CSV (Spreadsheet)",
        fileExtension = "csv",
        mimeType = "text/csv",
        description = "Comma-separated values, suitable for Excel and data analysis",
    ),
    JSON(
        displayName = "JSON (Structured)",
        fileExtension = "json",
        mimeType = "application/json",
        description = "JavaScript Object Notation, ideal for technical analysis and APIs",
    ),
    GPX(
        displayName = "GPX (GPS Exchange)",
        fileExtension = "gpx",
        mimeType = "application/gpx+xml",
        description = "Standard GPS format, compatible with most mapping and fitness apps",
    ),
    KML(
        displayName = "KML (Google Earth)",
        fileExtension = "kml",
        mimeType = "application/vnd.google-earth.kml+xml",
        description = "Keyhole Markup Language, for Google Earth and Google Maps",
    ),
    GEOJSON(
        displayName = "GeoJSON (Geographic)",
        fileExtension = "geojson",
        mimeType = "application/geo+json",
        description = "Geographic JSON format, widely supported by mapping libraries",
    ),
}

/**
 * Filter options for location data queries.
 */
data class LocationDataFilter(
    val minAccuracy: Float? = null,
    val maxAccuracy: Float? = null,
    val excludePaused: Boolean = false,
    val excludeMock: Boolean = false,
    val excludeAbnormal: Boolean = false,
    val onlyCheckpoints: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val minBatteryLevel: Double? = null,
    val specificProviders: List<String>? = null,
)

// ---------------------------------------------------------------------------

/**
 * Dialog for configuring location data export options.
 * Allows users to select format, apply filters, and configure export parameters.
 */
@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat, LocationDataFilter) -> Unit,
    trackName: String? = null,
    isMultipleTrackExport: Boolean = false,
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    // Filter options
    var excludePaused by remember { mutableStateOf(false) }
    var excludeMock by remember { mutableStateOf(true) }
    var excludeAbnormal by remember { mutableStateOf(true) }
    var onlyCheckpoints by remember { mutableStateOf(false) }
    var minAccuracy by remember { mutableFloatStateOf(0f) }
    var maxAccuracy by remember { mutableFloatStateOf(0f) }
    var minBatteryText by remember { mutableStateOf("") }

    var showAdvancedFilters by remember { mutableStateOf(false) }

    AppActionSheet(
        onDismiss = onDismiss,
        title = if (isMultipleTrackExport) "Export Multiple Tracks" else "Export Track Data",
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Track info
            trackName?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exporting: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Format selection
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                ExportFormat.values().forEach { format ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = format.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quality filters
            Text(
                text = "Data Quality Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Basic filters
            FilterCheckbox(
                checked = excludeMock,
                onCheckedChange = { excludeMock = it },
                text = "Exclude Mock Locations",
                description = "Filter out simulated/fake GPS points",
            )

            FilterCheckbox(
                checked = excludeAbnormal,
                onCheckedChange = { excludeAbnormal = it },
                text = "Exclude Abnormal Points",
                description = "Remove points flagged as abnormal",
            )

            FilterCheckbox(
                checked = excludePaused,
                onCheckedChange = { excludePaused = it },
                text = "Exclude Paused Points",
                description = "Remove points recorded when tracking was paused",
            )

            FilterCheckbox(
                checked = onlyCheckpoints,
                onCheckedChange = { onlyCheckpoints = it },
                text = "Only Checkpoints",
                description = "Export only manually marked checkpoint locations",
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Advanced filters toggle
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Advanced Filters",
                    style = MaterialTheme.typography.titleSmall,
                )
                Switch(
                    checked = showAdvancedFilters,
                    onCheckedChange = { showAdvancedFilters = it },
                )
            }

            if (showAdvancedFilters) {
                Spacer(modifier = Modifier.height(8.dp))

                // Accuracy filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = if (minAccuracy > 0) minAccuracy.toString() else "",
                        onValueChange = { value ->
                            minAccuracy = value.toFloatOrNull() ?: 0f
                        },
                        label = { Text("Min Accuracy (m)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = if (maxAccuracy > 0) maxAccuracy.toString() else "",
                        onValueChange = { value ->
                            maxAccuracy = value.toFloatOrNull() ?: 0f
                        },
                        label = { Text("Max Accuracy (m)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Battery filter
                OutlinedTextField(
                    value = minBatteryText,
                    onValueChange = { minBatteryText = it },
                    label = { Text("Min Battery Level (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. 20") },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val filter =
                        LocationDataFilter(
                            minAccuracy = if (minAccuracy > 0) minAccuracy else null,
                            maxAccuracy = if (maxAccuracy > 0) maxAccuracy else null,
                            excludePaused = excludePaused,
                            excludeMock = excludeMock,
                            excludeAbnormal = excludeAbnormal,
                            onlyCheckpoints = onlyCheckpoints,
                            minBatteryLevel = minBatteryText.toDoubleOrNull(),
                        )
                    onExport(selectedFormat, filter)
                },
                modifier = Modifier.weight(1f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("Export")
                }
            }
        }
    }
}

@Composable
private fun FilterCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    description: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
