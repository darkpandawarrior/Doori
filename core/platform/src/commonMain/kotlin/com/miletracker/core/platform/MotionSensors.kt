package com.miletracker.core.platform

import kotlinx.coroutines.flow.Flow
import kotlin.math.sqrt

/** One fused IMU sample (O): raw accelerometer + gyroscope axes plus a capture timestamp. */
data class MotionReading(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val timestampMillis: Long = 0L,
)

/** A 3-axis vector result from [MotionFusion] (gravity / linear-acceleration estimates). */
data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}

/**
 * Pure cross-platform sensor fusion (O) — no Android / iOS types, so it's fully JVM-unit-testable. A
 * single-pole low-pass filter isolates the slow-moving gravity vector from the accelerometer; subtracting it
 * yields the device's linear acceleration (the part the user actually caused). [isMoving] thresholds the
 * linear-acceleration magnitude for a cheap stationary-vs-moving signal the tracking engine can use.
 */
object MotionFusion {
    /** Smoothing factor for the gravity low-pass filter (0..1; higher = slower to react). */
    const val DEFAULT_ALPHA = 0.8f

    /** Linear-acceleration magnitude (m/s²) above which the device is considered moving. */
    const val MOVEMENT_THRESHOLD = 0.6f

    /** Update the running gravity estimate with a new accelerometer sample. */
    fun updateGravity(
        previous: Vector3,
        reading: MotionReading,
        alpha: Float = DEFAULT_ALPHA,
    ): Vector3 =
        Vector3(
            x = alpha * previous.x + (1 - alpha) * reading.accelX,
            y = alpha * previous.y + (1 - alpha) * reading.accelY,
            z = alpha * previous.z + (1 - alpha) * reading.accelZ,
        )

    /** The user-induced acceleration: the raw sample minus the [gravity] estimate. */
    fun linearAcceleration(
        reading: MotionReading,
        gravity: Vector3,
    ): Vector3 =
        Vector3(
            x = reading.accelX - gravity.x,
            y = reading.accelY - gravity.y,
            z = reading.accelZ - gravity.z,
        )

    /** True when the linear-acceleration magnitude clears [MOVEMENT_THRESHOLD]. */
    fun isMoving(
        reading: MotionReading,
        gravity: Vector3,
        threshold: Float = MOVEMENT_THRESHOLD,
    ): Boolean = linearAcceleration(reading, gravity).magnitude > threshold
}

/**
 * Cross-platform motion sensor stream (O). Android: SensorManager (accelerometer + gyroscope); iOS:
 * CoreMotion (CMMotionManager). Bound per platform through `platformModule()`.
 */
interface MotionSensorProvider {
    /** Hot stream of fused IMU readings while [start]ed. */
    val readings: Flow<MotionReading>

    fun start()

    fun stop()
}
