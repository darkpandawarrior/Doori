package com.mileway.feature.tracking.service.location

import com.mileway.core.data.model.db.LocationData

/**
 * P-A.2: Authoritative finalize-time distance recompute over persisted DB points.
 *
 * Consecutive-Haversine over all clean points (excluding isAbnormal, isMock, isPaused).
 * Pure Kotlin — no Android dependency, fully JVM-unit-testable.
 */
object DistanceCalculator {
    /**
     * Compute the cleaned distance in metres from a saved-point list.
     * Returns 0.0 if the list has fewer than 2 eligible points.
     */
    fun computeCleanedDistance(points: List<LocationData>): Double {
        val eligible = points.filter { !it.isAbnormal && !it.isMock && !it.isPaused }
        if (eligible.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until eligible.size) {
            val a = eligible[i - 1]
            val b = eligible[i]
            total += haversineMeters(a.lat, a.lng, b.lat, b.lng)
        }
        return total
    }
}
