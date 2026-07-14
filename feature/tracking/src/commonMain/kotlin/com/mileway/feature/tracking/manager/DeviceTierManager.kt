package com.mileway.feature.tracking.manager

/** RAM-based device capability tier, coarsest signal we have for "how conservative should GPS be." */
enum class DeviceTier { LOW, MID, HIGH }

/**
 * Pure policy: maps total device RAM to a [DeviceTier], and each tier to interval/threshold
 * multipliers. Low-end devices get a more conservative (longer) GPS interval and looser
 * thresholds to save battery/CPU; HIGH is today's baseline (multiplier 1.0 everywhere), so
 * plugging this in changes nothing on default/high-end hardware.
 */
object DeviceTierManager {
    // ponytail: round-number RAM cutoffs, not measured against real low-end hardware —
    // tighten if a specific device model misclassifies.
    private const val LOW_RAM_MAX_BYTES = 3L * 1024 * 1024 * 1024 // < 3 GB
    private const val MID_RAM_MAX_BYTES = 6L * 1024 * 1024 * 1024 // < 6 GB

    private const val LOW_INTERVAL_MULTIPLIER = 1.5
    private const val MID_INTERVAL_MULTIPLIER = 1.2
    private const val HIGH_INTERVAL_MULTIPLIER = 1.0

    private const val LOW_THRESHOLD_MULTIPLIER = 1.5
    private const val MID_THRESHOLD_MULTIPLIER = 1.2
    private const val HIGH_THRESHOLD_MULTIPLIER = 1.0

    fun tierFor(totalRamBytes: Long): DeviceTier =
        when {
            totalRamBytes < LOW_RAM_MAX_BYTES -> DeviceTier.LOW
            totalRamBytes < MID_RAM_MAX_BYTES -> DeviceTier.MID
            else -> DeviceTier.HIGH
        }

    /** Multiplies into [com.siddharth.kmp.location.DynamicIntervalCalculator]'s interval. */
    fun intervalMultiplier(tier: DeviceTier): Double =
        when (tier) {
            DeviceTier.LOW -> LOW_INTERVAL_MULTIPLIER
            DeviceTier.MID -> MID_INTERVAL_MULTIPLIER
            DeviceTier.HIGH -> HIGH_INTERVAL_MULTIPLIER
        }

    /** Not yet wired into a consumer — reserved for the abnormal-detection thresholds (parity §2.2). */
    fun thresholdMultiplier(tier: DeviceTier): Double =
        when (tier) {
            DeviceTier.LOW -> LOW_THRESHOLD_MULTIPLIER
            DeviceTier.MID -> MID_THRESHOLD_MULTIPLIER
            DeviceTier.HIGH -> HIGH_THRESHOLD_MULTIPLIER
        }
}
