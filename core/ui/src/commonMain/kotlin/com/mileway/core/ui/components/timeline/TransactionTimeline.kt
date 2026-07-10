package com.mileway.core.ui.components.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens

/**
 * V28 P28.1 (T-SCAFFOLD): one step in a transaction's status timeline (submitted / under review /
 * approved / rejected, etc). Extracted from the three near-identical hand-rolled timeline cards in
 * approvals-detail, expense-detail and purchase-request-details — each feature still builds its own
 * [TimelineStep] list from its own status enum, only the rendering is shared.
 */
data class TimelineStep(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val active: Boolean,
    val note: String = "",
)

/**
 * V28 P28.1: shared card-shaped timeline renderer for [DetailSection.Timeline][com.mileway.core.ui.components.scaffold.DetailSection.Timeline]
 * tab content. [title] is left to the caller (each feature has its own localized string) and
 * omitted entirely when null.
 */
@Composable
fun TransactionTimeline(
    steps: List<TimelineStep>,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            steps.forEach { step -> TimelineStepRow(step) }
        }
    }
}

@Composable
private fun TimelineStepRow(step: TimelineStep) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        if (step.active) step.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        DesignTokens.Shape.button,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = if (step.active) step.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                text = step.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (step.active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (step.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            if (step.note.isNotBlank()) {
                Text(
                    text = step.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
