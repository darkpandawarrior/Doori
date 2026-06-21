package com.miletracker

import com.miletracker.feature.tracking.service.location.GpsFix
import com.miletracker.feature.tracking.service.location.LocationProcessor
import com.miletracker.feature.tracking.service.location.TrackStats
import com.miletracker.feature.tracking.service.location.haversineMeters
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the advanced location-tracking pipeline. Pure Kotlin — no device needed.
 */
class LocationProcessorTest {

    private fun fix(lat: Double, lng: Double, t: Long, speed: Float = 11f, mock: Boolean = false) =
        GpsFix(lat = lat, lng = lng, timeMs = t, speedMps = speed, isMock = mock)

    @Test
    fun `haversine matches known distance`() {
        // ~0.001 deg latitude ≈ 111 m.
        val d = haversineMeters(18.5000, 73.8, 18.5010, 73.8)
        assertTrue(d in 105.0..115.0, "expected ~111m, got $d")
    }

    @Test
    fun `normal movement accumulates cleaned distance`() {
        val proc = LocationProcessor()
        val r0 = proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        assertTrue(r0 != null) // first fix is the anchor and is still persisted
        val r1 = proc.process(fix(18.5010, 73.8, 10_000), isPaused = false)
        assertTrue(r1 != null && r1.countedTowardDistance)
        assertTrue(proc.cleanedDistanceM in 105.0..115.0, "cleaned=${proc.cleanedDistanceM}")
        assertEquals(2, proc.totalPoints)
    }

    @Test
    fun `tiny stationary jitter is suppressed`() {
        val proc = LocationProcessor()
        // Parked: speed 0 so there is no movement-history bypass (C.1c).
        proc.process(fix(18.5000, 73.8, 0, speed = 0f), isPaused = false)
        // ~0.9m wander — below the stationary jitter gate.
        val jitter = proc.process(fix(18.500008, 73.8, 10_000, speed = 0f), isPaused = false)
        assertNull(jitter)
        assertEquals(0.0, proc.cleanedDistanceM, 0.001)
        assertEquals(1, proc.totalPoints)
    }

