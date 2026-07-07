package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors

/**
 * Enhanced metric card with trend indicators and status colors
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    color: Color,
    trend: MetricTrend? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = DesignTokens.Shape.roundedSm,
        colors =
            CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f),
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon with colored background
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(DesignTokens.Shape.roundedSm)
                            .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }

                // Trend indicator
                if (trend != null) {
                    TrendIndicator(trend = trend)
                }
            }
        }
    }
}

/**
 * Trend indicator for metrics
 */
@Composable
fun TrendIndicator(
    trend: MetricTrend,
    modifier: Modifier = Modifier,
) {
    val (icon, color) =
        when (trend) {
            MetricTrend.Good -> Icons.Default.CheckCircle to MilewayColors.success
            MetricTrend.Warning -> Icons.Default.Warning to MilewayColors.warning
            MetricTrend.Poor -> Icons.Default.Warning to MilewayColors.danger
        }

    Icon(
        imageVector = icon,
        contentDescription = trend.name,
        tint = color,
        modifier = modifier.size(20.dp),
    )
}

/**
 * Enhanced system status card with better visual hierarchy
 */
@Composable
fun SystemStatusCard(
    title: String,
    issues: List<SystemIssue>,
    modifier: Modifier = Modifier,
) {
    if (issues.isEmpty()) return

    Card(
        modifier = modifier,
        shape = DesignTokens.Shape.roundedSm,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            issues.forEach { issue ->
                SystemIssueItem(issue = issue)
                Spacer(modifier = Modifier.size(6.dp))
            }
        }
    }
}

/**
 * Individual system issue item with severity indicator
 */
@Composable
fun SystemIssueItem(
    issue: SystemIssue,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = issue.severity.color.copy(alpha = 0.3f),
                    shape = DesignTokens.Shape.button,
                )
                .background(
                    issue.severity.color.copy(alpha = 0.1f),
                    DesignTokens.Shape.button,
                )
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Severity indicator
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(DesignTokens.Shape.button)
                    .background(issue.severity.color),
        )

        // Issue description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (issue.impact.isNotEmpty()) {
                Text(
                    text = issue.impact,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Severity badge
        Text(
            text = issue.severity.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = issue.severity.color,
            modifier =
                Modifier
                    .background(
                        issue.severity.color.copy(alpha = 0.2f),
                        DesignTokens.Shape.button,
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
