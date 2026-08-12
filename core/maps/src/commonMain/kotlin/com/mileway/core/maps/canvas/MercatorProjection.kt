package com.mileway.core.maps.canvas

import androidx.compose.ui.geometry.Offset
import com.mileway.core.maps.MapCoordinate
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.tan

/**
 * Pure lat/lng -> canvas-space math for [com.mileway.core.maps.canvas.CanvasRouteSurface].
 *
 * No Compose draw calls here on purpose — this is the part that's cheap and worthwhile to unit
 * test directly (see MercatorProjectionTest), independent of any DrawScope/Canvas.
 */
internal object MercatorProjection {
    // Standard Web Mercator latitude clamp — beyond this the projection runs to +/-infinity at
    // the poles. A bogus GPS fix (or a deliberately degenerate test) must not turn into a crash.
    private const val MAX_LAT = 85.05112878
    private const val EPS = 1e-9
    private const val ANTIMERIDIAN_DEG = 180.0

    private const val DEG_TO_RAD = PI / 180.0

    /** Web Mercator x/y in radians on a unit sphere (R=1 — it cancels out once we auto-fit). */
    internal fun project(
        lat: Double,
        lng: Double,
    ): Pair<Double, Double> {
        val clampedLat = lat.coerceIn(-MAX_LAT, MAX_LAT)
        val x = lng * DEG_TO_RAD
        val y = ln(tan(PI / 4 + (clampedLat * DEG_TO_RAD) / 2))
        return x to y
    }

    /**
     * Unwraps a longitude sequence so a route crossing the antimeridian (+180/-180) stays one
     * continuous line instead of jumping across the whole projected world. Each longitude is
     * shifted by whole multiples of 360 so its delta from the previous (already-unwrapped) point
     * never exceeds 180 degrees.
     */
    internal fun unwrapLongitudes(lngs: List<Double>): List<Double> {
        if (lngs.isEmpty()) return emptyList()
        val out = ArrayList<Double>(lngs.size)
        out.add(lngs[0])
        for (i in 1 until lngs.size) {
            var lng = lngs[i]
            val prev = out[i - 1]
            while (lng - prev > ANTIMERIDIAN_DEG) lng -= 2 * ANTIMERIDIAN_DEG
            while (lng - prev < -ANTIMERIDIAN_DEG) lng += 2 * ANTIMERIDIAN_DEG
            out.add(lng)
        }
        return out
    }

    /**
     * Projects [coords] into canvas-space [Offset]s (origin top-left, y down), auto-fit to the
     * points' own bounds with [padding] px on every side, at ONE uniform scale for both axes so
     * the route never gets squashed out of aspect ratio.
     *
     * Degenerate input is handled honestly instead of crashing or dividing by zero:
     *  - empty input -> empty output.
     *  - a single point, or all-identical points -> every point lands on the canvas center (a
     *    dot, not a line).
     *  - zero span on only one axis (e.g. a perfectly north-south route) -> that axis is centered
     *    without letting a division by zero poison the other axis's scale.
     */
    fun fitToCanvas(
        coords: List<MapCoordinate>,
        canvasWidth: Float,
        canvasHeight: Float,
        padding: Float = 0f,
    ): List<Offset> {
        if (coords.isEmpty()) return emptyList()

        val unwrappedLngs = unwrapLongitudes(coords.map { it.lng })
        val projected = coords.mapIndexed { i, c -> project(c.lat, unwrappedLngs[i]) }

        val xs = projected.map { it.first }
        val ys = projected.map { it.second }
        val minX = xs.min()
        val maxX = xs.max()
        val minY = ys.min()
        val maxY = ys.max()
        val spanX = maxX - minX
        val spanY = maxY - minY

        val availW = (canvasWidth - 2 * padding).coerceAtLeast(0f).toDouble()
        val availH = (canvasHeight - 2 * padding).coerceAtLeast(0f).toDouble()

        // A zero span on an axis means every point already agrees on that axis — its "scale"
        // is irrelevant (multiplied against a zero delta below), so a large-but-finite stand-in
        // keeps the min() below well-defined without risking a 0 * Infinity = NaN.
        val scaleX = if (spanX > EPS) availW / spanX else Double.MAX_VALUE
        val scaleY = if (spanY > EPS) availH / spanY else Double.MAX_VALUE
        val scale = min(scaleX, scaleY)

        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2
        val canvasCenterX = canvasWidth / 2.0
        val canvasCenterY = canvasHeight / 2.0

        return projected.map { (x, y) ->
            Offset(
                x = (canvasCenterX + (x - centerX) * scale).toFloat(),
                // Mercator y grows north (up); screen y grows down — flip it.
                y = (canvasCenterY - (y - centerY) * scale).toFloat(),
            )
        }
    }
}
