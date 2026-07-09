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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_settings_auto_pause
import com.mileway.core.ui.resources.tracking_settings_auto_pause_desc
import com.mileway.core.ui.resources.tracking_settings_background_network
import com.mileway.core.ui.resources.tracking_settings_force_gps
import com.mileway.core.ui.resources.tracking_settings_force_gps_desc
import com.mileway.core.ui.resources.tracking_settings_gps_accuracy
import com.mileway.core.ui.resources.tracking_settings_location_interval
import com.mileway.core.ui.resources.tracking_settings_min_accuracy
import com.mileway.core.ui.resources.tracking_settings_min_displacement
import com.mileway.core.ui.resources.tracking_settings_provider
import com.mileway.core.ui.resources.tracking_settings_smart_pause
import com.mileway.core.ui.resources.tracking_settings_subtitle
import com.mileway.core.ui.resources.tracking_settings_title
import com.mileway.core.ui.resources.tracking_settings_update_interval
import com.mileway.core.ui.resources.tracking_settings_upload_background
import com.mileway.core.ui.resources.tracking_settings_upload_background_desc
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * PLAN_V24 P10.1: state is hoisted so the live call site (TrackingNavigation) can back each knob
 * with the persisted PluginRegistry value, while the defaults below reproduce the previous
 * `remember{}` defaults (30/10/10/true/false/false) verbatim — so the gallery/preview/showcase
 * render byte-identically without going through the registry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSettingsScreen(
    onBack: () -> Unit,
    gpsAccuracy: Float = 30f,
    onGpsAccuracyChange: (Float) -> Unit = {},
    locationInterval: Float = 10f,
    onLocationIntervalChange: (Float) -> Unit = {},
    distanceThreshold: Float = 10f,
    onDistanceThresholdChange: (Float) -> Unit = {},
    uploadInBackground: Boolean = true,
    onUploadInBackgroundChange: (Boolean) -> Unit = {},
    autoPauseDetection: Boolean = false,
    onAutoPauseDetectionChange: (Boolean) -> Unit = {},
    forceGpsOnly: Boolean = false,
    onForceGpsOnlyChange: (Boolean) -> Unit = {},
) {
    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.tracking_settings_title),
                subtitle = stringResource(Res.string.tracking_settings_subtitle),
                titleIcon = Icons.Default.Settings,
                depth = DesignTokens.NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tracking_cd_back),
                        )
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
                title = stringResource(Res.string.tracking_settings_gps_accuracy),
                leadingIcon = Icons.Filled.GpsFixed,
                initiallyExpanded = true,
            ) {
                SettingSliderRow(
                    label = stringResource(Res.string.tracking_settings_min_accuracy),
                    value = gpsAccuracy,
                    onValueChange = onGpsAccuracyChange,
                    valueRange = 10f..100f,
                    unit = "m",
                )
            }

            CollapsibleSectionCard(
                title = stringResource(Res.string.tracking_settings_location_interval),
                leadingIcon = Icons.Filled.Timeline,
                initiallyExpanded = true,
            ) {
                SettingSliderRow(
                    label = stringResource(Res.string.tracking_settings_update_interval),
                    value = locationInterval,
                    onValueChange = onLocationIntervalChange,
                    valueRange = 5f..60f,
                    unit = "s",
                )
                Spacer(Modifier.height(DesignTokens.Spacing.m))
                SettingSliderRow(
                    label = stringResource(Res.string.tracking_settings_min_displacement),
                    value = distanceThreshold,
                    onValueChange = onDistanceThresholdChange,
                    valueRange = 5f..50f,
                    unit = "m",
                )
            }

            CollapsibleSectionCard(
                title = stringResource(Res.string.tracking_settings_background_network),
                leadingIcon = Icons.Filled.Wifi,
                initiallyExpanded = true,
            ) {
                SettingToggleRow(
                    label = stringResource(Res.string.tracking_settings_upload_background),
                    description = stringResource(Res.string.tracking_settings_upload_background_desc),
                    icon = Icons.Filled.BatteryFull,
                    checked = uploadInBackground,
                    onCheckedChange = onUploadInBackgroundChange,
                )
            }

            CollapsibleSectionCard(
                title = stringResource(Res.string.tracking_settings_smart_pause),
                leadingIcon = Icons.Filled.Pause,
                initiallyExpanded = true,
            ) {
                SettingToggleRow(
                    label = stringResource(Res.string.tracking_settings_auto_pause),
                    description = stringResource(Res.string.tracking_settings_auto_pause_desc),
                    icon = Icons.Filled.Pause,
                    checked = autoPauseDetection,
                    onCheckedChange = onAutoPauseDetectionChange,
                )
            }

            CollapsibleSectionCard(
                title = stringResource(Res.string.tracking_settings_provider),
                leadingIcon = Icons.Filled.Settings,
                initiallyExpanded = true,
            ) {
                SettingToggleRow(
                    label = stringResource(Res.string.tracking_settings_force_gps),
                    description = stringResource(Res.string.tracking_settings_force_gps_desc),
                    icon = Icons.Filled.GpsFixed,
                    checked = forceGpsOnly,
                    onCheckedChange = onForceGpsOnlyChange,
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
