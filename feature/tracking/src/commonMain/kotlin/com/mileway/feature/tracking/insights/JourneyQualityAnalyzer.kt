package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.SavedTrack

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
        var score = 100

        fun deduct(
            label: String,
            points: Int,
        ) {
            score -= points
            factors += ScoreFactor(label, points)
        }

        if (track.wasMockLocationUsed) deduct("Mock location detected", 20)
        if (track.wasBatteryOptimizationEnabled) deduct("Battery optimisation on", 15)
        if (track.wasPowerSaverEnabled) deduct("Power saver on", 15)
        if (track.wasAppKilled) deduct("App killed during trip", 25)
        if (track.wasPermissionsViolated) deduct("Permissions violated", 15)
        if (track.wasPhoneShutDown) deduct("Device shutdown during trip", 25)

        // Unsynced-point ratio → up to −20
        if (track.unsyncedLocationPoints > 0 && track.totalLocationPoints > 0) {
            val unsyncedRatio = track.unsyncedLocationPoints.toDouble() / track.totalLocationPoints
            val deduction = (unsyncedRatio * 20).toInt()
            if (deduction > 0) deduct("Unsynced location points (${(unsyncedRatio * 100).toInt()}%)", deduction)
        }

        // Sparse data by average GPS interval (seconds between points)
        if (track.totalLocationPoints > 0 && track.duration > 0) {
            val avgInterval = (track.duration / 1000) / track.totalLocationPoints
            when {
                avgInterval > 30 -> deduct("Very sparse GPS data (avg ${avgInterval}s interval)", 15)
                avgInterval > 15 -> deduct("Sparse GPS data (avg ${avgInterval}s interval)", 10)
                avgInterval > 10 -> deduct("Slightly sparse GPS data (avg ${avgInterval}s interval)", 5)
            }
        }

        if (track.totalLocationPoints < 10) deduct("Very few data points (${track.totalLocationPoints})", 10)

        // Bonuses
        var bonus = 0
        if (track.totalLocationPoints >= 50) bonus += 5
        if (track.duration >= 300_000L) bonus += 5
        if (track.cleanedDistance > 0 && track.originalDistance > 0) {
            val cleanRatio = track.cleanedDistance / track.originalDistance
            if (cleanRatio > 0.9) bonus += 5
        }
        score += bonus

        return QualityResult(
            qualityScore = score.coerceIn(0, 100),
            dataCompleteness = calculateDataCompleteness(track),
            reliabilityScore = calculateReliabilityScore(track),
            scoreFactors = factors,
        )
    }

    private fun calculateDataCompleteness(track: SavedTrack): Double {
        val duration = track.duration.coerceAtLeast(1000L)
        val samplingRate =
            when {
                duration < 5 * 60 * 1000 -> 2.0 // <5 min  → 2 s/point
                duration < 30 * 60 * 1000 -> 3.0 // <30 min → 3 s/point
                else -> 5.0 // longer  → 5 s/point
            }
        val expectedPoints = (duration / 1000.0) / samplingRate
        val ratio = track.totalLocationPoints.toDouble() / expectedPoints.coerceAtLeast(1.0)
        val durationFactor = (1.0 + (duration.toDouble() / (60 * 60 * 1000)).coerceAtMost(2.0)) / 3.0
        return (ratio * (1.0 + durationFactor)).coerceIn(0.0, 1.0)
    }

    private fun calculateReliabilityScore(track: SavedTrack): Int {
        var reliability = 100

        if (track.cleanedDistance > 0 && track.originalDistance > 0) {
            val problemRatio =
                (track.abnormalDistance + track.mockDistance) /
                    track.originalDistance.coerceAtLeast(0.1)
            reliability -= (problemRatio * 50).toInt().coerceAtMost(50)

            val cleanRatio = track.cleanedDistance / track.originalDistance.coerceAtLeast(0.1)
            if (cleanRatio < 0.7 || cleanRatio > 1.3) reliability -= 20
        } else {
            reliability -= 15
        }

        if (track.wasMockLocationUsed) reliability -= 20
        if (track.wasAppKilled) reliability -= 15
        if (track.wasPhoneShutDown) reliability -= 15

        if (track.totalLocationPoints > 0 && track.distance > 0) {
            val pointsPerKm = track.totalLocationPoints / (track.distance / 1000.0).coerceAtLeast(0.1)
            if (pointsPerKm < 10) reliability -= 10
        }

        return reliability.coerceIn(0, 100)
    }
}
