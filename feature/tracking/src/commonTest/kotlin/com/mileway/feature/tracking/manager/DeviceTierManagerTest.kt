package com.mileway.feature.tracking.manager

import com.siddharth.kmp.location.DynamicIntervalCalculator
import com.siddharth.kmp.location.IntervalInputs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceTierManagerTest {
    private val gb = 1024L * 1024 * 1024

    // ── tierFor boundaries ──────────────────────────────────────────────────────

    @Test
    fun `just under 3gb is LOW`() {
        assertEquals(DeviceTier.LOW, DeviceTierManager.tierFor(3 * gb - 1))
    }

    @Test
    fun `exactly 3gb is MID`() {
        assertEquals(DeviceTier.MID, DeviceTierManager.tierFor(3 * gb))
    }

    @Test
    fun `just under 6gb is MID`() {
        assertEquals(DeviceTier.MID, DeviceTierManager.tierFor(6 * gb - 1))
    }

    @Test
    fun `exactly 6gb is HIGH`() {
        assertEquals(DeviceTier.HIGH, DeviceTierManager.tierFor(6 * gb))
    }

    @Test
    fun `well above 6gb is HIGH`() {
        assertEquals(DeviceTier.HIGH, DeviceTierManager.tierFor(12 * gb))
    }

    // ── multipliers ──────────────────────────────────────────────────────────────

    @Test
    fun `HIGH tier multipliers are baseline 1_0`() {
        assertEquals(1.0, DeviceTierManager.intervalMultiplier(DeviceTier.HIGH))
        assertEquals(1.0, DeviceTierManager.thresholdMultiplier(DeviceTier.HIGH))
    }

    @Test
    fun `LOW tier multipliers are more conservative than MID`() {
        assertTrue(DeviceTierManager.intervalMultiplier(DeviceTier.LOW) > DeviceTierManager.intervalMultiplier(DeviceTier.MID))
        assertTrue(DeviceTierManager.thresholdMultiplier(DeviceTier.LOW) > DeviceTierManager.thresholdMultiplier(DeviceTier.MID))
    }

    @Test
    fun `MID tier multipliers are more conservative than HIGH`() {
        assertTrue(DeviceTierManager.intervalMultiplier(DeviceTier.MID) > DeviceTierManager.intervalMultiplier(DeviceTier.HIGH))
        assertTrue(DeviceTierManager.thresholdMultiplier(DeviceTier.MID) > DeviceTierManager.thresholdMultiplier(DeviceTier.HIGH))
    }

    // ── wiring into DynamicIntervalCalculator ───────────────────────────────────

    private fun inputs(tierMultiplier: Double) =
        IntervalInputs(
            speedMps = 8.0,
            batteryPct = 100,
            isCharging = false,
            isPowerSaver = false,
            elapsedMs = 0L,
            tierMultiplier = tierMultiplier,
        )

    @Test
    fun `HIGH tier multiplier reproduces the pre-change interval (regression guard)`() {
        val withoutTier = DynamicIntervalCalculator.intervalMs(inputs(tierMultiplier = 1.0))
        val highTierMultiplier = DeviceTierManager.intervalMultiplier(DeviceTier.HIGH)
        val withHighTier = DynamicIntervalCalculator.intervalMs(inputs(tierMultiplier = highTierMultiplier))
        assertEquals(withoutTier, withHighTier)
        assertEquals(10_000L, withHighTier) // city base, no other penalties
    }

    @Test
    fun `LOW tier lengthens the interval`() {
        val highTier = DynamicIntervalCalculator.intervalMs(inputs(tierMultiplier = DeviceTierManager.intervalMultiplier(DeviceTier.HIGH)))
        val lowTier = DynamicIntervalCalculator.intervalMs(inputs(tierMultiplier = DeviceTierManager.intervalMultiplier(DeviceTier.LOW)))
        assertTrue(lowTier > highTier, "expected LOW tier ($lowTier) to be longer than HIGH tier ($highTier)")
    }
}
