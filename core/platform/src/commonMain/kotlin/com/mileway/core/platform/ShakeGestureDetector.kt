package com.mileway.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * P31.MISC.1: shake-to-report gesture, derived from the same accelerometer stream O fuses for motion
 * state ([MotionSensorProvider.readings]). [MotionFusion]'s gravity low-pass filter doubles as the
 * high-pass filter here — subtracting the slow-moving gravity estimate from each raw sample isolates
 * the sharp, user-caused jolts a shake produces. [ShakeSpikeTracker] then debounces those jolts into
 * one shake event: several sharp accelerations in a short window (a deliberate shake), not a single
 * bump or the ordinary jostle of driving.
 */
object ShakeGesture {
    /** Linear-acceleration magnitude (m/s²) a single jolt must clear to count as a shake spike. */
    const val SPIKE_THRESHOLD = 15f

    /** Spikes required inside [WINDOW_MS] before a shake event fires. */
    const val MIN_SPIKES = 3

    /** Rolling window (ms) spikes are counted in. */
    const val WINDOW_MS = 1_000L

    /** Minimum gap (ms) between two fired shake events, so one shake gesture can't fire twice. */
    const val COOLDOWN_MS = 2_000L
}

/**
 * Pure, unit-testable spike counter (no platform types, no Flow): feed it jolt timestamps (already
 * thresholded), it reports when a shake gesture just completed. [Flow<MotionReading>.toShakeEvents]
 * wraps this for the live sensor stream.
 */
class ShakeSpikeTracker(
    private val minSpikes: Int = ShakeGesture.MIN_SPIKES,
    private val windowMs: Long = ShakeGesture.WINDOW_MS,
    private val cooldownMs: Long = ShakeGesture.COOLDOWN_MS,
) {
    private val spikeTimestamps = ArrayDeque<Long>()
    private var lastShakeAtMs: Long? = null

    /** Record a jolt at [timestampMs]; returns true exactly when this jolt completes a shake gesture. */
    fun onSpike(timestampMs: Long): Boolean {
        spikeTimestamps.addLast(timestampMs)
        while (spikeTimestamps.isNotEmpty() && timestampMs - spikeTimestamps.first() > windowMs) {
            spikeTimestamps.removeFirst()
        }
        if (spikeTimestamps.size < minSpikes) return false
        val last = lastShakeAtMs
        if (last != null && timestampMs - last < cooldownMs) return false
        lastShakeAtMs = timestampMs
        spikeTimestamps.clear()
        return true
    }
}

/**
 * Folds a [MotionReading] stream into shake-gesture events (P31.MISC.1) — high-pass via
 * [MotionFusion] (gravity subtraction), spike-debounced via [ShakeSpikeTracker]. Emits [Unit] once
 * per completed shake.
 */
fun Flow<MotionReading>.toShakeEvents(
    spikeThreshold: Float = ShakeGesture.SPIKE_THRESHOLD,
    tracker: ShakeSpikeTracker = ShakeSpikeTracker(),
): Flow<Unit> {
    val upstream = this
    return flow {
        var gravity = Vector3(0f, 0f, 0f)
        upstream.collect { reading ->
            gravity = MotionFusion.updateGravity(gravity, reading)
            val magnitude = MotionFusion.linearAcceleration(reading, gravity).magnitude
            if (magnitude > spikeThreshold && tracker.onSpike(reading.timestampMillis)) {
                emit(Unit)
            }
        }
    }
}

/**
 * Thin wrapper bound in Koin so consumers ask for shake events without depending on
 * [MotionSensorProvider] directly (mirrors [MotionSensorProvider.motionState]'s pattern). Does not
 * start/stop the sensor itself — whatever already starts [MotionSensorProvider] for tracking/motion
 * state also drives this.
 */
class ShakeGestureDetector(private val motionSensorProvider: MotionSensorProvider) {
    val shakeEvents: Flow<Unit> get() = motionSensorProvider.readings.toShakeEvents()
}
