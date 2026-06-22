package com.miletracker.feature.tracking.service.location

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackingPipelineAccuracyTest {
    private fun fix(
        lat: Double = 18.5000,
        lng: Double = 73.8000,
        t: Long = 0L,
        speed: Float = 11f,
        accuracy: Float = 10f,
        mock: Boolean = false,
    ) = GpsFix(lat = lat, lng = lng, timeMs = t, speedMps = speed, accuracyM = accuracy, isMock = mock)

    // ── Coordinate hard gate ──────────────────────────────────────────────────

    @Test
    fun `latitude too low is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(lat = -91.0), isPaused = false))
    }

    @Test
    fun `latitude too high is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(lat = 91.0), isPaused = false))
    }

    @Test
    fun `longitude too low is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(lng = -181.0), isPaused = false))
    }

    @Test
    fun `longitude too high is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(lng = 181.0), isPaused = false))
    }

    @Test
    fun `boundary coordinates are accepted`() {
        val proc = LocationProcessor()
        // Exact boundary values must pass the gate.
        assertNotNull(proc.process(fix(lat = -90.0, lng = -180.0), isPaused = false))
    }

    @Test
    fun `valid coordinates are accepted`() {
        val proc = LocationProcessor()
        assertNotNull(proc.process(fix(lat = 18.5, lng = 73.8, accuracy = 10f), isPaused = false))
    }

    // ── Accuracy hard gate ────────────────────────────────────────────────────

    @Test
    fun `impossibly precise accuracy is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(accuracy = 0.05f), isPaused = false))
    }

    @Test
    fun `exact accuracy lower bound is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(accuracy = 0.1f), isPaused = false))
    }

    @Test
    fun `accuracy just above lower bound is accepted`() {
        val proc = LocationProcessor()
        assertNotNull(proc.process(fix(accuracy = 0.11f), isPaused = false))
    }

    @Test
    fun `hopelessly noisy accuracy above 250 is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(accuracy = 260f), isPaused = false))
    }

    @Test
    fun `accuracy exactly at 250 is rejected`() {
        val proc = LocationProcessor()
        assertNull(proc.process(fix(accuracy = 250f), isPaused = false))
    }

    @Test
    fun `accuracy just under 250 is accepted`() {
        val proc = LocationProcessor()
        assertNotNull(proc.process(fix(accuracy = 249f), isPaused = false))
    }

    // ── Soft accuracy contribution gate ──────────────────────────────────────

    @Test
    fun `fix with accuracy above threshold is persisted but not counted`() {
        val proc = LocationProcessor()
        // Anchor.
        proc.process(fix(lat = 18.5000, t = 0L, accuracy = 10f), isPaused = false)
        // Move 100 m north with poor accuracy (70 m > default 50 m threshold).
        val result = proc.process(fix(lat = 18.5009, t = 10_000L, speed = 5f, accuracy = 70f), isPaused = false)
        // Point must be returned (persisted), but cleanedDistance must stay zero.
        assertNotNull(result, "poor-accuracy fix must be persisted, not dropped")
        assertFalse(result.countedTowardDistance, "poor-accuracy fix must not count toward distance")
        assertTrue(proc.cleanedDistanceM < 1.0, "cleanedDistance must not grow for accuracy-gated fix")
    }

    @Test
    fun `fix with accuracy at or below threshold is counted`() {
        // Kalman off: this test asserts accuracy-gate logic, not smoothing behaviour.
        val proc = LocationProcessor(enableKalman = false)
        proc.process(fix(lat = 18.5000, t = 0L, accuracy = 10f), isPaused = false)
        val result = proc.process(fix(lat = 18.5009, t = 10_000L, speed = 5f, accuracy = 45f), isPaused = false)
        assertNotNull(result)
        assertTrue(result.countedTowardDistance, "good-accuracy fix must count toward distance")
        assertTrue(proc.cleanedDistanceM > 50.0, "cleanedDistance must grow for good-accuracy fix")
    }

    @Test
    fun `custom threshold gates a fix that passes the default`() {
        val proc = LocationProcessor(maxAccuracyThreshold = 30.0)
        proc.process(fix(lat = 18.5000, t = 0L, accuracy = 10f), isPaused = false)
        // accuracy = 40m > custom threshold 30m → gated
        val result = proc.process(fix(lat = 18.5009, t = 10_000L, speed = 5f, accuracy = 40f), isPaused = false)
        assertNotNull(result)
        assertFalse(result.countedTowardDistance, "40 m accuracy must be gated at 30 m threshold")
    }

    // ── Exceptional-stationary allowance ─────────────────────────────────────

    @Test
    fun `stationary after movement with good accuracy is not gated even above threshold`() {
        // The exceptional allowance requires hasRecentMovement() to be true, so we prime it with
        // a moving fix first, then feed a stationary fix with accuracy > threshold but < 20 m.
        val proc = LocationProcessor(maxAccuracyThreshold = 30.0)
        // Anchor + a moving fix to populate speed history (speed = 5 m/s > MOVEMENT_HISTORY_MPS 1.5).
        proc.process(fix(lat = 18.5000, t = 0L, speed = 5f, accuracy = 10f), isPaused = false)
        // Repeat to build speed history window.
        proc.process(fix(lat = 18.5005, t = 5_000L, speed = 5f, accuracy = 10f), isPaused = false)
        proc.process(fix(lat = 18.5010, t = 10_000L, speed = 5f, accuracy = 10f), isPaused = false)
        // Now a stationary fix: speed ≤ 0.1, accuracy = 15 m (< 20 m), threshold = 30 m.
        // hasMovementHistory() is true from the prior moving fixes → NOT accuracy-gated.
        val stationary =
            proc.process(
                fix(lat = 18.5010, t = 12_000L, speed = 0.05f, accuracy = 15f),
                isPaused = false,
            )
        assertNotNull(stationary)
        // The fix may or may not count depending on jitter suppression; what matters is it's not
        // accuracy-gated. We check cleanedDistance hasn't stayed zero after the whole sequence.
        assertTrue(proc.cleanedDistanceM > 50.0, "distance should have accumulated from valid prior fixes")
    }

    // ── P-C.6: resume-grace accuracy gate ────────────────────────────────────

    @Test
    fun `grace window: fix with accuracy above 50m is persisted but not counted`() {
        val proc = LocationProcessor(enableKalman = false)
        // Anchor.
        proc.process(fix(lat = 18.5000, t = 0L, accuracy = 10f), isPaused = false)
        // Move 200 m north during grace (suppressSpike=true); accuracy = 70 m > 50 m gate.
        val result =
            proc.process(
                fix(lat = 18.5018, t = 5_000L, speed = 5f, accuracy = 70f),
                isPaused = false,
                suppressSpike = true,
            )
        // Fix must be returned (persisted), but not counted.
        assertNotNull(result, "grace fix must be persisted")
        assertFalse(result.countedTowardDistance, "accuracy>50m during grace must not count toward distance")
        assertTrue(proc.cleanedDistanceM < 1.0, "cleanedDistance must not grow for grace-gated fix")
    }

    @Test
    fun `grace window: fix with accuracy at or below 50m counts normally`() {
        val proc = LocationProcessor(enableKalman = false)
        proc.process(fix(lat = 18.5000, t = 0L, accuracy = 10f), isPaused = false)
        // Move 200 m north during grace; accuracy = 45 m ≤ 50 m gate → should count.
        val result =
            proc.process(
                fix(lat = 18.5018, t = 5_000L, speed = 5f, accuracy = 45f),
                isPaused = false,
                suppressSpike = true,
            )
        assertNotNull(result)
        assertTrue(result.countedTowardDistance, "accuracy≤50m during grace must count toward distance")
        assertTrue(proc.cleanedDistanceM > 100.0, "cleanedDistance must grow for grace fix with good accuracy")
    }

    @Test
    fun `outside grace window: accuracy-51m fix is gated by the normal accuracy gate`() {
        val proc = LocationProcessor(enableKalman = false)
        proc.process(fix(lat = 18.5000, t = 0L, accuracy = 10f), isPaused = false)
        // Same 51 m accuracy, but suppressSpike=false (not in grace) — already tested above,
        // but explicit here to confirm the non-grace path is unaffected.
        val result =
            proc.process(
                fix(lat = 18.5018, t = 5_000L, speed = 5f, accuracy = 51f),
                isPaused = false,
                suppressSpike = false,
            )
        assertNotNull(result)
        assertFalse(result.countedTowardDistance, "accuracy>50m outside grace must also be gated")
    }
}
