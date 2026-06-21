package com.miletracker.feature.tracking.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.theme.MilewayColors

enum class TrendDirection { UP, DOWN, STABLE }

@Composable
fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    trend: TrendDirection? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                trend?.let {
                    Spacer(Modifier.width(4.dp))
                    TrendIndicator(it)
                }
            }
        }
    }
}

@Composable
fun TrendIndicator(
    trend: TrendDirection,
    modifier: Modifier = Modifier,
) {
    val (icon, color) =
        when (trend) {
            TrendDirection.UP -> Icons.Default.ArrowUpward to MilewayColors.success
            TrendDirection.DOWN -> Icons.Default.ArrowDownward to MilewayColors.danger
            TrendDirection.STABLE -> Icons.Default.Remove to MilewayColors.neutral
        }
    Icon(icon, contentDescription = trend.name, tint = color, modifier = modifier.size(12.dp))
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animatedColor by animateColorAsState(color, animationSpec = spring(), label = "badge")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = animatedColor.copy(alpha = 0.15f),
    ) {
        Text(
            text = text,
            color = animatedColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun DataQualityItem(
    label: String,
    value: String,
    trend: TrendDirection = TrendDirection.STABLE,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            TrendIndicator(trend)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun QualityDot(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val color =
        when {
            score >= 75 -> MilewayColors.success
            score >= 35 -> MilewayColors.warning
            else -> MilewayColors.danger
        }
    Box(
        modifier =
            modifier
                .size(10.dp)
                .background(color, CircleShape),
    )
}

/**
 * Non-composable quality→colour map for text tints in score cards. Uses the static v2
 * fallbacks (kept in lock-step with [MilewayColors]); the composable [QualityDot] mirrors
 * the same three-tier success/warning/danger split.
 */
fun qualityColor(score: Int): Color =
    when {
        score >= 75 -> com.miletracker.core.ui.theme.DesignTokens.StatusColors.success
        score >= 35 -> com.miletracker.core.ui.theme.DesignTokens.StatusColors.warning
        else -> com.miletracker.core.ui.theme.DesignTokens.StatusColors.error
    }
