package com.mileway.feature.tracking.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSettingsScreen(onBack: () -> Unit) {
    var gpsAccuracy by remember { mutableFloatStateOf(30f) }
    var locationInterval by remember { mutableFloatStateOf(10f) }
    var distanceThreshold by remember { mutableFloatStateOf(10f) }
    var uploadInBackground by remember { mutableStateOf(true) }
    var autoPauseDetection by remember { mutableStateOf(false) }
    var forceGpsOnly by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Settings",
                subtitle = "Tracking configuration",
                depth = DesignTokens.NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            CollapsibleSectionCard(
                title = "GPS Accuracy",
                leadingIcon = Icons.Filled.GpsFixed,
                initiallyExpanded = true,
            ) {
                SettingSliderRow(
                    label = "Min accuracy threshold",
                    value = gpsAccuracy,
                    onValueChange = { gpsAccuracy = it },
                    valueRange = 10f..100f,
                    unit = "m",
                )
            }

            CollapsibleSectionCard(
                title = "Location Interval",
                leadingIcon = Icons.Filled.Timeline,
                initiallyExpanded = true,
            ) {
                SettingSliderRow(
                    label = "Update interval",
                    value = locationInterval,
                    onValueChange = { locationInterval = it },
                    valueRange = 5f..60f,
                    unit = "s",
                )
                Spacer(Modifier.height(DesignTokens.Spacing.m))
                SettingSliderRow(
                    label = "Min displacement before recording",
                    value = distanceThreshold,
                    onValueChange = { distanceThreshold = it },
                    valueRange = 5f..50f,
                    unit = "m",
                )
            }

            CollapsibleSectionCard(
                title = "Background & Network",
                leadingIcon = Icons.Filled.Wifi,
                initiallyExpanded = true,
            ) {
                SettingToggleRow(
                    label = "Upload in background",
                    description = "Sync tracking data when app is in background",
                    icon = Icons.Filled.BatteryFull,
                    checked = uploadInBackground,
                    onCheckedChange = { uploadInBackground = it },
                )
            }

            CollapsibleSectionCard(
                title = "Smart Pause",
                leadingIcon = Icons.Filled.Pause,
                initiallyExpanded = true,
            ) {
                SettingToggleRow(
                    label = "Auto-pause detection",
                    description = "Pauses when speed < 2 km/h for 3+ minutes",
                    icon = Icons.Filled.Pause,
                    checked = autoPauseDetection,
                    onCheckedChange = { autoPauseDetection = it },
                )
            }

            CollapsibleSectionCard(
                title = "Provider",
                leadingIcon = Icons.Filled.Settings,
                initiallyExpanded = true,
            ) {
                SettingToggleRow(
                    label = "Force GPS only",
                    description = "Disable network/fused provider (GPS only)",
                    icon = Icons.Filled.GpsFixed,
                    checked = forceGpsOnly,
                    onCheckedChange = { forceGpsOnly = it },
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${value.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = androidx.compose.ui.Modifier.padding(end = 0.dp),
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
