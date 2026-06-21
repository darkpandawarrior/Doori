package com.miletracker.core.data.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Douglas-Peucker path simplification for UI rendering only.
 * NEVER use for distance calculations — loses 5–25% accuracy.
 */
object PathSimplifier {
    object Epsilon {
        const val VERY_SMOOTH = 0.0001
        const val SMOOTH = 0.00005
        const val SUBTLE = 0.00002
        const val MINIMAL = 0.00001
    }

    /**
     * Simplify a list of (lat, lng) pairs for display-only polyline rendering.
     * @param points list of GeoPoint
     * @param epsilon distance threshold in degrees (~0.00001 ≈ 1 m)
     */
    fun simplify(
        points: List<GeoPoint>,
        epsilon: Double = Epsilon.SMOOTH,
    ): List<GeoPoint> {
        if (points.size <= 2) return points
        return douglasPeucker(points, epsilon)
    }

    private fun douglasPeucker(
        points: List<GeoPoint>,
        epsilon: Double,
    ): List<GeoPoint> {
        if (points.size <= 2) return points
        var maxDistance = 0.0
        var maxIndex = 0
        val start = points.first()
        val end = points.last()
        for (i in 1 until points.size - 1) {
            val d = perpendicularDistance(points[i], start, end)
            if (d > maxDistance) {
                maxDistance = d
                maxIndex = i
            }
        }
        return if (maxDistance > epsilon) {
            val left = douglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    private fun perpendicularDistance(
        point: GeoPoint,
        lineStart: GeoPoint,
        lineEnd: GeoPoint,
    ): Double {
        val x = point.longitude
        val y = point.latitude
        val x1 = lineStart.longitude
        val y1 = lineStart.latitude
        val x2 = lineEnd.longitude
        val y2 = lineEnd.latitude
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0.0 && dy == 0.0) return sqrt((x - x1).pow(2) + (y - y1).pow(2))
        val numerator = abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1)
        return numerator / sqrt(dx.pow(2) + dy.pow(2))
    }

    fun getStats(
        original: List<GeoPoint>,
        simplified: List<GeoPoint>,
    ): SimplificationStats {
        val pct = if (original.isNotEmpty()) (original.size - simplified.size).toDouble() / original.size * 100 else 0.0
        return SimplificationStats(original.size, simplified.size, original.size - simplified.size, pct)
    }

    data class SimplificationStats(
        val originalPoints: Int,
        val simplifiedPoints: Int,
        val pointsRemoved: Int,
        val reductionPercent: Double,
    ) {
        override fun toString(): String =
            "Simplified: $originalPoints → $simplifiedPoints points ($pointsRemoved removed, ${reductionPercent.fmt1d()}% reduction)"
    }
}

/** Lightweight lat/lng pair used by [PathSimplifier] and [KalmanSmoother]. */
data class GeoPoint(val latitude: Double, val longitude: Double)
