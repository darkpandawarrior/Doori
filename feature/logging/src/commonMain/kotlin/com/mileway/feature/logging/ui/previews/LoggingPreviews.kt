package com.mileway.feature.logging.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.EmptyState
import com.mileway.core.ui.components.StatCard
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewMatrix
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.previews.SampleData
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.ui.components.StepHeaderCard
import com.mileway.feature.logging.ui.components.TapFieldRow
// ---------------------------------------------------------------------------
// LoggingPreviews.kt: Phase 9.1 preview functions for feature:logging
//
// Rules:
// - No DI, no ViewModel, no Koin
// - Uses @PreviewLightDark / @PreviewMatrix from :core:ui
// - Previews sub-composables or self-contained content only
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 1. TapFieldRow, the labelled "tap to open" field used on Log Miles Step 1
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun TapFieldRowEmptyPreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TapFieldRow(
                label = "Journey Date",
                value = "Select date",
                onClick = {},
                isPlaceholder = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TapFieldRowFilledPreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TapFieldRow(
                label = "Journey Date",
                value = "19 Jun 2026",
                onClick = {},
                leadingIcon = Icons.Filled.DirectionsCar,
                isPlaceholder = false,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. StepHeaderCard, "Step 1 of 2" progress card used at top of LogMilesScreen
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun StepHeaderCardPreview() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            StepHeaderCard(
                title = "Step 1 of 2",
                subtitle = "Enter your journey details: start location, stops, and vehicle type.",
            )
        }
    }
}

@PreviewMatrix
@Composable
private fun StepHeaderCardMatrixPreview() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            StepHeaderCard(
                title = "Step 2 of 2",
                subtitle = "Review distance, reimbursement rate, and submit your mileage claim.",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Log Miles success content, inline (screen takes ViewModel, so we mirror
//    its content layout with hardcoded sample values)
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun LogMilesSuccessContentPreview() {
    PreviewSurface {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .padding(DesignTokens.Spacing.l)
                            .size(72.dp),
                )
            }
            Spacer(Modifier.size(DesignTokens.Spacing.l))
            Text(
                "Miles Logged!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Text(
                "Your mileage expense has been submitted successfully.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(DesignTokens.Spacing.xl))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Reimbursable amount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "₹${SampleData.Trip.reimbursableAmount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.size(DesignTokens.Spacing.m))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Transaction id",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            SampleData.Trip.routeId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Expense history, empty-state variant (screen takes ViewModel; preview
//    the shared EmptyState from :core:ui instead)
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun ExpenseHistoryEmptyStatePreview() {
    PreviewSurface {
        EmptyState(
            title = "No expenses found",
            subtitle = "Add your first expense using the + button.",
        )
    }
}

// ---------------------------------------------------------------------------
// 5. Stat summary strip, reusable StatCard components shown in history headers
// ---------------------------------------------------------------------------

@PreviewLightDark
@Composable
private fun LoggingStatCardsPreview() {
    PreviewSurface {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Total Distance",
                value = "${SampleData.Trip.distanceKm} km",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Reimbursable",
                value = "₹${SampleData.Trip.reimbursableAmount}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
