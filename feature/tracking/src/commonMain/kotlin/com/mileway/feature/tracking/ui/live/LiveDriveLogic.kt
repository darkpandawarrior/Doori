package com.mileway.feature.tracking.ui.live

import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.feature.tracking.viewmodel.TrackSignal

// Pure logic for LiveDriveScreen — no Compose, no platform APIs, fully unit-testable. Kept apart
// from the composable so the state machine can be verified without a device.

// ── Camera ─────────────────────────────────────────────────────────────────────────

/** How the map camera is currently behaving. */
enum class CameraMode { FOLLOW, OVERVIEW, FREE }

/** Everything that can move [CameraMode]. */
sealed interface CameraEvent {
    /** User tapped the map — cycles FOLLOW → OVERVIEW → FREE → FOLLOW. */
    data object Tap : CameraEvent

    /** User panned/dragged the map. */
    data object UserPan : CameraEvent

    /** [CAMERA_IDLE_TIMEOUT_MS] elapsed with no interaction. */
    data object IdleTimeout : CameraEvent

    /** The trip just became paused or stopped — auto-enter OVERVIEW. */
    data object TripPausedOrStopped : CameraEvent

    /** The trip just (re)entered TRACKING — default back to FOLLOW. */
    data object TripTracking : CameraEvent
}

/** Idle time before a non-FOLLOW camera mode auto-returns to FOLLOW. */
const val CAMERA_IDLE_TIMEOUT_MS: Long = 8_000L

/**
 * Pure camera-mode reducer. [TripPausedOrStopped]/[TripTracking] are phase-driven and always win;
 * [Tap] cycles through the three modes; [UserPan] always lands on FREE; [IdleTimeout] always
 * returns to FOLLOW.
 */
fun CameraMode.reduce(event: CameraEvent): CameraMode =
    when (event) {
        CameraEvent.TripPausedOrStopped -> CameraMode.OVERVIEW
        CameraEvent.TripTracking -> CameraMode.FOLLOW
        CameraEvent.IdleTimeout -> CameraMode.FOLLOW
        CameraEvent.UserPan -> CameraMode.FREE
        CameraEvent.Tap ->
            when (this) {
                CameraMode.FOLLOW -> CameraMode.OVERVIEW
                CameraMode.OVERVIEW -> CameraMode.FREE
                CameraMode.FREE -> CameraMode.FOLLOW
            }
    }

// ── Head-position interpolation ─────────────────────────────────────────────────────

/** Never animate faster than this — a snap between fixes looks like a glitch, not motion. */
private const val MIN_INTERPOLATION_MS = 600L

/** Never animate slower than this — a stale-looking crawl if the next fix is already overdue. */
private const val MAX_INTERPOLATION_MS = 3_000L

/**
 * The dot must arrive exactly when the next fix is due: never early, never overshooting. Duration
 * is therefore the measured gap to the previous fix, clamped to a sane animatable range — never a
 * fixed duration.
 */
fun interpolationDurationMs(measuredGapMs: Long): Long = measuredGapMs.coerceIn(MIN_INTERPOLATION_MS, MAX_INTERPOLATION_MS)

// ── Zoom-for-speed ───────────────────────────────────────────────────────────────────

private const val ZOOM_AT_STANDSTILL = 18f
private const val ZOOM_AT_HIGHWAY_SPEED = 14f
private const val HIGHWAY_SPEED_KMH = 100.0

/**
 * Linear zoom-out as speed rises: tight on the driver at a standstill, wide enough to read the
 * road ahead at highway speed. Monotone by construction (linear interpolation of a clamped input),
 * which is what keeps the camera from ever appearing to "breathe".
 *
 * ponytail: [MapSurface][com.mileway.core.maps.MapSurface] has no zoom parameter yet, so this
 * value is computed and ready but not yet fed to the map — wire it in once that contract grows one.
 */
fun zoomForSpeed(speedKmh: Double): Float {
    val t = (speedKmh.coerceIn(0.0, HIGHWAY_SPEED_KMH) / HIGHWAY_SPEED_KMH).toFloat()
    return ZOOM_AT_STANDSTILL - (ZOOM_AT_STANDSTILL - ZOOM_AT_HIGHWAY_SPEED) * t
}

// ── Bearing freeze ───────────────────────────────────────────────────────────────────

/** Below this speed GPS course-over-ground is noise, not a real heading. */
const val BEARING_FREEZE_THRESHOLD_KMH = 5.0

data class BearingDisplay(val degrees: Float, val isFrozen: Boolean)

/** Freeze the last valid heading (and mark it dimmed) below [BEARING_FREEZE_THRESHOLD_KMH]. */
fun deriveBearingDisplay(
    speedKmh: Double,
    rawBearing: Float,
    lastValidBearing: Float,
): BearingDisplay =
    if (speedKmh < BEARING_FREEZE_THRESHOLD_KMH) {
        BearingDisplay(degrees = lastValidBearing, isFrozen = true)
    } else {
        BearingDisplay(degrees = rawBearing, isFrozen = false)
    }

// ── Degraded states ──────────────────────────────────────────────────────────────────

/** Every condition this screen must explain, ranked most urgent first. */
enum class DegradedState {
    NONE,
    PERMISSION_REVOKED,
    NO_FIX,
    OFFLINE,
    POOR_ACCURACY,
    RECOVERED_AFTER_GAP,
    BATTERY_SAVER,
}

data class DegradedInputs(
    val hasFix: Boolean,
    val signal: TrackSignal,
    val flags: TrackingSystemFlags,
    val isOffline: Boolean,
    val recoveredAfterGap: Boolean,
)

/** Priority-ordered so only the single most urgent condition is ever surfaced at once. */
fun deriveDegradedState(inputs: DegradedInputs): DegradedState =
    when {
        inputs.flags.permissionMissing -> DegradedState.PERMISSION_REVOKED
        !inputs.hasFix -> DegradedState.NO_FIX
        inputs.isOffline -> DegradedState.OFFLINE
        inputs.signal == TrackSignal.POOR -> DegradedState.POOR_ACCURACY
        inputs.recoveredAfterGap -> DegradedState.RECOVERED_AFTER_GAP
        inputs.flags.powerSaverOn || inputs.flags.batteryOptimized -> DegradedState.BATTERY_SAVER
        else -> DegradedState.NONE
    }

/** What is happening, and what (if anything) the user can do about it. */
data class DegradedMessage(val title: String, val action: String?)

fun DegradedState.message(): DegradedMessage? =
    when (this) {
        DegradedState.NONE -> null
        DegradedState.PERMISSION_REVOKED ->
            DegradedMessage("Location permission was revoked", "Grant it in Settings to keep tracking")
        DegradedState.NO_FIX ->
            DegradedMessage("Waiting for a GPS fix", "Move to open sky for a faster lock")
        DegradedState.OFFLINE ->
            DegradedMessage("No map tiles — you're offline", "Tracking continues; the map redraws once you're back online")
        DegradedState.POOR_ACCURACY ->
            DegradedMessage("GPS accuracy is poor", "Distance may be less precise until signal improves")
        DegradedState.RECOVERED_AFTER_GAP ->
            DegradedMessage("GPS signal recovered", null)
        DegradedState.BATTERY_SAVER ->
            DegradedMessage("Battery saver may slow location updates", "Distance still tracks, just less often")
    }
