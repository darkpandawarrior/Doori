package com.mileway.feature.tracking.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_no_route
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

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
        shape = DesignTokens.Shape.roundedSm,
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
        shape = DesignTokens.Shape.button,
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
                .background(color, DesignTokens.Shape.button),
    )
}

/**
 * Non-composable quality→colour map for text tints in score cards. Uses the static v2
 * fallbacks (kept in lock-step with [MilewayColors]); the composable [QualityDot] mirrors
 * the same three-tier success/warning/danger split.
 */
fun qualityColor(score: Int): Color =
    when {
        score >= 75 -> com.mileway.core.ui.theme.DesignTokens.StatusColors.success
        score >= 35 -> com.mileway.core.ui.theme.DesignTokens.StatusColors.warning
        else -> com.mileway.core.ui.theme.DesignTokens.StatusColors.error
    }

/**
 * P-E.1: Canvas-based static route thumbnail — renders GPS polyline without a live map SDK.
 * Normalises the lat/lng bounding box to the canvas area and draws the route as a stroked path.
 */
@Composable
fun StaticPolylineThumbnail(
    latLngs: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    thumbHeight: Dp = 80.dp,
    lineColor: Color = Color.Unspecified,
) {
    val lineColorResolved = if (lineColor == Color.Unspecified) MaterialTheme.colorScheme.primary else lineColor
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(thumbHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant, DesignTokens.Shape.button),
        contentAlignment = Alignment.Center,
    ) {
        if (latLngs.size >= 2) {
            Canvas(modifier = Modifier.matchParentSize().padding(8.dp)) {
                val minLat = latLngs.minOf { it.first }
                val maxLat = latLngs.maxOf { it.first }
                val minLng = latLngs.minOf { it.second }
                val maxLng = latLngs.maxOf { it.second }
                val latRange = (maxLat - minLat).takeIf { it > 0.0 } ?: 0.001
                val lngRange = (maxLng - minLng).takeIf { it > 0.0 } ?: 0.001
                val points =
                    latLngs.map { (lat, lng) ->
                        Offset(
                            x = ((lng - minLng) / lngRange * size.width).toFloat(),
                            y = ((1.0 - (lat - minLat) / latRange) * size.height).toFloat(),
                        )
                    }
                val path =
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                drawPath(
                    path = path,
                    color = lineColorResolved,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawCircle(lineColorResolved, radius = 5.dp.toPx(), center = points.first())
                drawCircle(lineColorResolved, radius = 5.dp.toPx(), center = points.last())
            }
        } else {
            Text(
                text = stringResource(Res.string.tracking_no_route),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** P-E.1: Horizontally-scrollable chip row for sectioned submission forms. */
@Composable
fun SubmissionTabChips(
    tabs: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { tab ->
            FilterChip(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(tab, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}
