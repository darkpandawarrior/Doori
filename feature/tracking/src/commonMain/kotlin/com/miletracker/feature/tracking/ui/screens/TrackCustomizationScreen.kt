package com.miletracker.feature.tracking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import com.miletracker.core.ui.components.CollapsibleSectionCard
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackCustomizationScreen(
    onBack: () -> Unit,
    // G6: the Kalman toggle is now persisted — its state is hoisted and bound to DemoSettings at
    // the nav site. Defaults keep previews/screenshot callers and tests source-compatible.
    kalmanEnabled: Boolean = false,
    onKalmanChange: (Boolean) -> Unit = {},
) {
    var gapDetectionEnabled by remember { mutableStateOf(true) }
    var motionStabilityEnabled by remember { mutableStateOf(false) }
    var deadReckoningEnabled by remember { mutableStateOf(false) }
    var spikeThreshold by remember { mutableFloatStateOf(500f) }
    var abnormalSpeed by remember { mutableFloatStateOf(120f) }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Tracking Customization",
                subtitle = "Experimental algorithms & thresholds",
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
                title = "Experimental Optimizations",
                leadingIcon = Icons.Filled.Tune,
                initiallyExpanded = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    CustomizationToggleRow(
                        icon = Icons.Filled.Filter,
                        label = "Kalman Smoothing",
                        subtitle = "Reduces GPS noise by predicting position trajectory",
                        infoChip = "Reduces GPS noise",
                        checked = kalmanEnabled,
                        onCheckedChange = onKalmanChange,
                    )
                    CustomizationToggleRow(
                        icon = Icons.Filled.BugReport,
                        label = "Gap Detection",
                        subtitle = "Identifies and fills gaps in location signal",
                        infoChip = "Catches location gaps",
                        checked = gapDetectionEnabled,
                        onCheckedChange = { gapDetectionEnabled = it },
                    )
                    CustomizationToggleRow(
                        icon = Icons.Filled.Bolt,
                        label = "Motion Stability",
                        subtitle = "Uses accelerometer to validate GPS readings",
                        infoChip = "Uses accelerometer for activity",
                        checked = motionStabilityEnabled,
                        onCheckedChange = { motionStabilityEnabled = it },
                    )
                    CustomizationToggleRow(
                        icon = Icons.Filled.Navigation,
                        label = "Predictive Dead Reckoning",
                        subtitle = "Continues tracking during temporary GPS outages",
                        infoChip = "Continues tracking during gaps",
                        checked = deadReckoningEnabled,
                        onCheckedChange = { deadReckoningEnabled = it },
                    )
                }
            }

            CollapsibleSectionCard(
                title = "Algorithm Thresholds",
                leadingIcon = Icons.Filled.Timeline,
                initiallyExpanded = true,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    AlgorithmSliderRow(
                        label = "Spike threshold",
                        value = spikeThreshold,
                        onValueChange = { spikeThreshold = it },
                        valueRange = 100f..2000f,
                        unit = "m",
                    )
                    AlgorithmSliderRow(
                        label = "Abnormal speed threshold",
                        value = abnormalSpeed,
                        onValueChange = { abnormalSpeed = it },
                        valueRange = 50f..300f,
                        unit = "km/h",
                    )
                }
            }

            // Amber disclaimer card
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(DesignTokens.Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = "Experimental features may affect battery and accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
        }
    }
}

@Composable
private fun CustomizationToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    infoChip: String,
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
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = infoChip,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.padding(0.dp),
                        )
                    },
                    colors =
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AlgorithmSliderRow(
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "${value.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
