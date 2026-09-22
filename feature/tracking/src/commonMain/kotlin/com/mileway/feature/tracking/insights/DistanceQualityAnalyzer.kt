package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack

// The scoring rule stated in DistanceQualityAnalyzer's KDoc below, as constants. Every one of these
// was a bare literal in the arithmetic, so the doc and the code could drift with nothing noticing.
private const val PerfectScore = 100
private const val WorstScore = 0

/** Weight and cap for the share of DISTANCE that came from mock or abnormal points. */
private const val ProblematicDistanceWeight = 0.5
private const val ProblematicDistanceMaxDeduction = 50

/** Weight and cap for the share of POINTS that were mock or abnormal. */
private const val ProblemPointWeight = 0.3
private const val ProblemPointMaxDeduction = 30

/** Mock pollution past this share of the distance is severe enough for a flat extra deduction. */
private const val SevereMockPct = 30.0
private const val SevereMockDeduction = 10

/** Likewise for abnormal (jump) distance, which is the milder of the two signals. */
private const val SignificantAbnormalPct = 20.0
private const val SignificantAbnormalDeduction = 5

/** A trip is safe to claim on expenses only above this score and this much surviving distance. */
private const val BusinessReliableScore = 70
private const val BusinessReliableCleanedRatio = 0.8

// Assessment bands, highest first.
private const val ExcellentScore = 90
private const val GoodScore = 75
private const val AcceptableScore = 60
private const val FairScore = 40
private const val PoorScore = 20

/** Guards the divide when a track recorded no distance at all. */
private const val MinDivisorMetres = 0.001

private const val PctScale = 100.0

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

        val totalDist = (track.originalDistance.takeIf { it > 0 } ?: track.distance).coerceAtLeast(MinDivisorMetres)
        val mockPct = (track.mockDistance / totalDist) * PctScale
        val abnormalPct = (track.abnormalDistance / totalDist) * PctScale
        val cleanedRatio = getCleanedDistanceRatio(track.cleanedDistance, totalDist)

        return DistanceQualityResult(
            score = score,
            assessment = getAssessment(score),
            cleanedDistanceRatio = cleanedRatio,
            isReliableForBusiness = score >= BusinessReliableScore && cleanedRatio >= BusinessReliableCleanedRatio,
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
            return if (mockCount > 0 || abnormalCount > 0) WorstScore else PerfectScore
        }

        val mockPct = (mockDistance / totalDistance) * PctScale
        val abnormalPct = (abnormalDistance / totalDistance) * PctScale
        val problematicPct = mockPct + abnormalPct

        val problemPointPct = ((mockCount + abnormalCount) / totalCount.toDouble()) * PctScale

        var score = PerfectScore
        score -= (problematicPct * ProblematicDistanceWeight).toInt().coerceAtMost(ProblematicDistanceMaxDeduction)
        score -= (problemPointPct * ProblemPointWeight).toInt().coerceAtMost(ProblemPointMaxDeduction)
        if (mockPct > SevereMockPct) score -= SevereMockDeduction
        if (abnormalPct > SignificantAbnormalPct) score -= SignificantAbnormalDeduction

        return score.coerceIn(WorstScore, PerfectScore)
    }

    fun getAssessment(score: Int): String =
        when {
            score >= ExcellentScore -> "Excellent quality tracking data"
            score >= GoodScore -> "Good quality tracking data"
            score >= AcceptableScore -> "Acceptable tracking data with minor issues"
            score >= FairScore -> "Fair tracking data with some quality issues"
            score >= PoorScore -> "Poor tracking data with significant issues"
            else -> "Very poor tracking data quality"
        }

    fun getCleanedDistanceRatio(
        cleanedDistance: Double,
        totalDistance: Double,
    ): Double = if (totalDistance > 0) (cleanedDistance / totalDistance).coerceIn(0.0, 1.0) else 1.0

    /** Raw per-category distance buckets already stored on [track] — no recomputation. */
    fun getBucketBreakdown(track: SavedTrack): DistanceBucketBreakdown =
        DistanceBucketBreakdown(
            originalDistance = track.originalDistance,
            cleanedDistance = track.cleanedDistance,
            mockDistance = track.mockDistance,
            abnormalDistance = track.abnormalDistance,
            spikeDistance = track.spikeDistance,
        )
}
