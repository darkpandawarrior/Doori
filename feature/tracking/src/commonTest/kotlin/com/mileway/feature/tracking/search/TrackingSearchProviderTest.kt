package com.mileway.feature.tracking.search

import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.tracking.repository.FakeCheckInLocationDao
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.viewmodel.FakeSavedTrackDao
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V29 P29.S.1: the previously-dead SearchEntityType.MILEAGE/CHECKIN providers, now bound in
 * trackingModule as one class (see [TrackingSearchProvider]'s doc for why they're one Koin binding).
 */
class TrackingSearchProviderTest {
    private fun track(routeId: String) =
        SavedTrack(
            routeId = routeId,
            name = "Nashik field visit",
            startLatitude = 0.0,
            startLongitude = 0.0,
            endLatitude = 0.0,
            endLongitude = 0.0,
            pausedLatitude = 0.0,
            pausedLongitude = 0.0,
            startTime = 1_700_000_000_000L,
            endTime = 1_700_003_600_000L,
            distance = 12_000.0,
            duration = 3_600_000L,
        )

    private fun point(
        id: Long,
        miscellaneous: String,
        wasCheckInPoint: Boolean = true,
    ) = LocationData(
        id = id,
        activity = "STILL",
        speed = 0f,
        lat = 0.0,
        lng = 0.0,
        token = "trip-1",
        date = 100L * id,
        batteryPercentage = 100.0,
        wasCheckInPoint = wasCheckInPoint,
        checkInType = "MANUAL",
        miscellaneous = miscellaneous,
    )

    private fun provider(
        tracks: List<SavedTrack> = emptyList(),
        points: List<LocationData> = emptyList(),
    ): TrackingSearchProvider {
        val locationDao = FakeCheckInLocationDao()
        locationDao.rows += points
        return TrackingSearchProvider(SavedTrackRepository(FakeSavedTrackDao(tracks)), LocationRepository(locationDao))
    }

    @Test
    fun `serves both MILEAGE and CHECKIN`() {
        assertEquals(setOf(SearchEntityType.MILEAGE, SearchEntityType.CHECKIN), provider().types)
    }

    @Test
    fun `returns nothing for a foreign scope`() =
        runTest {
            val results = provider(tracks = listOf(track("route-1"))).search("nashik", SearchScope.TRAVEL, SearchFilters())
            assertTrue(results.isEmpty())
        }

    @Test
    fun `returns nothing for a one-character query`() =
        runTest {
            val results = provider(tracks = listOf(track("route-1"))).search("n", SearchScope.EXPENSES, SearchFilters())
            assertTrue(results.isEmpty())
        }

    @Test
    fun `finds a trip by name in EXPENSES scope`() =
        runTest {
            val results = provider(tracks = listOf(track("route-1"))).search("nashik", SearchScope.EXPENSES, SearchFilters())
            assertEquals(1, results.size)
            assertEquals(SearchEntityType.MILEAGE, results.single().type)
            assertEquals("route-1", results.single().id)
        }

    @Test
    fun `finds a check-in by its miscellaneous label, non check-in rows excluded`() =
        runTest {
            val results =
                provider(points = listOf(point(1, "Whitefield office"), point(2, "Whitefield warehouse", wasCheckInPoint = false)))
                    .search("whitefield", SearchScope.EXPENSES, SearchFilters())
            assertEquals(1, results.size)
            assertEquals(SearchEntityType.CHECKIN, results.single().type)
            assertEquals("1", results.single().id)
        }

    @Test
    fun `type filter restricts to the requested entity type`() =
        runTest {
            val results =
                provider(tracks = listOf(track("route-1")), points = listOf(point(1, "route-1 office")))
                    .search("route-1", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.MILEAGE)))
            assertTrue(results.isNotEmpty())
            assertTrue(results.all { it.type == SearchEntityType.MILEAGE })
        }
}
