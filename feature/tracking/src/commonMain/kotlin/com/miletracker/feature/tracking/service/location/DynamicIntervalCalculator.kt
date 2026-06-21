package com.miletracker.feature.tracking.service.location

import kotlin.math.roundToLong

/** Inputs for one location-request interval recomputation. */
data class IntervalInputs(
    val speedMps: Double,
    val batteryPct: Int,
    val isCharging: Boolean,
    val isPowerSaver: Boolean,
    val elapsedMs: Long,
)

/**
 * Pure, platform-independent location-request interval calculator.
 *
 * Picks a base interval from the current speed band, then *stretches* it for low battery,
 * power-saver mode, and long-running sessions to conserve power. The result is clamped to
 * [[MIN_INTERVAL_MS], [MAX_INTERVAL_MS]].
 *
 * Faster movement → shorter interval (denser track). Low battery / power-saver / long
 * duration → longer interval (fewer fixes). Charging disables the battery penalty entirely.
 *
 * No Android dependency, so it is fully covered by JVM unit tests. The service (C.2a) calls
 * [intervalMs] per fix and re-registers the FLP request only when the value changes ≥1s.
 */
object DynamicIntervalCalculator {
    const val MIN_INTERVAL_MS = 5_000L
    const val MAX_INTERVAL_MS = 60_000L

    // Speed-band upper bounds (m/s).
    private const val IDLE_MAX_MPS = 0.5 // < ~1.8 km/h: stationary
    private const val WALK_MAX_MPS = 4.0 // < ~14 km/h: walking / jogging
    private const val CITY_MAX_MPS = 11.0 // < ~40 km/h: city driving
    private const val HIGHWAY_MAX_MPS = 25.0 // < ~90 km/h: fast road

    // Per-band base intervals (ms): idle 20s → highway 5s.
    private const val BASE_IDLE_MS = 20_000L
    private const val BASE_WALK_MS = 15_000L
    private const val BASE_CITY_MS = 10_000L
    private const val BASE_HIGHWAY_MS = 6_000L
    private const val BASE_FAST_MS = 5_000L

    private fun baseForSpeed(speedMps: Double): Long =
        when {
            speedMps < IDLE_MAX_MPS -> BASE_IDLE_MS
            speedMps < WALK_MAX_MPS -> BASE_WALK_MS
            speedMps < CITY_MAX_MPS -> BASE_CITY_MS
            speedMps < HIGHWAY_MAX_MPS -> BASE_HIGHWAY_MS
            else -> BASE_FAST_MS
        }

    private fun batteryMultiplier(
        pct: Int,
        charging: Boolean,
    ): Double =
        when {
            charging -> 1.0
            pct < 30 -> 2.0
            pct < 50 -> 1.5
            else -> 1.0
        }

    private fun durationMultiplier(elapsedMs: Long): Double {
        val hours = elapsedMs / 3_600_000.0
        return when {
            hours > 6 -> 1.75
            hours > 4 -> 1.5
            hours > 2 -> 1.25
            else -> 1.0
        }
    }

    /** Compute the location-request interval in milliseconds, clamped to the allowed range. */
    fun intervalMs(inputs: IntervalInputs): Long {
        var ms = baseForSpeed(inputs.speedMps).toDouble()
        ms *= batteryMultiplier(inputs.batteryPct, inputs.isCharging)
        if (inputs.isPowerSaver) ms *= 1.5
        ms *= durationMultiplier(inputs.elapsedMs)
        return ms.roundToLong().coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
    }
}
