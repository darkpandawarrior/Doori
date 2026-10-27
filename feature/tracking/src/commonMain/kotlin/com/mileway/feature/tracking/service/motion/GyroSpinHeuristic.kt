package com.mileway.feature.tracking.service.motion

import kotlin.math.sqrt

/**
 * Wave-2 IMU polish: sustained high gyro magnitude is a "phone being physically spun" tell —
 * one of the cheap mock-location/spoofing hints (a mocked GPS route can't fabricate a matching
 * gyro signal, so real high-rate spin alongside suspicious location data is a possible-mock cue,
 * not proof by itself). Pure threshold check on the raw gyro vector (rad/s).
 */
object GyroSpinHeuristic {
    // ~3.5 rad/s (~200 deg/s) is well above normal hand-jitter/turn rates for a phone
    // resting in a mount; loosen if real-world driving turns start tripping it.
    const val SPIN_THRESHOLD_RAD_S = 3.5f

    /** True when the raw gyro magnitude (rad/s) clears the spin threshold. */
    fun isPossibleSpin(
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
        threshold: Float = SPIN_THRESHOLD_RAD_S,
    ): Boolean = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ) > threshold
}
