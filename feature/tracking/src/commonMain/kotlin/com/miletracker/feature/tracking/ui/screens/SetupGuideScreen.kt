package com.miletracker.feature.tracking.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens

private data class SetupStep(
    val number: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val SETUP_STEPS =
    listOf(
        SetupStep(1, "Grant Location Permission", "Allow location access 'Always' for background tracking", Icons.Filled.GpsFixed),
        SetupStep(2, "Disable Battery Optimization", "Prevent Android from stopping the tracking service", Icons.Filled.BatteryFull),
        SetupStep(3, "Enable Background Data", "Allow the app to sync trip data in the background", Icons.Filled.Wifi),
        SetupStep(4, "Configure Vehicle & Odometer", "Set up your vehicle details and odometer readings", Icons.Filled.DirectionsCar),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    onBack: () -> Unit,
    onOpenTrackSettings: () -> Unit = {},
) {
    // Demo: each step toggles independently on Configure/Done tap.
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }

    val allDone = completedSteps.size == SETUP_STEPS.size

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Setup Guide",
                subtitle = "Configure the app for accurate tracking",
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
            // Completion banner — only visible when all steps done.
            if (allDone) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(DesignTokens.Spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "All set! You're ready to track.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Stepper list
            SETUP_STEPS.forEachIndexed { index, step ->
                val isDone = step.number in completedSteps
                val isLast = index == SETUP_STEPS.lastIndex

                SetupStepRow(
                    step = step,
                    isDone = isDone,
                    isLast = isLast,
                    onAction = {
                        if (isDone) {
                            completedSteps = completedSteps - step.number
                        } else {
                            completedSteps = completedSteps + step.number
                            if (step.number == 4) onOpenTrackSettings()
                        }
                    },
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xl))
        }
    }
}

@Composable
private fun SetupStepRow(
    step: SetupStep,
    isDone: Boolean,
    isLast: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        verticalAlignment = Alignment.Top,
    ) {
        // Step indicator column: number circle + connector line
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .background(
                            color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = step.number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!isLast) {
                Spacer(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(DesignTokens.Spacing.xl)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }

        // Step content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = step.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isDone) {
                OutlinedButton(
                    onClick = onAction,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Done")
                }
            } else {
                Button(onClick = onAction) {
                    Text("Configure")
                }
            }
            if (!isLast) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))
            }
        }
    }
}
