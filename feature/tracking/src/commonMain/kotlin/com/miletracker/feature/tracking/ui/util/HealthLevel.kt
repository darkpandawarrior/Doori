package com.miletracker.feature.tracking.ui.util

import com.miletracker.core.data.model.db.SavedTrack

enum class HealthLevel { EXCELLENT, GOOD, FAIR, POOR, CRITICAL }

fun computeHealthLevel(track: SavedTrack): HealthLevel {
    var score = 100
    if (track.wasMockLocationUsed) score -= 40
    if (track.wasBatteryOptimizationEnabled) score -= 10
    if (track.wasPowerSaverEnabled) score -= 10
    if (track.wasAppKilled) score -= 15
    if (track.wasPhoneShutDown) score -= 20
    if (track.wasPermissionsViolated) score -= 30
    return when {
        score >= 90 -> HealthLevel.EXCELLENT
        score >= 70 -> HealthLevel.GOOD
        score >= 50 -> HealthLevel.FAIR
        score >= 30 -> HealthLevel.POOR
        else -> HealthLevel.CRITICAL
    }
}
