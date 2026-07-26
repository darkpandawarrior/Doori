package com.mileway.desktop

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** D.2 smoke test: the desktop dashboard's mock data folds into a sane [SurfaceSnapshot] + trip list. */
class DashboardMockDataTest {
    // 1970-01-11T12:00Z. Two things matter here and both were wrong before:
    //  - MIDDAY, not midnight. This was `10 * 86_400_000L`, i.e. exactly midnight UTC, so the newest
    //    mock trip (now − 2h) landed on the *previous* calendar day. Midday leaves 12h of headroom
    //    either side, so "2 hours ago" is the same day regardless of zone.
    //  - The zone is PINNED at the call site below. SurfaceSnapshotProducer buckets "today" by local
    //    calendar day via TimeZone.currentSystemDefault(), so on the system default this test asserted
    //    the machine's UTC offset rather than the code — green in IST, red on the UTC CI runner, which
    //    is exactly how it failed the first time :desktopApp:desktopTest was wired into the gate.
    private val now = 10 * 86_400_000L + 12 * 3_600_000L

    @Test
    fun `mockSnapshot reports today and week distance from the mock trips`() {
        val snapshot = mockSnapshot(now, timeZone = TimeZone.UTC)

        assertTrue(snapshot.todayTrips >= 1)
        assertEquals(3, snapshot.weekTrips)
        assertTrue(snapshot.weekDistanceKm > 0.0)
    }

    @Test
    fun `mockTripRows returns all mock trips newest-first`() {
        val rows = mockTripRows(now)

        assertEquals(listOf("d1", "d2", "d3"), rows.map { it.token })
    }
}
