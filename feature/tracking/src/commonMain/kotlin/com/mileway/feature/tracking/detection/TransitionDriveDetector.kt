package com.mileway.feature.tracking.detection

import kotlinx.serialization.Serializable

/** Signals from a low power source. Departure alone never proves driving. */
enum class DriveTransition { VEHICLE_ENTER, VEHICLE_EXIT, DEPARTURE }

/** Persisted reducer state lets a manifest receiver survive process recreation. */
@Serializable
data class DriveTransitionState(
    val armed: Boolean = true,
    val requiresDeparture: Boolean = false,
    val inVehicle: Boolean = false,
    val departed: Boolean = false,
    val armedAtNanos: Long = 0,
    val lastEventNanos: Long = 0,
)

/** Emits one recording decision per arm, after motion and any requested departure. */
class TransitionDriveDetector(
    var state: DriveTransitionState = DriveTransitionState(),
) {
    fun accept(
        transition: DriveTransition,
        elapsedNanos: Long,
    ): Boolean {
        if (!state.armed || elapsedNanos < state.armedAtNanos || elapsedNanos < state.lastEventNanos) return false
        state =
            when (transition) {
                DriveTransition.VEHICLE_ENTER -> state.copy(inVehicle = true, lastEventNanos = elapsedNanos)
                DriveTransition.VEHICLE_EXIT -> state.copy(inVehicle = false, lastEventNanos = elapsedNanos)
                DriveTransition.DEPARTURE -> state.copy(departed = true)
            }
        if (!state.inVehicle || (state.requiresDeparture && !state.departed)) return false
        state = state.copy(armed = false)
        return true
    }
}
