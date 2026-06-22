package com.miletracker.feature.tracking.service.location

import kotlinx.coroutines.flow.Flow

/** O.2: coarse user activity from on-device activity recognition (Android ActivityRecognition / iOS CoreMotion). */
enum class RecognizedActivity { UNKNOWN, STILL, ON_FOOT, ON_BICYCLE, IN_VEHICLE }

/**
 * O.2: a live stream of the user's recognized activity. The Android impl is backed by Play Services
 * ActivityRecognition; on a device without Play Services it simply never emits (the IMU MotionState fusion
 * from O.1/O.3 still drives stillness), so the offline/FOSS path degrades gracefully.
 */
interface ActivityRecognizer {
    val activity: Flow<RecognizedActivity>
}

/**
 * Pure mapper from a Play Services `DetectedActivity.type` code to [RecognizedActivity] — kept here (not in
 * androidMain) so it is JVM-unit-testable without the gms types. The integer codes mirror the stable
 * `com.google.android.gms.location.DetectedActivity` constants.
 */
object ActivityTypeMapper {
    private const val IN_VEHICLE = 0
    private const val ON_BICYCLE = 1
    private const val ON_FOOT = 2
    private const val STILL = 3
    private const val WALKING = 7
    private const val RUNNING = 8

    fun fromDetectedType(type: Int): RecognizedActivity =
        when (type) {
            IN_VEHICLE -> RecognizedActivity.IN_VEHICLE
            ON_BICYCLE -> RecognizedActivity.ON_BICYCLE
            ON_FOOT, WALKING, RUNNING -> RecognizedActivity.ON_FOOT
            STILL -> RecognizedActivity.STILL
            else -> RecognizedActivity.UNKNOWN
        }
}
