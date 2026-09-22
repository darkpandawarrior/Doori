package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.util.MillisPerHour
import com.mileway.core.data.util.MillisPerMinute
import com.mileway.core.data.util.MillisPerSecond

// The scoring rule stated in JourneyQualityAnalyzer's KDoc below, as constants. Written out so the
// doc comment and the arithmetic cannot drift apart, and so a penalty can be retuned in one place.
private const val PerfectScore = 100
private const val WorstScore = 0

// Penalties: how much each recorded hazard costs the journey.
private const val MockLocationPenalty = 20
private const val BatteryOptimisationPenalty = 15
private const val PowerSaverPenalty = 15
private const val AppKilledPenalty = 25
private const val PermissionsViolatedPenalty = 15
private const val PhoneShutDownPenalty = 25

/** A fully unsynced track loses this much; a partly unsynced one loses it pro rata. */
private const val MaxUnsyncedPenalty = 20

// Sparse-GPS bands, in seconds between consecutive fixes.
private const val VerySparseIntervalSec = 30
private const val SparseIntervalSec = 15
private const val SlightlySparseIntervalSec = 10
private const val VerySparsePenalty = 15
private const val SparsePenalty = 10
private const val SlightlySparsePenalty = 5

private const val MinUsefulPoints = 10
private const val FewPointsPenalty = 10

// Bonuses for a track that is dense, long and clean.
private const val DensePointCount = 50
private const val CleanDistanceRatio = 0.9
private const val QualityBonus = 5

// The five-minute mark does double duty: below it a trip is "short" and gets the densest expected
// sampling rate, at or above it the trip earns the duration bonus. One boundary, one constant.
private const val ShortTripMillis = 5L * MillisPerMinute
private const val MediumTripMillis = 30L * MillisPerMinute

// Expected sampling rate per band, in seconds per point.
private const val ShortTripSamplingSec = 2.0
private const val MediumTripSamplingSec = 3.0
private const val LongTripSamplingSec = 5.0

/** Longer trips are allowed a looser completeness bar; this caps how much slack they earn. */
private const val MaxDurationSlackHours = 2.0
private const val DurationSlackDivisor = 3.0

// Reliability scoring, which is scored separately from journey quality.
private const val ProblemDistanceWeight = 50
private const val MaxProblemDistancePenalty = 50
private const val CleanRatioLowerBound = 0.7
private const val CleanRatioUpperBound = 1.3
private const val CleanRatioOutOfBandPenalty = 20
private const val NoDistanceDataPenalty = 15
private const val UnreliableMockPenalty = 20
private const val UnreliableAppKilledPenalty = 15
private const val UnreliableShutDownPenalty = 15
private const val MinPointsPerKm = 10
private const val SparsePerKmPenalty = 10

/** Floors for divides where a track may legitimately have recorded almost nothing. */
private const val MinDurationMillis = 1_000L
private const val MinDistanceMetres = 0.1
private const val MinExpectedPoints = 1.0

private const val MetresPerKm = 1_000.0
private const val PctScale = 100

/**
 * Pure-Kotlin analyzer for overall journey quality.
 *
 * Scoring rules:
 *   - mock location used          → −20
 *   - battery optimisation on     → −15
 *   - power saver on              → −15
 *   - app killed                  → −25
 *   - permissions violated        → −15
 *   - phone shut down             → −25
 *   - unsynced-point ratio        → up to −20
 *   - sparse data (avg interval)  → −5 / −10 / −15
 *   - fewer than 10 points        → −10
 *   - ≥50 points bonus            → +5
 *   - duration ≥5 min bonus       → +5
 *   - clean-ratio >0.9 bonus      → +5
 *   Final clamped to [0, 100].
 *
 * Data-completeness uses a duration-adaptive expected sampling rate:
 *   <5 min → 2 s/point, <30 min → 3 s/point, else → 5 s/point.
 */
