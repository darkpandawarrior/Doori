package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack

/**
 * Pure-Kotlin distance-quality analyzer.
 *
 * Score rules:
 *   - Start at 100.
 *   - Deduct (problematicDistancePct * 0.5), capped at 50.
 *   - Deduct (problemPointPct * 0.3),         capped at 30.
 *   - Extra −10 if mockPct > 30 %  (severe mock pollution).
 *   - Extra  −5 if abnormalPct > 20 % (significant jumps).
 *   - Clamped to [0, 100].
 *
 * Reliability threshold (preserved): score ≥ 70 AND cleanedDistanceRatio ≥ 0.8.
 */
object DistanceQualityAnalyzer {
    fun analyze(
        track: SavedTrack,
        points: List<LocationData>,
    ): DistanceQualityResult {
        val mockCount = points.count { it.isMock }
        val abnormalCount = points.count { it.isAbnormal }
        val totalCount = points.size

        val score =
            computeScore(
                mockDistance = track.mockDistance,
                abnormalDistance = track.abnormalDistance,
                totalDistance = track.originalDistance.takeIf { it > 0 } ?: track.distance,
                mockCount = mockCount,
                abnormalCount = abnormalCount,
                totalCount = totalCount,
            )

        val totalDist = (track.originalDistance.takeIf { it > 0 } ?: track.distance).coerceAtLeast(0.001)
        val mockPct = (track.mockDistance / totalDist) * 100.0
        val abnormalPct = (track.abnormalDistance / totalDist) * 100.0
        val cleanedRatio = getCleanedDistanceRatio(track.cleanedDistance, totalDist)

        return DistanceQualityResult(
            score = score,
            assessment = getAssessment(score),
            cleanedDistanceRatio = cleanedRatio,
            isReliableForBusiness = score >= 70 && cleanedRatio >= 0.8,
            mockPct = mockPct,
            abnormalPct = abnormalPct,
        )
    }

    fun computeScore(
        mockDistance: Double,
        abnormalDistance: Double,
        totalDistance: Double,
        mockCount: Int,
        abnormalCount: Int,
        totalCount: Int,
    ): Int {
        if (totalDistance <= 0 || totalCount <= 0) {
            return if (mockCount > 0 || abnormalCount > 0) 0 else 100
        }

        val mockPct = (mockDistance / totalDistance) * 100.0
        val abnormalPct = (abnormalDistance / totalDistance) * 100.0
        val problematicPct = mockPct + abnormalPct

        val problemPointPct = ((mockCount + abnormalCount) / totalCount.toDouble()) * 100.0

        var score = 100
        score -= (problematicPct * 0.5).toInt().coerceAtMost(50)
        score -= (problemPointPct * 0.3).toInt().coerceAtMost(30)
        if (mockPct > 30.0) score -= 10
        if (abnormalPct > 20.0) score -= 5

        return score.coerceIn(0, 100)
    }

    fun getAssessment(score: Int): String =
        when {
            score >= 90 -> "Excellent quality tracking data"
            score >= 75 -> "Good quality tracking data"
            score >= 60 -> "Acceptable tracking data with minor issues"
            score >= 40 -> "Fair tracking data with some quality issues"
            score >= 20 -> "Poor tracking data with significant issues"
            else -> "Very poor tracking data quality"
        }

    fun getCleanedDistanceRatio(
        cleanedDistance: Double,
        totalDistance: Double,
    ): Double = if (totalDistance > 0) (cleanedDistance / totalDistance).coerceIn(0.0, 1.0) else 1.0
}
