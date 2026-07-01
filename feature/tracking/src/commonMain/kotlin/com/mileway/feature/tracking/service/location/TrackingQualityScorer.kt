package com.mileway.feature.tracking.service.location

/** Live tracking-environment signals used to score the *current* fix quality. */
data class QualityInputs(
    val isMock: Boolean = false,
    val isBatteryOptimized: Boolean = false,
    val isPowerSaver: Boolean = false,
    val wasAppKilled: Boolean = false,
    val wasRestarted: Boolean = false,
    val isPermissionMissing: Boolean = false,
    val isGpsOff: Boolean = false,
    val accuracyM: Float = 0f,
    val isStable: Boolean = false,
)

/**
 * Pure, live tracking-quality scorer. Starts at [MAX] and subtracts a fixed penalty per
 * active environmental issue, applies an accuracy-tier penalty, then adds a small bonus when
 * the fix stream is stable. Result is clamped to [[MIN], [MAX]].
 *
 * This is the *live* counterpart to the post-hoc
 * [com.mileway.feature.tracking.insights.JourneyQualityAnalyzer]: it scores conditions as
 * they happen (for the tracking notification / quality chip), not the finished journey.
 *
 * No Android dependency, so it is fully covered by JVM unit tests.
 */
object TrackingQualityScorer {
    const val MAX = 100
    const val MIN = 0

    const val PENALTY_MOCK = 25
    const val PENALTY_BATTERY_OPT = 15
    const val PENALTY_POWER_SAVER = 15
    const val PENALTY_APP_KILLED = 20
    const val PENALTY_RESTART = 20
    const val PENALTY_PERMISSION = 30
    const val PENALTY_GPS_OFF = 20
    const val STABLE_BONUS = 5

    /** Accuracy-tier penalty (m). 0 (unknown) is treated as no penalty. */
    private fun accuracyPenalty(accuracyM: Float): Int =
        when {
            accuracyM <= 0f -> 0
            accuracyM <= 15f -> 0
            accuracyM <= 35f -> 5
            accuracyM <= 75f -> 10
            else -> 20
        }

    fun score(inputs: QualityInputs): Int {
        var score = MAX
        if (inputs.isMock) score -= PENALTY_MOCK
        if (inputs.isBatteryOptimized) score -= PENALTY_BATTERY_OPT
        if (inputs.isPowerSaver) score -= PENALTY_POWER_SAVER
        if (inputs.wasAppKilled) score -= PENALTY_APP_KILLED
        if (inputs.wasRestarted) score -= PENALTY_RESTART
        if (inputs.isPermissionMissing) score -= PENALTY_PERMISSION
        if (inputs.isGpsOff) score -= PENALTY_GPS_OFF
        score -= accuracyPenalty(inputs.accuracyM)
        if (inputs.isStable) score += STABLE_BONUS
        return score.coerceIn(MIN, MAX)
    }
}
