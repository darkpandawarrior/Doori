package com.miletracker.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.sqrt

/** O.1: coarse motion classification derived from the IMU stream, fused into the tracking pipeline. */
enum class MotionState { UNKNOWN, STILL, MOVING }

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
 * Pure cross-platform sensor fusion (O), no Android / iOS types, so it's fully JVM-unit-testable. A
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
 * O.1: fold an IMU [MotionReading] stream into a debounced [MotionState] (STILL/MOVING) using
 * [MotionFusion]'s gravity-isolated linear acceleration. Pure (no platform types), with fresh gravity state
 * per collection, and only emits on change — so a provider gets [MotionSensorProvider.motionState] for free
 * from its [MotionSensorProvider.readings].
 */
fun Flow<MotionReading>.toMotionState(threshold: Float = MotionFusion.MOVEMENT_THRESHOLD): Flow<MotionState> {
    val upstream = this
    return flow {
        var gravity = Vector3(0f, 0f, 0f)
        var last: MotionState? = null
        upstream.collect { reading ->
            gravity = MotionFusion.updateGravity(gravity, reading)
            val state =
                if (MotionFusion.isMoving(reading, gravity, threshold)) MotionState.MOVING else MotionState.STILL
            if (state != last) {
                last = state
                emit(state)
            }
        }
    }
}

/**
 * Cross-platform motion sensor stream (O). Android: SensorManager (accelerometer + gyroscope); iOS:
 * CoreMotion (CMMotionManager). Bound per platform through `platformModule()`.
 */
interface MotionSensorProvider {
    /** Hot stream of fused IMU readings while [start]ed. */
    val readings: Flow<MotionReading>

    /** O.1: debounced STILL/MOVING classification, derived from [readings]. */
    val motionState: Flow<MotionState>
        get() = readings.toMotionState()

    fun start()

    fun stop()
}
