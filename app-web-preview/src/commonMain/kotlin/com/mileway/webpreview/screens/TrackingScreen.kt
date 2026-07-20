package com.mileway.webpreview.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.TrackPolyline
import com.mileway.core.ui.theme.TrackStart
import com.mileway.core.ui.theme.terminalStyle
import com.mileway.webpreview.DemoTrackingEngine
import com.mileway.webpreview.formatKm

@Composable
fun TrackingScreen(
    engine: DemoTrackingEngine,
    modifier: Modifier = Modifier,
) {
    val state by engine.state.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DesignTokens.Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TRACK MILES",
                style = MaterialTheme.typography.labelMedium.terminalStyle(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = DesignTokens.Shape.chip,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = "GPS: SIMULATED",
                    style = MaterialTheme.typography.labelSmall.terminalStyle(),
                    color = DesignTokens.StatusColors.info,
                    modifier =
                        Modifier.padding(
                            horizontal = DesignTokens.Spacing.m,
                            vertical = DesignTokens.Spacing.xs,
                        ),
                )
            }
        }

        // Live route, drawn from the Kalman-smoothed fixes — the demo's stand-in for the map tile.
        Card(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = DesignTokens.Shape.roundedMd,
            elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
        ) {
            Box(Modifier.fillMaxSize().padding(DesignTokens.Spacing.m)) {
                if (state.path.size < 2) {
                    Text(
                        text = "Start tracking to draw the route",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    val current = MaterialTheme.colorScheme.primary
                    Canvas(Modifier.fillMaxSize()) {
                        val lats = state.path.map { it.first }
                        val lngs = state.path.map { it.second }
                        val latSpan = (lats.max() - lats.min()).coerceAtLeast(1e-5)
                        val lngSpan = (lngs.max() - lngs.min()).coerceAtLeast(1e-5)

                        fun toOffset(p: Pair<Double, Double>): Offset =
                            Offset(
                                x = (((p.second - lngs.min()) / lngSpan) * size.width * 0.9 + size.width * 0.05).toFloat(),
                                // Screen y grows downward; latitude grows upward.
                                y = (((lats.max() - p.first) / latSpan) * size.height * 0.9 + size.height * 0.05).toFloat(),
                            )
                        val path = Path()
                        state.path.forEachIndexed { i, p ->
                            val o = toOffset(p)
                            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                        }
                        drawPath(path, TrackPolyline, style = Stroke(width = 3.dp.toPx()))
                        drawCircle(TrackStart, radius = 5.dp.toPx(), center = toOffset(state.path.first()))
                        drawCircle(current, radius = 6.dp.toPx(), center = toOffset(state.path.last()))
                    }
                }
            }
        }

        Text(
            text = formatKm(state.distanceKm),
            style = MaterialTheme.typography.displayMedium.terminalStyle(),
            color = MaterialTheme.colorScheme.primary,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            Metric("SPEED", "${state.speedKmh.toInt()} km/h", Modifier.weight(1f))
            Metric("ACCURACY", "±${state.accuracyM.toInt()} m", Modifier.weight(1f))
            Metric("ELAPSED", formatElapsed(state.elapsedSec), Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            if (!state.isTracking || state.isPaused) {
                Button(
                    onClick = { engine.start() },
                    shape = DesignTokens.Shape.button,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.isPaused) "Resume" else "Start trip") }
            } else {
                OutlinedButton(
                    onClick = { engine.pause() },
                    shape = DesignTokens.Shape.button,
                    modifier = Modifier.weight(1f),
                ) { Text("Pause") }
            }
            if (state.isTracking) {
                OutlinedButton(
                    onClick = { engine.stop() },
                    shape = DesignTokens.Shape.button,
                    modifier = Modifier.weight(1f),
                ) { Text("End trip") }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = DesignTokens.Shape.roundedSm,
        ) {
            Row(
                modifier = Modifier.padding(DesignTokens.Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(
                            color = if (state.isTracking && !state.isPaused) DesignTokens.StatusColors.success else DesignTokens.StatusColors.neutral,
                            shape = CircleShape,
                        ),
                )
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                Text(
                    text = "Kalman smoothing + haversine distance — the production tracking math, running in wasm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = DesignTokens.Shape.roundedSm) {
        Column(Modifier.padding(DesignTokens.Spacing.m)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.terminalStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.terminalStyle(),
            )
        }
    }
}

private fun formatElapsed(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "${m}m ${s.toString().padStart(2, '0')}s"
}
