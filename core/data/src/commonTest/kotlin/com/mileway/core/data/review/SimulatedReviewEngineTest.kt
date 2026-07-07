package com.mileway.core.data.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * PLAN_V24 P0.5 — the review simulator drives status lifecycles across four features, so its
 * transitions are pinned: pending before the delay, deterministic approve/reject after, reason
 * extraction, and per-call delay override.
 */
class SimulatedReviewEngineTest {
    private class MutableClock(var millis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    @Test
    fun `pending until the sim delay elapses`() {
        val clock = MutableClock(0)
        val engine = SimulatedReviewEngine(clock, simDelayMillis = 5_000)
        assertEquals(ReviewResult.Pending, engine.resolve(submittedAtMillis = 0, payload = "clean"))
        assertFalse(engine.isReviewed(submittedAtMillis = 0))

        clock.millis = 4_999
        assertEquals(ReviewResult.Pending, engine.resolve(submittedAtMillis = 0, payload = "clean"))
    }

    @Test
    fun `approves a clean payload after the delay`() {
        val clock = MutableClock(5_000)
        val engine = SimulatedReviewEngine(clock, simDelayMillis = 5_000)
        assertEquals(ReviewResult.Approved, engine.resolve(submittedAtMillis = 0, payload = "license A123"))
        assertTrue(engine.isReviewed(submittedAtMillis = 0))
    }

    @Test
    fun `rejects a payload carrying the marker, with a default reason`() {
        val clock = MutableClock(10_000)
        val engine = SimulatedReviewEngine(clock, simDelayMillis = 5_000)
        val result = engine.resolve(submittedAtMillis = 0, payload = "please REJECT this")
        assertTrue(result is ReviewResult.Rejected)
        assertEquals(SimulatedReviewEngine.DEFAULT_REJECT_REASON, (result as ReviewResult.Rejected).reason)
    }

    @Test
    fun `extracts an explicit reason after the marker`() {
        val clock = MutableClock(10_000)
        val engine = SimulatedReviewEngine(clock, simDelayMillis = 5_000)
        val result = engine.resolve(submittedAtMillis = 0, payload = "reject: blurry photo")
        assertEquals(ReviewResult.Rejected("blurry photo"), result)
    }

    @Test
    fun `per-call delay override is respected`() {
        val clock = MutableClock(2_000)
        val engine = SimulatedReviewEngine(clock, simDelayMillis = 5_000)
        // Default delay (5s) not yet elapsed at t=2s, but a 1s override has.
        assertEquals(ReviewResult.Pending, engine.resolve(submittedAtMillis = 0, payload = "ok"))
        assertEquals(ReviewResult.Approved, engine.resolve(submittedAtMillis = 0, payload = "ok", delayMillis = 1_000))
    }
}
