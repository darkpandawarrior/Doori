package com.miletracker

import com.miletracker.feature.tracking.service.location.DynamicIntervalCalculator
import com.miletracker.feature.tracking.service.location.IntervalInputs
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [DynamicIntervalCalculator]. Pure Kotlin, no device needed.
 */
class DynamicIntervalCalculatorTest {

    private fun inputs(
        speed: Double = 11.0,
        battery: Int = 100,
        charging: Boolean = false,
        powerSaver: Boolean = false,
        elapsedMs: Long = 0L,
    ) = IntervalInputs(speed, battery, charging, powerSaver, elapsedMs)

    private fun calc(
        speed: Double = 11.0,
        battery: Int = 100,
        charging: Boolean = false,
        powerSaver: Boolean = false,
        elapsedMs: Long = 0L,
    ) = DynamicIntervalCalculator.intervalMs(inputs(speed, battery, charging, powerSaver, elapsedMs))

    // ── Speed bands ────────────────────────────────────────────────────────────

    @Test
    fun `idle uses the longest base interval`() {
        assertEquals(20_000L, calc(speed = 0.0))
    }

    @Test
    fun `walking speed uses the walk base interval`() {
        assertEquals(15_000L, calc(speed = 2.0))
    }

    @Test
    fun `city speed uses the city base interval`() {
        assertEquals(10_000L, calc(speed = 8.0))
    }

    @Test
    fun `highway speed uses the highway base interval`() {
        assertEquals(6_000L, calc(speed = 20.0))
    }

    @Test
    fun `very fast speed clamps to the fast base interval`() {
        assertEquals(5_000L, calc(speed = 40.0))
    }

    // ── Battery ────────────────────────────────────────────────────────────────

    @Test
    fun `low battery doubles the interval`() {
        // city base 10s × 2.0 = 20s
        assertEquals(20_000L, calc(speed = 8.0, battery = 20))
    }

    @Test
    fun `medium battery scales the interval by 1_5`() {
        // city base 10s × 1.5 = 15s
        assertEquals(15_000L, calc(speed = 8.0, battery = 40))
    }

    @Test
    fun `charging cancels the low-battery penalty`() {
        assertEquals(10_000L, calc(speed = 8.0, battery = 10, charging = true))
    }

    // ── Power saver + duration ───────────────────────────────────────────────────

    @Test
    fun `power saver scales the interval by 1_5`() {
        // city base 10s × 1.5 = 15s
        assertEquals(15_000L, calc(speed = 8.0, powerSaver = true))
    }

    @Test
    fun `long session stretches the interval`() {
        // city base 10s × 1.25 (>2h) = 12.5s
        assertEquals(12_500L, calc(speed = 8.0, elapsedMs = 3 * 3_600_000L))
        // city base 10s × 1.75 (>6h) = 17.5s
        assertEquals(17_500L, calc(speed = 8.0, elapsedMs = 7 * 3_600_000L))
    }

    // ── Clamping + combination ───────────────────────────────────────────────────

    @Test
    fun `result is clamped to the max interval`() {
        // idle 20s × 2.0 (battery) × 1.5 (power-saver) × 1.75 (>6h) = 105s → clamp 60s
        val result = calc(speed = 0.0, battery = 10, powerSaver = true, elapsedMs = 7 * 3_600_000L)
        assertEquals(60_000L, result)
    }

    @Test
    fun `result never drops below the min interval`() {
        // fastest band with no penalties is already at the floor.
        val result = calc(speed = 50.0, battery = 100, charging = true)
        assertTrue(result >= DynamicIntervalCalculator.MIN_INTERVAL_MS, "got $result")
        assertEquals(5_000L, result)
    }
}