class JourneyQualityAnalyzer {
    fun analyze(track: SavedTrack): QualityResult {
        val factors = mutableListOf<ScoreFactor>()
        var score = PerfectScore

        fun deduct(
            label: String,
            points: Int,
        ) {
            score -= points
            factors += ScoreFactor(label, points)
        }

        if (track.wasMockLocationUsed) deduct("Mock location detected", MockLocationPenalty)
        if (track.wasBatteryOptimizationEnabled) deduct("Battery optimisation on", BatteryOptimisationPenalty)
        if (track.wasPowerSaverEnabled) deduct("Power saver on", PowerSaverPenalty)
        if (track.wasAppKilled) deduct("App killed during trip", AppKilledPenalty)
        if (track.wasPermissionsViolated) deduct("Permissions violated", PermissionsViolatedPenalty)
        if (track.wasPhoneShutDown) deduct("Device shutdown during trip", PhoneShutDownPenalty)

        // Unsynced-point ratio → up to −20
        if (track.unsyncedLocationPoints > 0 && track.totalLocationPoints > 0) {
            val unsyncedRatio = track.unsyncedLocationPoints.toDouble() / track.totalLocationPoints
            val deduction = (unsyncedRatio * MaxUnsyncedPenalty).toInt()
            if (deduction > 0) deduct("Unsynced location points (${(unsyncedRatio * PctScale).toInt()}%)", deduction)
        }

        // Sparse data by average GPS interval (seconds between points)
        if (track.totalLocationPoints > 0 && track.duration > 0) {
            val avgInterval = (track.duration / MillisPerSecond) / track.totalLocationPoints
            when {
                avgInterval > VerySparseIntervalSec -> deduct("Very sparse GPS data (avg ${avgInterval}s interval)", VerySparsePenalty)
                avgInterval > SparseIntervalSec -> deduct("Sparse GPS data (avg ${avgInterval}s interval)", SparsePenalty)
                avgInterval > SlightlySparseIntervalSec -> deduct("Slightly sparse GPS data (avg ${avgInterval}s interval)", SlightlySparsePenalty)
            }
        }

        if (track.totalLocationPoints < MinUsefulPoints) {
            deduct("Very few data points (${track.totalLocationPoints})", FewPointsPenalty)
        }

        // Bonuses
        var bonus = 0
        if (track.totalLocationPoints >= DensePointCount) bonus += QualityBonus
        if (track.duration >= ShortTripMillis) bonus += QualityBonus
        if (track.cleanedDistance > 0 && track.originalDistance > 0) {
            val cleanRatio = track.cleanedDistance / track.originalDistance
            if (cleanRatio > CleanDistanceRatio) bonus += QualityBonus
        }
        score += bonus

        return QualityResult(
            qualityScore = score.coerceIn(WorstScore, PerfectScore),
            dataCompleteness = calculateDataCompleteness(track),
            reliabilityScore = calculateReliabilityScore(track),
            scoreFactors = factors,
        )
    }

    private fun calculateDataCompleteness(track: SavedTrack): Double {
        val duration = track.duration.coerceAtLeast(MinDurationMillis)
        val samplingRate =
            when {
                duration < ShortTripMillis -> ShortTripSamplingSec
                duration < MediumTripMillis -> MediumTripSamplingSec
                else -> LongTripSamplingSec
            }
        val expectedPoints = (duration / MillisPerSecond.toDouble()) / samplingRate
        val ratio = track.totalLocationPoints.toDouble() / expectedPoints.coerceAtLeast(MinExpectedPoints)
        val durationFactor =
            (1.0 + (duration.toDouble() / MillisPerHour.toDouble()).coerceAtMost(MaxDurationSlackHours)) / DurationSlackDivisor
        return (ratio * (1.0 + durationFactor)).coerceIn(0.0, 1.0)
    }

    private fun calculateReliabilityScore(track: SavedTrack): Int {
        var reliability = PerfectScore

        if (track.cleanedDistance > 0 && track.originalDistance > 0) {
            val problemRatio =
                (track.abnormalDistance + track.mockDistance) /
                    track.originalDistance.coerceAtLeast(MinDistanceMetres)
            reliability -= (problemRatio * ProblemDistanceWeight).toInt().coerceAtMost(MaxProblemDistancePenalty)

            val cleanRatio = track.cleanedDistance / track.originalDistance.coerceAtLeast(MinDistanceMetres)
            if (cleanRatio < CleanRatioLowerBound || cleanRatio > CleanRatioUpperBound) reliability -= CleanRatioOutOfBandPenalty
        } else {
            reliability -= NoDistanceDataPenalty
        }

        if (track.wasMockLocationUsed) reliability -= UnreliableMockPenalty
        if (track.wasAppKilled) reliability -= UnreliableAppKilledPenalty
        if (track.wasPhoneShutDown) reliability -= UnreliableShutDownPenalty

        if (track.totalLocationPoints > 0 && track.distance > 0) {
            val pointsPerKm = track.totalLocationPoints / (track.distance / MetresPerKm).coerceAtLeast(MinDistanceMetres)
            if (pointsPerKm < MinPointsPerKm) reliability -= SparsePerKmPenalty
        }

        return reliability.coerceIn(WorstScore, PerfectScore)
    }
}
