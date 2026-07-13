package com.mileway.core.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShakeSpikeTrackerTest {
    @Test
    fun `fires only once enough spikes land inside the window`() {
        val tracker = ShakeSpikeTracker(minSpikes = 3, windowMs = 1_000, cooldownMs = 2_000)
        assertFalse(tracker.onSpike(0))
        assertFalse(tracker.onSpike(200))
        assertTrue(tracker.onSpike(400))
    }

    @Test
    fun `spikes outside the window do not count toward the total`() {
        val tracker = ShakeSpikeTracker(minSpikes = 3, windowMs = 1_000, cooldownMs = 2_000)
        assertFalse(tracker.onSpike(0))
        assertFalse(tracker.onSpike(2_000)) // outside the 1s window from t=0, resets the count
        assertFalse(tracker.onSpike(2_200))
        assertTrue(tracker.onSpike(2_400))
    }

    @Test
    fun `a second shake within the cooldown does not fire again`() {
        val tracker = ShakeSpikeTracker(minSpikes = 3, windowMs = 1_000, cooldownMs = 2_000)
        tracker.onSpike(0)
        tracker.onSpike(100)
        assertTrue(tracker.onSpike(200))
        assertFalse(tracker.onSpike(300))
        assertFalse(tracker.onSpike(400))
        assertFalse(tracker.onSpike(500))
    }

    @Test
    fun `a shake after the cooldown elapses fires again`() {
        val tracker = ShakeSpikeTracker(minSpikes = 3, windowMs = 1_000, cooldownMs = 2_000)
        tracker.onSpike(0)
        tracker.onSpike(100)
        assertTrue(tracker.onSpike(200))
        tracker.onSpike(2_300)
        tracker.onSpike(2_400)
        assertTrue(tracker.onSpike(2_500))
    }
}
