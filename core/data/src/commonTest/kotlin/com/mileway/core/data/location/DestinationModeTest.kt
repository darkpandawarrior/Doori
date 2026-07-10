package com.mileway.core.data.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** PLAN_V24 P11.3 — the head-home countdown/expiry rule, unit-tested once as a pure function. */
class DestinationModeTest {
    @Test
    fun remaining_is_zero_when_no_expiry() {
        assertEquals(0L, destinationRemainingMs(expiresAt = null, now = 1_000L))
    }

    @Test
    fun remaining_counts_down_toward_expiry() {
        assertEquals(30_000L, destinationRemainingMs(expiresAt = 130_000L, now = 100_000L))
    }

    @Test
    fun remaining_floors_at_zero_after_expiry() {
        assertEquals(0L, destinationRemainingMs(expiresAt = 100_000L, now = 250_000L))
    }

    @Test
    fun active_only_before_expiry() {
        assertTrue(isDestinationActive(expiresAt = 200_000L, now = 100_000L))
        assertFalse(isDestinationActive(expiresAt = 100_000L, now = 100_000L), "expiry instant is not active")
        assertFalse(isDestinationActive(expiresAt = 50_000L, now = 100_000L))
        assertFalse(isDestinationActive(expiresAt = null, now = 100_000L))
    }

    @Test
    fun budget_is_thirty_minutes() {
        assertEquals(30 * 60 * 1000L, DESTINATION_BUDGET_MS)
    }

    @Test
    fun region_csv_round_trips() {
        assertEquals(setOf("north", "west"), parseSelectedRegions("north, west"))
        assertEquals(emptySet(), parseSelectedRegions(""))
        assertEquals(emptySet(), parseSelectedRegions("  ,  "))
    }
}
