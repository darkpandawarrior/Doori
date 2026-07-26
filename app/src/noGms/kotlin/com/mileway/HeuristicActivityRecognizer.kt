package com.mileway

import com.mileway.core.platform.MotionSensorProvider
import com.mileway.core.platform.MotionState
import com.mileway.core.platform.toMotionState
import com.mileway.feature.tracking.service.location.ActivityRecognizer
import com.mileway.feature.tracking.service.location.RecognizedActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * noGms/F-Droid flavor [ActivityRecognizer]: no Play Services here, so the signal comes from the IMU
 * instead — `core:platform`'s accelerometer fusion ([toMotionState]) folded into [RecognizedActivity]
 * by [MotionActivityMapper].
 *
 * What that buys, and deliberately nothing more: **still vs not-still**. Subtracting the low-pass gravity
 * estimate from raw accelerometer samples separates a phone at rest from a phone being carried or driven;
 * it cannot separate walking from cycling from riding in a car, since all three read as the same broadband
 * jostle. Movement therefore maps to [RecognizedActivity.UNKNOWN] rather than a guessed mode — only
 * [RecognizedActivity.STILL] is ever asserted, which is the one value `LocationTrackingService` consumes
 * (its stillness check). This is a strictly weaker classifier than Play Services' `DetectedActivity`, which
 * fuses the IMU with a trained model; the gms flavor keeps using that via [GmsActivityRecognizer].
 *
 * ponytail: telling ON_FOOT from IN_VEHICLE needs a speed signal (GPS ground speed), which nothing hands
 * to this seam today — add it as a second [MotionActivityMapper] input once the tracking pipeline can
 * supply one, rather than inventing a travel mode from the accelerometer alone.
 *
 * Collecting [activity] starts and stops the shared [MotionSensorProvider], which is a Koin `single` that
 * `ShakeGestureDetector` (mounted app-wide) also reads. That is safe only because [MotionSensorProvider]
 * start/stop is reference-counted — the sensor is unregistered when the LAST consumer releases it, not
 * when this one does. Do not "simplify" that ref-count away.
 */
class HeuristicActivityRecognizer(
    private val motionSensorProvider: MotionSensorProvider,
) : ActivityRecognizer {
    override val activity: Flow<RecognizedActivity> =
        motionSensorProvider.readings
            .toMotionState()
            .map(MotionActivityMapper::fromMotionState)
            .distinctUntilChanged()
            .onStart { motionSensorProvider.start() }
            .onCompletion { motionSensorProvider.stop() }
}

/**
 * Pure [MotionState] → [RecognizedActivity] mapping, kept apart from the flow plumbing so it is
 * JVM-unit-testable without a SensorManager — the same reason `ActivityTypeMapper` sits next to the
 * [ActivityRecognizer] interface rather than inside the gms implementation.
 */
object MotionActivityMapper {
    fun fromMotionState(state: MotionState): RecognizedActivity =
        when (state) {
            MotionState.STILL -> RecognizedActivity.STILL
            // A gravity-subtracted accelerometer says "not at rest"; it does not say which travel mode.
            MotionState.MOVING, MotionState.UNKNOWN -> RecognizedActivity.UNKNOWN
        }
}
