package com.mileway

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.travel.repository.TravelHistoryRepository
import com.mileway.feature.travel.search.TravelSearchProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/** TR.9 (V17): the travel SearchProvider, scope gating, min-length, trip + booking hits, type filter. */
class TravelSearchProviderTest {

    private fun provider() = TravelSearchProvider(TravelHistoryRepository())

    @Test
    fun `returns nothing for a foreign scope`() = runTest {
        val results = provider().search("trip", SearchScope.EXPENSES, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `returns nothing for a one-character query`() = runTest {
        val results = provider().search("T", SearchScope.TRAVEL, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `finds trips by route in TRAVEL scope`() = runTest {
        val results = provider().search("Bengaluru", SearchScope.TRAVEL, SearchFilters())
        assertTrue(results.any { it.type == SearchEntityType.TRIP })
    }

    @Test
    fun `type filter restricts to bookings`() = runTest {
        val results =
            provider().search("IndiGo", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.BOOKING)))
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == SearchEntityType.BOOKING })
    }
}
