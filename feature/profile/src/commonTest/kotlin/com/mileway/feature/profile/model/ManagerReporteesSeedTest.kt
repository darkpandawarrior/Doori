package com.mileway.feature.profile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V24 P10.6 — the runnable check on the seed math: seeded summaries/trips are deterministic
 * and each reportee's `totalKm` equals the sum of that reportee's seeded trip distances.
 */
class ManagerReporteesSeedTest {
    @Test
    fun `summaries cover all seeded reportees`() {
        assertEquals(SeededReportees.all.size, ManagerReportees.summaries().size)
        assertEquals(4, ManagerReportees.summaries().size)
    }

    @Test
    fun `seed is deterministic`() {
        assertEquals(ManagerReportees.summaries(), ManagerReportees.summaries())
        assertEquals(ManagerReportees.tripsFor("EMP-2101"), ManagerReportees.tripsFor("EMP-2101"))
    }

    @Test
    fun `totalKm equals the sum of that reportee's trip distances`() {
        ManagerReportees.summaries().forEach { summary ->
            val trips = ManagerReportees.tripsFor(summary.reportee.code)
            assertEquals(trips.size, summary.tripCount)
            assertEquals(trips.sumOf { it.distanceKm }, summary.totalKm)
            assertEquals(trips.count { it.status == "Pending" }, summary.pendingApprovals)
        }
    }

    @Test
    fun `unknown code yields no trips`() {
        assertTrue(ManagerReportees.tripsFor("no-digit").isEmpty())
    }
}
