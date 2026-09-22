package com.mileway.feature.tracking.ui.util

import com.mileway.core.data.model.db.SavedTrack

/**
 * Score bands for a track's integrity. Ordered best-first; [forScore] takes the first band the
 * score still clears, so a score below every band's floor lands on [CRITICAL].
 */
enum class HealthLevel(
    private val minScore: Int,
) {
    EXCELLENT(90),
    GOOD(70),
    FAIR(50),
    POOR(30),
    CRITICAL(Int.MIN_VALUE),
    ;

    internal companion object {
        fun forScore(score: Int): HealthLevel = entries.first { score >= it.minScore }
    }
}

// What each recorded violation costs a perfect track. These weights are the policy, so they are
// named: a reviewer arguing whether a killed app is worse than a power saver reads them here.
private const val PerfectScore = 100
private const val MockLocationPenalty = 40
private const val PermissionsViolatedPenalty = 30
private const val PhoneShutDownPenalty = 20
private const val AppKilledPenalty = 15
private const val BatteryOptimizationPenalty = 10
private const val PowerSaverPenalty = 10

fun computeHealthLevel(track: SavedTrack): HealthLevel {
    var score = PerfectScore
    if (track.wasMockLocationUsed) score -= MockLocationPenalty
    if (track.wasPermissionsViolated) score -= PermissionsViolatedPenalty
    if (track.wasPhoneShutDown) score -= PhoneShutDownPenalty
    if (track.wasAppKilled) score -= AppKilledPenalty
    if (track.wasBatteryOptimizationEnabled) score -= BatteryOptimizationPenalty
    if (track.wasPowerSaverEnabled) score -= PowerSaverPenalty
    return HealthLevel.forScore(score)
}
