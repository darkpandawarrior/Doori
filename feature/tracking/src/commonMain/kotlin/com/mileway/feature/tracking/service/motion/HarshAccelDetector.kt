package com.mileway.feature.tracking.service.motion

import kotlin.math.sqrt

/**
 * Wave-2 IMU polish: flags harsh acceleration/braking from the linear-acceleration magnitude
 * (gravity already subtracted upstream by `MotionFusion`). Pure threshold check — no direction
 * split between "harsh accel" and "harsh brake" since the magnitude alone is what the tracking
 * pipeline needs (both harden jitter suppression + shorten the GPS interval the same way).
 */
object HarshAccelDetector {
    // single magnitude threshold, not accel-vs-brake specific. ~1g of linear
    // acceleration (excluding gravity) reads as a hard stop/launch on a phone in a cupholder.
    // Tighten/split if false positives show up on real driving data.
    const val HARSH_THRESHOLD_MPS2 = 4.0f

    /** True when the linear-acceleration magnitude (m/s^2) clears the harsh-event threshold. */
    fun isHarsh(
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        threshold: Float = HARSH_THRESHOLD_MPS2,
    ): Boolean = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ) > threshold
}