    @Test
    fun `gps spike is flagged abnormal and excluded from cleaned distance`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        proc.process(fix(18.5010, 73.8, 10_000), isPaused = false) // ~111m, counted
        val cleanedBefore = proc.cleanedDistanceM
        // Teleport ~11km in 1s → implausible speed.
        val spike = proc.process(fix(18.6000, 73.8, 11_000), isPaused = false)
        assertTrue(spike != null && spike.isAbnormal)
        assertEquals(cleanedBefore, proc.cleanedDistanceM, 0.001) // cleaned unchanged
        assertTrue(proc.abnormalDistanceM > 1000.0)
    }

    @Test
    fun `mock location is tracked separately from cleaned distance`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        proc.process(fix(18.5010, 73.8, 10_000), isPaused = false)
        val cleanedBefore = proc.cleanedDistanceM
        val mock = proc.process(fix(18.5020, 73.8, 20_000, mock = true), isPaused = false)
        assertTrue(mock != null && mock.isMock)
        assertEquals(cleanedBefore, proc.cleanedDistanceM, 0.001)
        assertTrue(proc.mockDistanceM > 100.0)
    }

    @Test
    fun `seeded processor reports persisted totals before any new fix`() {
        val proc = LocationProcessor(
            initialStats = TrackStats(
                totalPoints = 40,
                originalDistanceM = 5_000.0,
                cleanedDistanceM = 5_000.0,
                abnormalDistanceM = 0.0,
                mockDistanceM = 0.0,
                avgSpeedMps = 10.0,
                maxSpeedMps = 18.0
            )
        )
        assertEquals(5_000.0, proc.cleanedDistanceM, 0.001)
        assertEquals(40, proc.totalPoints)
        assertEquals(10.0, proc.avgSpeedMps, 0.001)
        assertEquals(18.0, proc.maxSpeedMps, 0.001)
    }

    @Test
    fun `seeded processor accumulates new movement on top of persisted distance`() {
        val proc = LocationProcessor(
            initialStats = TrackStats(
                totalPoints = 40,
                originalDistanceM = 5_000.0,
                cleanedDistanceM = 5_000.0,
                abnormalDistanceM = 0.0,
                mockDistanceM = 0.0,
                avgSpeedMps = 10.0,
                maxSpeedMps = 18.0
            )
        )
        // First fix after a restore is only an anchor: the gap travelled while the service
        // was dead must not count as distance, even if it is kilometres from the last point.
        val anchor = proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        assertTrue(anchor != null)
        assertEquals(5_000.0, proc.cleanedDistanceM, 0.001)

        // Movement from the anchor accumulates on top of the seeded total (~111m).
        proc.process(fix(18.5010, 73.8, 10_000), isPaused = false)
        assertTrue(proc.cleanedDistanceM in 5_105.0..5_115.0, "cleaned=${proc.cleanedDistanceM}")
        assertEquals(42, proc.totalPoints)
    }

    @Test
    fun `paused points do not add to cleaned distance`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        val paused = proc.process(fix(18.5010, 73.8, 10_000), isPaused = true)
        assertTrue(paused != null)
        assertFalse(paused.countedTowardDistance)
        assertEquals(0.0, proc.cleanedDistanceM, 0.001)
        assertTrue(proc.originalDistanceM > 100.0) // still counted in the raw figure
    }

    // ── C.1a — speed-adaptive jitter gate ────────────────────────────────────────

    @Test
    fun `walking-speed step above the small gate counts`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0, speed = 0f), isPaused = false) // parked anchor, no momentum
        // ~4m step at walking speed (gate 2m) → counts.
        val r = proc.process(fix(18.500036, 73.8, 5_000, speed = 2f), isPaused = false)
        assertTrue(r != null && r.countedTowardDistance, "walking step should count")
    }

    @Test
    fun `sub-gate step without momentum is suppressed`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0, speed = 0f), isPaused = false)
        // ~1m step at walking speed (gate 2m), no movement history → suppressed.
        val r = proc.process(fix(18.500009, 73.8, 5_000, speed = 2f), isPaused = false)
        assertNull(r)
    }

    // ── C.1c — movement-history bypass ───────────────────────────────────────────

    @Test
    fun `small step is kept through when recent history shows movement`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0, speed = 5f), isPaused = false)
        proc.process(fix(18.5010, 73.8, 10_000, speed = 5f), isPaused = false) // builds momentum
        // ~1m step below the cycling gate (3m) but momentum is established → kept.
        val r = proc.process(fix(18.501009, 73.8, 20_000, speed = 5f), isPaused = false)
        assertTrue(r != null && r.countedTowardDistance, "momentum should bypass the jitter gate")
    }

    // ── C.1b — 5 km hard teleport gate + consecutive-normal counter ───────────────

    @Test
    fun `five km teleport is abnormal even below the speed cap`() {
        // High speed cap so only the hard 5km gate can flag the jump.
        val proc = LocationProcessor(maxPlausibleSpeedMps = 10_000.0)
        proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        // ~6km jump in 20s → implied 300 m/s (< cap) but > 5km hard gate.
        val r = proc.process(fix(18.5540, 73.8, 20_000), isPaused = false)
        assertTrue(r != null && r.isAbnormal, "6km teleport should be abnormal")
        assertTrue(proc.spikeDistanceM > 5_000.0, "spikeDistanceM=${proc.spikeDistanceM}")
    }

    @Test
    fun `consecutive-normal count resets on a spike and recovers`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        proc.process(fix(18.5010, 73.8, 10_000), isPaused = false) // normal
        proc.process(fix(18.5020, 73.8, 20_000), isPaused = false) // normal → count 2
        assertEquals(2, proc.consecutiveNormalCount)
        proc.process(fix(18.6020, 73.8, 21_000), isPaused = false) // ~11km/1s spike
        assertEquals(0, proc.consecutiveNormalCount)
        proc.process(fix(18.6030, 73.8, 31_000), isPaused = false)
        proc.process(fix(18.6040, 73.8, 41_000), isPaused = false)
        proc.process(fix(18.6050, 73.8, 51_000), isPaused = false)
        assertEquals(3, proc.consecutiveNormalCount)
    }

    // ── C.1d — gap-recovery tiers ────────────────────────────────────────────────

    @Test
    fun `plausible jump after a 10 minute gap is counted`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        // ~40km over 10min → implied ~67 m/s, within the 5m–1h tier (≤100) → counted.
        val r = proc.process(fix(18.8600, 73.8, 600_000), isPaused = false)
        assertTrue(r != null && !r.isAbnormal, "gap-tier jump should not be abnormal")
        assertTrue(proc.cleanedDistanceM > 39_000.0, "cleaned=${proc.cleanedDistanceM}")
    }

    @Test
    fun `implausible jump within a short gap is abnormal`() {
        val proc = LocationProcessor()
        proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        // ~40km over 1min → implied ~667 m/s, above the 30s–5m tier cap (150) → abnormal.
        val r = proc.process(fix(18.8600, 73.8, 60_000), isPaused = false)
        assertTrue(r != null && r.isAbnormal, "667 m/s over a 1min gap should be abnormal")
    }

    // ── C.1g — optional Kalman smoothing ─────────────────────────────────────────

    @Test
    fun `kalman disabled persists raw coordinates`() {
        val proc = LocationProcessor() // enableKalman defaults to false
        val r = proc.process(fix(18.5000, 73.8, 0), isPaused = false)
        assertEquals(18.5000, r!!.location.lat, 1e-9)
        assertEquals(73.8, r.location.lng, 1e-9)
    }

    @Test
    fun `kalman first fix passes through unchanged`() {
        val proc = LocationProcessor(enableKalman = true)
        val r = proc.process(GpsFix(lat = 18.5000, lng = 73.8, timeMs = 0, accuracyM = 10f), isPaused = false)
        assertEquals(18.5000, r!!.location.lat, 1e-9)
    }

    @Test
    fun `kalman pulls a second fix toward the previous position`() {
        val proc = LocationProcessor(enableKalman = true)
        proc.process(GpsFix(lat = 18.5000, lng = 73.8, timeMs = 0, accuracyM = 10f), isPaused = false)
        val r = proc.process(GpsFix(lat = 18.5010, lng = 73.8, timeMs = 10_000, accuracyM = 10f), isPaused = false)
        val smoothedLat = r!!.location.lat
        // Smoothed toward the prior anchor: strictly between the two raw latitudes.
        assertTrue(smoothedLat > 18.5000 && smoothedLat < 18.5010, "smoothed=$smoothedLat")
    }
}
