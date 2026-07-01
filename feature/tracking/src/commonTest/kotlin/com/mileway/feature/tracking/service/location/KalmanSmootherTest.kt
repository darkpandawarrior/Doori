package com.mileway.feature.tracking.service.location

import com.mileway.core.data.util.KalmanSmoother
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KalmanSmootherTest {
    // ── KalmanSmoother unit ───────────────────────────────────────────────────

    @Test
    fun `first fix always returns raw coordinates`() {
        val k = KalmanSmoother()
        k.reset()
        val (lat, lng) = k.smooth(18.5000, 73.8, 10f, 0L)
        assertEquals(18.5000, lat, 1e-9)
        assertEquals(73.8, lng, 1e-9)
    }

    @Test
    fun `second fix is pulled toward previous position`() {
        val k = KalmanSmoother()
        k.reset()
        k.smooth(18.5000, 73.8, 10f, 0L)
        val (lat, _) = k.smooth(18.5010, 73.8, 10f, 10_000L)
        assertTrue(lat > 18.5000 && lat < 18.5010, "Kalman must blend two positions, got $lat")
    }

    @Test
    fun `reset clears filter state so next fix is raw again`() {
        val k = KalmanSmoother()
        k.reset()
        k.smooth(18.5000, 73.8, 10f, 0L)
        k.smooth(18.5010, 73.8, 10f, 10_000L)
        // After reset the filter has no prior knowledge: first fix must be exact again.
        k.reset()
        val (lat, lng) = k.smooth(18.6000, 74.0, 15f, 20_000L)
        assertEquals(18.6000, lat, 1e-9)
        assertEquals(74.0, lng, 1e-9)
    }

    @Test
    fun `higher process noise pushes blend closer to measurement`() {
        val lowNoise = KalmanSmoother(processNoiseMetersPerSec = 0.01)
        val highNoise = KalmanSmoother(processNoiseMetersPerSec = 100.0)
        lowNoise.reset()
        highNoise.reset()
        lowNoise.smooth(18.5000, 73.8, 10f, 0L)
        highNoise.smooth(18.5000, 73.8, 10f, 0L)
        val (latLow, _) = lowNoise.smooth(18.5010, 73.8, 10f, 10_000L)
        val (latHigh, _) = highNoise.smooth(18.5010, 73.8, 10f, 10_000L)
        // High process noise → more weight on new measurement → closer to 18.5010.
        assertTrue(latHigh > latLow, "high noise blend=$latHigh must exceed low noise blend=$latLow")
    }

    // ── LocationProcessor integration ─────────────────────────────────────────

    @Test
    fun `LocationProcessor has Kalman enabled by default`() {
        val proc = LocationProcessor()
        // Feed two fixes; with Kalman on, the second is blended so the stored lat is strictly
        // between the two raw values.
        proc.process(GpsFix(lat = 18.5000, lng = 73.8, timeMs = 0L, accuracyM = 10f), isPaused = false)
        val r = proc.process(GpsFix(lat = 18.5010, lng = 73.8, timeMs = 10_000L, accuracyM = 10f), isPaused = false)
        val lat = r!!.location.lat
        assertTrue(lat > 18.5000 && lat < 18.5010, "default-on Kalman must smooth, got $lat")
    }

    @Test
    fun `LocationProcessor with Kalman off persists raw coordinates`() {
        val proc = LocationProcessor(enableKalman = false)
        val r = proc.process(GpsFix(lat = 18.5000, lng = 73.8, timeMs = 0L, accuracyM = 10f), isPaused = false)
        assertEquals(18.5000, r!!.location.lat, 1e-9)
        assertEquals(73.8, r.location.lng, 1e-9)
    }

    @Test
    fun `resetKalman resets filter on enabled processor`() {
        val proc = LocationProcessor()
        val fix1 = GpsFix(lat = 18.5000, lng = 73.8, timeMs = 0L, accuracyM = 10f)
        val fix2 = GpsFix(lat = 18.5010, lng = 73.8, timeMs = 10_000L, accuracyM = 10f)
        val fix3 = GpsFix(lat = 18.6000, lng = 73.8, timeMs = 20_000L, accuracyM = 10f)
        proc.process(fix1, isPaused = false)
        proc.process(fix2, isPaused = false)
        // Reset the filter mid-session to simulate a pause→resume.
        proc.resetKalman()
        // After reset the next fix is treated as a new anchor: raw coordinates returned.
        val r = proc.process(fix3, isPaused = false)
        assertEquals(18.6000, r!!.location.lat, 1e-9, "post-reset first fix must be raw")
    }

    @Test
    fun `resetKalman is a no-op when Kalman is disabled`() {
        val proc = LocationProcessor(enableKalman = false)
        // Should not throw; calling resetKalman on a disabled processor is harmless.
        proc.resetKalman()
        val r = proc.process(GpsFix(lat = 18.5000, lng = 73.8, timeMs = 0L, accuracyM = 10f), isPaused = false)
        assertFalse(r == null)
    }
}
