package com.mileway.feature.tracking.service.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicIntervalCalculatorTest {
    private fun inputs(
        harshAccel: Boolean = false,
        batteryPct: Int = 100,
        isCharging: Boolean = false,
    ) = IntervalInputs(
        // 8.0 m/s is the city speed band
        speedMps = 8.0,
        batteryPct = batteryPct,
        isCharging = isCharging,
        isPowerSaver = false,
        elapsedMs = 0L,
        harshAccel = harshAccel,
    )

    // ── accelMultiplier / harshAccel wiring ─────────────────────────────────────

    @Test
    fun `harshAccel false reproduces the pre-change interval (regression guard)`() {
        assertEquals(10_000L, DynamicIntervalCalculator.intervalMs(inputs(harshAccel = false)))
    }

    @Test
    fun `harshAccel true shortens the interval`() {
        val normal = DynamicIntervalCalculator.intervalMs(inputs(harshAccel = false))
        val harsh = DynamicIntervalCalculator.intervalMs(inputs(harshAccel = true))
        assertTrue(harsh < normal, "expected harsh-accel interval ($harsh) to be shorter than normal ($normal)")
    }

    // ── charging-zeroed battery multiplier ──────────────────────────────────────

    @Test
    fun `low battery lengthens the interval when not charging`() {
        val full = DynamicIntervalCalculator.intervalMs(inputs(batteryPct = 100, isCharging = false))
        val low = DynamicIntervalCalculator.intervalMs(inputs(batteryPct = 20, isCharging = false))
        assertTrue(low > full, "expected low-battery interval ($low) to be longer than full-battery ($full)")
    }

    @Test
    fun `charging neutralizes the low-battery penalty`() {
        val full = DynamicIntervalCalculator.intervalMs(inputs(batteryPct = 100, isCharging = true))
        val lowButCharging = DynamicIntervalCalculator.intervalMs(inputs(batteryPct = 10, isCharging = true))
        assertEquals(full, lowButCharging)
    }

    // ── P10.1 user-set interval floor ───────────────────────────────────────────

    @Test
    fun `userFloorMs default 0 reproduces the pre-change interval`() {
        assertEquals(10_000L, DynamicIntervalCalculator.intervalMs(inputs().copy(userFloorMs = 0L)))
    }

    @Test
    fun `userFloorMs raises the interval and is never undercut even under harsh accel`() {
        val floored =
            DynamicIntervalCalculator.intervalMs(inputs(harshAccel = true).copy(userFloorMs = 15_000L))
        assertTrue(floored >= 15_000L, "expected interval ($floored) to respect the 15s floor")
    }
}
