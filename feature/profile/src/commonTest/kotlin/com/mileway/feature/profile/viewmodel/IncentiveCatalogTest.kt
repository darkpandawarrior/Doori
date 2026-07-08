package com.mileway.feature.profile.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V24 P6.3: covers [IncentiveCatalog] — the live "weekly trips" progress tracks the completed
 * count (clamped to the target) and the active/expired split is stable.
 */
class IncentiveCatalogTest {
    @Test
    fun `weekly-trips progress reflects the completed count`() {
        val weekly = IncentiveCatalog.build(completedTrips = 2).first { it.id == "weekly_trips" }
        assertEquals(2, weekly.progress)
        assertEquals(IncentiveCatalog.WEEKLY_TRIPS_TARGET, weekly.target)
    }

    @Test
    fun `weekly-trips progress clamps to the target`() {
        val weekly = IncentiveCatalog.build(completedTrips = 99).first { it.id == "weekly_trips" }
        assertEquals(IncentiveCatalog.WEEKLY_TRIPS_TARGET, weekly.progress)
    }

    @Test
    fun `weekly-trips progress never goes negative`() {
        val weekly = IncentiveCatalog.build(completedTrips = -5).first { it.id == "weekly_trips" }
        assertEquals(0, weekly.progress)
    }

    @Test
    fun `catalog has both active and expired programs`() {
        val programs = IncentiveCatalog.build(completedTrips = 0)
        assertTrue(programs.any { !it.expired }, "expected at least one active program")
        assertTrue(programs.any { it.expired }, "expected at least one expired program")
    }
}
