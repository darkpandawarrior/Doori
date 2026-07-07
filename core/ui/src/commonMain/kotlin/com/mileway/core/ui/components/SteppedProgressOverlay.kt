@file:Suppress("ktlint:standard:function-naming")

package com.mileway.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

/**
 * One step in a [SteppedProgressOverlay] sequence (e.g. "Validating", "Uploading", "Processing").
 */
data class ProgressStep(val label: String)

/** Derived visual status of a step relative to the overlay's current step index. */
enum class StepStatus { PENDING, ACTIVE, DONE }

private fun statusFor(
    index: Int,
    currentStepIndex: Int,
): StepStatus =
    when {
        index < currentStepIndex -> StepStatus.DONE
        index == currentStepIndex -> StepStatus.ACTIVE
        else -> StepStatus.PENDING
    }

/**
 * Full-screen, non-dismissible loading overlay showing an ordered list of [steps] with the
 * current step highlighted and prior steps checked off. Used for multi-stage operations
 * (e.g. validating -> uploading -> processing) where a bare spinner underrepresents progress.
 *
 * Non-dismissible by design: no onDismiss callback, and taps on the scrim are consumed so they
 * never reach content underneath.
 */
@Composable
fun SteppedProgressOverlay(
    steps: List<ProgressStep>,
    currentStepIndex: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                // ponytail: consume touches so the scrim is inert; no dismiss callback exists at all.
                .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.padding(DesignTokens.Spacing.xl),
            shape = DesignTokens.Shape.roundedMd,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.prominent),
        ) {
            Column(
                modifier = Modifier.padding(DesignTokens.Spacing.xl),
            ) {
                steps.forEachIndexed { index, step ->
                    StepRow(step = step, status = statusFor(index, currentStepIndex))
                    if (index != steps.lastIndex) {
                        Spacer(Modifier.size(DesignTokens.Spacing.m))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    step: ProgressStep,
    status: StepStatus,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Box(
            modifier = Modifier.size(DesignTokens.Spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                StepStatus.DONE ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(DesignTokens.Shape.button)
                                .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(DesignTokens.IconSize.inline),
                        )
                    }

                StepStatus.ACTIVE ->
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )

                StepStatus.PENDING ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(DesignTokens.Shape.button)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
            }
        }

        Text(
            text = step.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (status == StepStatus.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
            color =
                if (status == StepStatus.PENDING) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}
