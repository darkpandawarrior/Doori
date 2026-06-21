package com.miletracker.feature.tracking.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.theme.MilewayColors

@Composable
fun HeatmapLegend() {
    LegendHeatmapRow()
}

@Composable
fun StatusLegend() {
    LegendStatusColumn(lineItem = { color, label, dashed ->
        LegendLineItem(color = color, label = label, dashed = dashed)
    })
}

@Composable
fun HeatmapLegendCompact() {
    LegendHeatmapRow()
}

@Composable
fun StatusLegendCompact() {
    LegendStatusColumn(lineItem = { color, label, dashed ->
        LegendLineItemCompact(color = color, label = label, dashed = dashed)
    })
}

@Composable
private fun LegendHeatmapRow() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(Color(0xFF2196F3), CircleShape),
            )
            Text("Slow", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(14.dp)
                    .background(Color(0xFF4CAF50), CircleShape),
            )
            Text("City", style = MaterialTheme.typography.bodySmall)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(Color(0xFFFF9800), CircleShape),
            )
            Text("Fast", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(14.dp)
                    .background(Color(0xFFF44336), CircleShape),
            )
            Text("Highway", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LegendStatusColumn(lineItem: @Composable (color: Color, label: String, dashed: Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lineItem(MilewayColors.info, "Normal", false)
        lineItem(MilewayColors.neutral, "Paused", true)
        lineItem(MilewayColors.warning, "Inactivity", true)
        lineItem(MilewayColors.warning, "Abnormal", false)
        lineItem(MilewayColors.danger, "Mock", false)
    }
}

@Composable
private fun LegendLineItem(
    color: Color,
    label: String,
    dashed: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.size(width = 36.dp, height = 10.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect =
                    if (dashed) {
                        PathEffect.dashPathEffect(
                            floatArrayOf(12f, 12f),
                            0f,
                        )
                    } else {
                        null
                    },
            )
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LegendLineItemCompact(
    color: Color,
    label: String,
    dashed: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.size(width = 36.dp, height = 10.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect =
                    if (dashed) {
                        PathEffect.dashPathEffect(
                            floatArrayOf(12f, 12f),
                            0f,
                        )
                    } else {
                        null
                    },
            )
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
