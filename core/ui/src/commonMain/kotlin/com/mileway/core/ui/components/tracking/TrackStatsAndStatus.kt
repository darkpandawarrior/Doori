package com.mileway.core.ui.components.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_cd_collapse
import com.mileway.core.ui.resources.core_cd_expand
import com.mileway.core.ui.resources.core_current_journey
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

// =============================================================================
// Expandable journey-stats card
// =============================================================================

/** One labelled stat tile (e.g. "Distance" / "2.38 km"). */
data class StatItem(
    val label: String,
    val value: String,
    val icon: ImageVector? = null,
)

/**
 * Collapsible stats card. Collapsed it shows a compact "Current Journey" header with the
 * first few values inline; expanded it reveals every stat in a 2-column grid. Mirrors the
 * "Current Journey" panel on the live-tracking screen (Distance / Duration / Avg Speed / Paused).
 */
@Composable
fun ExpandableStatsCard(
    stats: List<StatItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.core_current_journey),
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevron",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedLg,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(DesignTokens.Spacing.l),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) stringResource(Res.string.core_cd_collapse) else stringResource(Res.string.core_cd_expand),
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Collapsed preview: the first row of values inline.
            if (!expanded) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
                ) {
                    stats.take(2).forEach { StatColumn(it, Modifier.weight(1f)) }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = DesignTokens.Spacing.m)) {
                    stats.chunked(2).forEach { rowItems ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = DesignTokens.Spacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
                        ) {
                            rowItems.forEach { StatColumn(it, Modifier.weight(1f)) }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    item: StatItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// =============================================================================
// System-status chips + banner
// =============================================================================

/** Health level for a status chip → green / amber / red. */
enum class StatusLevel { OK, WARN, BAD }

/** One status chip (e.g. GPS / battery / network / accuracy). */
data class StatusChip(
    val icon: ImageVector,
    val label: String,
    val level: StatusLevel,
)

private fun StatusLevel.color(): Color =
    when (this) {
        StatusLevel.OK -> DesignTokens.StatusColors.success
        StatusLevel.WARN -> DesignTokens.StatusColors.warning
        StatusLevel.BAD -> DesignTokens.StatusColors.error
    }

/**
 * A horizontal row of compact, color-coded status chips (GPS, battery, network, accuracy).
 */
@Composable
fun CompactSystemStatusIndicator(
    chips: List<StatusChip>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            val tint = chip.level.color()
            Row(
                modifier =
                    Modifier
                        .clip(DesignTokens.Shape.chip)
                        .background(tint.copy(alpha = 0.12f))
                        .padding(horizontal = DesignTokens.Spacing.s, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = chip.icon,
                    contentDescription = chip.label,
                    modifier = Modifier.size(14.dp),
                    tint = tint,
                )
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = tint,
                )
            }
        }
    }
}

/**
 * The "All systems OK" pill seen below the hero card: a tinted rounded bar with a status
 * dot/check and a message. Green when [allOk], amber otherwise.
 */
@Composable
fun SystemStatusBanner(
    allOk: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (allOk) DesignTokens.StatusColors.success else DesignTokens.StatusColors.warning,
        animationSpec = tween(300),
        label = "statusTint",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            if (allOk) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint,
                )
            } else {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(tint),
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
