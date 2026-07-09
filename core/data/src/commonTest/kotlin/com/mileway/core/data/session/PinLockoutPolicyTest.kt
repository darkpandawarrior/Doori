package com.mileway.core.data.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P1.4 — the tiered lockout durations are the security-relevant contract, so they are
 * pinned exactly against the reference app's failed-attempt escalation table.
 */
class PinLockoutPolicyTest {
    @Test
    fun `first four attempts are free`() {
        (1..4).forEach { attempts ->
            assertEquals(0L, PinLockoutPolicy.lockoutMillisFor(attempts), "attempt $attempts should be free")
        }
    }

    @Test
    fun `escalating windows match the reference table`() {
        assertEquals(30_000L, PinLockoutPolicy.lockoutMillisFor(5))
        assertEquals(60_000L, PinLockoutPolicy.lockoutMillisFor(6))
        assertEquals(5 * 60_000L, PinLockoutPolicy.lockoutMillisFor(7))
        assertEquals(15 * 60_000L, PinLockoutPolicy.lockoutMillisFor(8))
        assertEquals(30 * 60_000L, PinLockoutPolicy.lockoutMillisFor(9))
    }

    @Test
    fun `attempts beyond nine stay at the thirty minute ceiling`() {
        assertEquals(30 * 60_000L, PinLockoutPolicy.lockoutMillisFor(12))
        assertEquals(30 * 60_000L, PinLockoutPolicy.lockoutMillisFor(100))
    }

    @Test
    fun `lockout state reports locked and remaining seconds`() {
        val state = PinLockoutState(failedAttempts = 5, lockoutUntilMillis = 30_000)
        assertTrue(state.isLocked(nowMillis = 0))
        assertEquals(30, state.remainingSeconds(nowMillis = 0))
        assertEquals(1, state.remainingSeconds(nowMillis = 29_500))
        assertFalse(state.isLocked(nowMillis = 30_000))
        assertEquals(0, state.remainingSeconds(nowMillis = 40_000))
    }
}
