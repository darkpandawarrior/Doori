package com.mileway.feature.tracking.ui.live

import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.feature.tracking.viewmodel.TrackSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveDriveLogicTest {
    // ── zoom-for-speed ───────────────────────────────────────────────────────────

    @Test
    fun `zoomForSpeed is tightest at a standstill`() {
        assertEquals(18f, zoomForSpeed(0.0))
    }

    @Test
    fun `zoomForSpeed reaches its floor at and beyond highway speed`() {
        assertEquals(14f, zoomForSpeed(100.0))
        assertEquals(14f, zoomForSpeed(250.0))
    }

    @Test
    fun `zoomForSpeed is monotonically non-increasing as speed rises`() {
        val speeds = listOf(0.0, 10.0, 25.0, 40.0, 60.0, 80.0, 100.0)
        val zooms = speeds.map(::zoomForSpeed)
        for (i in 1 until zooms.size) {
            assertTrue(zooms[i] <= zooms[i - 1], "zoom should never increase as speed rises: $zooms")
        }
    }

    @Test
    fun `zoomForSpeed never breathes past a standstill zoom for negative or huge speeds`() {
        // Defensive: a bad sensor reading must not push the camera past the designed bounds.
        assertEquals(18f, zoomForSpeed(-5.0))
        assertEquals(14f, zoomForSpeed(9_999.0))
    }

    // ── interpolation duration clamping ─────────────────────────────────────────

    @Test
    fun `interpolationDurationMs clamps a too-fast gap up to the floor`() {
        assertEquals(600L, interpolationDurationMs(50L))
        assertEquals(600L, interpolationDurationMs(0L))
    }

    @Test
    fun `interpolationDurationMs clamps a too-slow gap down to the ceiling`() {
        assertEquals(3_000L, interpolationDurationMs(10_000L))
    }

    @Test
    fun `interpolationDurationMs passes a normal gap through unchanged`() {
        assertEquals(1_200L, interpolationDurationMs(1_200L))
    }

    // ── bearing freeze ───────────────────────────────────────────────────────────

    @Test
    fun `bearing freezes below the threshold speed`() {
        val display = deriveBearingDisplay(speedKmh = 2.0, rawBearing = 90f, lastValidBearing = 10f)
        assertEquals(10f, display.degrees)
        assertTrue(display.isFrozen)
    }

    @Test
    fun `bearing follows the live fix at or above the threshold speed`() {
        val display = deriveBearingDisplay(speedKmh = BEARING_FREEZE_THRESHOLD_KMH, rawBearing = 90f, lastValidBearing = 10f)
        assertEquals(90f, display.degrees)
        assertFalse(display.isFrozen)
    }

    // ── camera-mode transitions ──────────────────────────────────────────────────

    @Test
    fun `tap cycles FOLLOW to OVERVIEW to FREE and back to FOLLOW`() {
        var mode = CameraMode.FOLLOW
        mode = mode.reduce(CameraEvent.Tap)
        assertEquals(CameraMode.OVERVIEW, mode)
        mode = mode.reduce(CameraEvent.Tap)
        assertEquals(CameraMode.FREE, mode)
        mode = mode.reduce(CameraEvent.Tap)
        assertEquals(CameraMode.FOLLOW, mode)
    }

    @Test
    fun `pausing or stopping always forces OVERVIEW regardless of current mode`() {
        assertEquals(CameraMode.OVERVIEW, CameraMode.FOLLOW.reduce(CameraEvent.TripPausedOrStopped))
        assertEquals(CameraMode.OVERVIEW, CameraMode.FREE.reduce(CameraEvent.TripPausedOrStopped))
    }

    @Test
    fun `resuming tracking always defaults back to FOLLOW`() {
        assertEquals(CameraMode.FOLLOW, CameraMode.OVERVIEW.reduce(CameraEvent.TripTracking))
    }

    @Test
    fun `a user pan always lands on FREE`() {
        assertEquals(CameraMode.FREE, CameraMode.FOLLOW.reduce(CameraEvent.UserPan))
        assertEquals(CameraMode.FREE, CameraMode.OVERVIEW.reduce(CameraEvent.UserPan))
    }

    @Test
    fun `idle timeout always returns to FOLLOW`() {
        assertEquals(CameraMode.FOLLOW, CameraMode.FREE.reduce(CameraEvent.IdleTimeout))
        assertEquals(CameraMode.FOLLOW, CameraMode.OVERVIEW.reduce(CameraEvent.IdleTimeout))
    }

    // ── degraded-state derivation ────────────────────────────────────────────────

    private fun inputs(
        hasFix: Boolean = true,
        signal: TrackSignal = TrackSignal.GOOD,
        flags: TrackingSystemFlags = TrackingSystemFlags(),
        isOffline: Boolean = false,
        recoveredAfterGap: Boolean = false,
    ) = DegradedInputs(hasFix, signal, flags, isOffline, recoveredAfterGap)

    @Test
    fun `a healthy state derives NONE`() {
        assertEquals(DegradedState.NONE, deriveDegradedState(inputs()))
    }

    @Test
    fun `permission missing outranks every other condition`() {
        val everythingBad =
            inputs(
                hasFix = false,
                signal = TrackSignal.POOR,
                flags = TrackingSystemFlags(permissionMissing = true, powerSaverOn = true),
                isOffline = true,
                recoveredAfterGap = true,
            )
        assertEquals(DegradedState.PERMISSION_REVOKED, deriveDegradedState(everythingBad))
    }

    @Test
    fun `no fix outranks offline, poor accuracy, and battery saver`() {
        val state =
            deriveDegradedState(
                inputs(
                    hasFix = false,
                    signal = TrackSignal.POOR,
                    flags = TrackingSystemFlags(powerSaverOn = true),
                    isOffline = true,
                ),
            )
        assertEquals(DegradedState.NO_FIX, state)
    }

    @Test
    fun `offline outranks poor accuracy and battery saver`() {
        val state =
            deriveDegradedState(
                inputs(signal = TrackSignal.POOR, flags = TrackingSystemFlags(batteryOptimized = true), isOffline = true),
            )
        assertEquals(DegradedState.OFFLINE, state)
    }

    @Test
    fun `poor accuracy outranks recovered-after-gap and battery saver`() {
        val state =
            deriveDegradedState(
                inputs(signal = TrackSignal.POOR, flags = TrackingSystemFlags(powerSaverOn = true), recoveredAfterGap = true),
            )
        assertEquals(DegradedState.POOR_ACCURACY, state)
    }

    @Test
    fun `battery saver alone is the lowest-priority condition shown`() {
        val state = deriveDegradedState(inputs(flags = TrackingSystemFlags(batteryOptimized = true)))
        assertEquals(DegradedState.BATTERY_SAVER, state)
    }

    @Test
    fun `NONE has no message, every real degraded state has a title`() {
        assertEquals(null, DegradedState.NONE.message())
        DegradedState.entries.filter { it != DegradedState.NONE }.forEach { state ->
            assertTrue(state.message()!!.title.isNotBlank(), "expected a title for $state")
        }
    }
}
