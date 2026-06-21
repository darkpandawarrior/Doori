@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.feature.logging.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.theme.DesignTokens

/**
 * A lightweight decorative "map preview" rendered entirely with Canvas — a tinted
 * grid plus a hinted route polyline and two endpoint markers. There is no real map
 * provider (osmdroid is intentionally optional in this offline demo), so this card
 * stands in for the map shown at the top of Step 1 in the reference.
 *
 * @param stopCount number of itinerary stops, used to vary the route hint
 */
@Composable
fun MapPreviewCard(
    stopCount: Int,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val routeColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.secondary
    val surfaceTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(160.dp),
        shape = DesignTokens.Shape.roundedMd,
        color = surfaceTint,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.s)) {
                val step = 28.dp.toPx()
                // Grid lines.
                var x = 0f
                while (x <= size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                // Route hint: a dashed polyline with a vertex per (virtual) stop.
                val points = routePoints(stopCount.coerceAtLeast(2), size.width, size.height)
                val dash = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = routeColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 6f,
                        pathEffect = dash,
                    )
                }

                // Endpoint markers.
                drawCircle(markerColor, radius = 9f, center = points.first())
                drawCircle(routeColor, radius = 9f, center = points.last())
                drawCircle(
                    color = routeColor,
                    radius = 16f,
                    center = points.last(),
                    style = Stroke(width = 3f),
                )
            }

            // Corner badge so the card reads as a map even when empty.
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(DesignTokens.Spacing.s),
                shape = DesignTokens.Shape.chip,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(14.dp),
                    )
                    Text(
                        "  Route preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Deterministic zig-zag of vertices spanning the canvas, one per stop. */
private fun routePoints(
    count: Int,
    width: Float,
    height: Float,
): List<Offset> {
    val n = count.coerceIn(2, 6)
    val marginX = width * 0.12f
    val usableW = width - marginX * 2
    return (0 until n).map { i ->
        val t = if (n == 1) 0f else i.toFloat() / (n - 1)
        val px = marginX + usableW * t
        // Alternate high/low to mimic a winding route.
        val py = if (i % 2 == 0) height * 0.7f else height * 0.32f
        Offset(px, py)
    }
}
