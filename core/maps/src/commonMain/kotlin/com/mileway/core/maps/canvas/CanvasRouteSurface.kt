package com.mileway.core.maps.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.mileway.core.maps.MapCoordinate
import com.mileway.core.maps.MapSurface

/**
 * [MapSurface] implementation with pure Compose Canvas — no map SDK, no tiles, no network.
 *
 * Promoted from the hand-rolled polyline in `app-web-preview`'s TrackingScreen into a real,
 * tested component because it earns its place three times over: it's the only option on wasmJs
 * (no MapLibre/MapKit there), it backs the desktop target, and on every platform it's the
 * "tiles unavailable" degraded state — a normal condition for a driving app in a tunnel or a
 * rural dead zone, not an edge case. No animation: this is the fallback surface, it must be cheap.
 */
@Suppress("ktlint:standard:function-naming")
class CanvasRouteSurface : MapSurface {
    @Composable
    override fun LocationPinMap(
        latitude: Double,
        longitude: Double,
        modifier: Modifier,
    ) {
        val pinColor = MaterialTheme.colorScheme.primary
        Canvas(modifier.fillMaxSize()) {
            drawCircle(pinColor, radius = MARKER_RADIUS_DP.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f))
        }
    }

    @Composable
    override fun LiveTrackMap(
        routeCoords: List<MapCoordinate>,
        filteredCoords: List<MapCoordinate>,
        abnormalCoords: List<MapCoordinate>,
        startCoord: MapCoordinate?,
        endCoord: MapCoordinate?,
        currentLat: Double,
        currentLng: Double,
        bearing: Float,
        autoCenterEnabled: Boolean,
        playbackCoord: MapCoordinate?,
        showIssueMarkers: Boolean,
        showCompass: Boolean,
        showTraffic: Boolean,
        offlineTiles: Boolean,
        modifier: Modifier,
    ) {
        // Colour separation matches the real map surfaces (clean/filtered/abnormal), sourced from
        // MaterialTheme so every theme (light/dark/accent) re-tints it — never hardcoded here.
        val colors =
            RouteColors(
                clean = MaterialTheme.colorScheme.primary,
                filtered = MaterialTheme.colorScheme.tertiary,
                abnormal = MaterialTheme.colorScheme.error,
                end = MaterialTheme.colorScheme.secondary,
            )

        Canvas(modifier.fillMaxSize()) {
            val points =
                fitLiveTrackPoints(
                    routeCoords = routeCoords,
                    filteredCoords = if (showIssueMarkers) filteredCoords else emptyList(),
                    abnormalCoords = if (showIssueMarkers) abnormalCoords else emptyList(),
                    startCoord = startCoord,
                    endCoord = endCoord,
                    current = MapCoordinate(currentLat, currentLng),
                    playbackCoord = playbackCoord,
                    padding = PADDING_DP.dp.toPx(),
                )
            drawLiveTrack(points, colors, bearing)
        }
    }

    /** Fitted (canvas-space) points for one [LiveTrackMap] frame, all sharing one projection. */
    private class LiveTrackPoints(
        val route: List<Offset>,
        val filtered: List<Offset>,
        val abnormal: List<Offset>,
        val start: Offset?,
        val end: Offset?,
        val playback: Offset?,
        val current: Offset,
    )

    private class RouteColors(
        val clean: Color,
        val filtered: Color,
        val abnormal: Color,
        val end: Color,
    )

    /**
     * One combined projection pass so route, markers and the current-position head all share the
     * same bounds/scale/origin — fitting each independently would put them in geometrically
     * inconsistent places.
     */
    private fun DrawScope.fitLiveTrackPoints(
        routeCoords: List<MapCoordinate>,
        filteredCoords: List<MapCoordinate>,
        abnormalCoords: List<MapCoordinate>,
        startCoord: MapCoordinate?,
        endCoord: MapCoordinate?,
        current: MapCoordinate,
        playbackCoord: MapCoordinate?,
        padding: Float,
    ): LiveTrackPoints {
        val optional = listOfNotNull(startCoord, endCoord, playbackCoord)
        val combined = routeCoords + filteredCoords + abnormalCoords + optional + listOf(current)
        val fitted = MercatorProjection.fitToCanvas(combined, size.width, size.height, padding)

        var cursor = 0

        fun take(n: Int): List<Offset> = fitted.subList(cursor, cursor + n).also { cursor += n }

        fun takeOptional(coord: MapCoordinate?): Offset? = if (coord != null) take(1).first() else null

        val route = take(routeCoords.size)
        val filtered = take(filteredCoords.size)
        val abnormal = take(abnormalCoords.size)
        val start = takeOptional(startCoord)
        val end = takeOptional(endCoord)
        val playback = takeOptional(playbackCoord)
        val currentPt = take(1).first()

        return LiveTrackPoints(route, filtered, abnormal, start, end, playback, currentPt)
    }

    private fun DrawScope.drawLiveTrack(
        points: LiveTrackPoints,
        colors: RouteColors,
        bearing: Float,
    ) {
        drawPolyline(points.route, colors.clean)
        points.filtered.forEach { drawCircle(colors.filtered, radius = ISSUE_RADIUS_DP.dp.toPx(), center = it) }
        points.abnormal.forEach { drawCircle(colors.abnormal, radius = ISSUE_RADIUS_DP.dp.toPx(), center = it) }
        points.start?.let { drawCircle(colors.clean, MARKER_RADIUS_DP.dp.toPx(), it) }
        points.end?.let { drawCircle(colors.end, MARKER_RADIUS_DP.dp.toPx(), it) }
        points.playback?.let { drawCircle(colors.clean.copy(alpha = PLAYBACK_ALPHA), MARKER_RADIUS_DP.dp.toPx(), it) }
        drawCurrentPositionHead(points.current, bearing, colors.clean)
    }

    private fun DrawScope.drawPolyline(
        points: List<Offset>,
        color: Color,
    ) {
        if (points.size < 2) return
        val path =
            Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
        drawPath(
            path,
            color,
            style = Stroke(width = STROKE_WIDTH_DP.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }

    /** A small triangle pointing "up" at bearing 0, rotated to the live heading around its own center. */
    private fun DrawScope.drawCurrentPositionHead(
        center: Offset,
        bearing: Float,
        color: Color,
    ) {
        rotate(degrees = bearing, pivot = center) {
            val r = CURRENT_RADIUS_DP.dp.toPx()
            val arrow =
                Path().apply {
                    moveTo(center.x, center.y - r)
                    lineTo(center.x - r * ARROW_HALF_WIDTH_FRACTION, center.y + r * ARROW_HALF_WIDTH_FRACTION)
                    lineTo(center.x + r * ARROW_HALF_WIDTH_FRACTION, center.y + r * ARROW_HALF_WIDTH_FRACTION)
                    close()
                }
            drawPath(arrow, color)
        }
    }

    private companion object {
        // core:maps has no dependency on core:ui/DesignTokens (module boundary — see AGENTS.md),
        // so these are plain dp constants rather than DesignTokens.Spacing/*. Colours still come
        // from MaterialTheme per the theming requirement above.
        const val PADDING_DP = 16
        const val STROKE_WIDTH_DP = 3
        const val MARKER_RADIUS_DP = 5
        const val ISSUE_RADIUS_DP = 3
        const val CURRENT_RADIUS_DP = 7
        const val PLAYBACK_ALPHA = 0.6f
        const val ARROW_HALF_WIDTH_FRACTION = 0.6f
    }
}
