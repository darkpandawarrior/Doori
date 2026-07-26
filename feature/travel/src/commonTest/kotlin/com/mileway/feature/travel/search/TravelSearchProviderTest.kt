package com.mileway.feature.travel.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.travel.repository.TravelHistoryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** TR.9: the travel module's SearchProvider — scope gating, min-length, trip + booking hits, type filter. */
class TravelSearchProviderTest {
    private fun provider() = TravelSearchProvider(TravelHistoryRepository())

    @Test
    fun `serves TRIP and BOOKING`() {
        assertEquals(setOf(SearchEntityType.TRIP, SearchEntityType.BOOKING), provider().types)
    }

    @Test
    fun `returns nothing for a foreign scope`() =
        runTest {
            assertTrue(provider().search("delhi", SearchScope.EXPENSES, SearchFilters()).isEmpty())
        }

    @Test
    fun `returns nothing for a one-character query`() =
        runTest {
            assertTrue(provider().search("d", SearchScope.TRAVEL, SearchFilters()).isEmpty())
        }

    @Test
    fun `finds a trip by purpose and by id`() =
        runTest {
            val results = provider().search("Client visit", SearchScope.TRAVEL, SearchFilters())
            assertTrue(results.any { it.type == SearchEntityType.TRIP && it.id == "TRP-4401" })
            assertTrue(provider().search("TRP-4401", SearchScope.TRAVEL, SearchFilters()).isNotEmpty())
        }

    @Test
    fun `finds a booking by summary and by id`() =
        runTest {
            val results = provider().search("IndiGo", SearchScope.TRAVEL, SearchFilters())
            assertTrue(results.any { it.type == SearchEntityType.BOOKING && it.id == "FLT-5001" })
            assertTrue(provider().search("FLT-5001", SearchScope.TRAVEL, SearchFilters()).isNotEmpty())
        }

    @Test
    fun `type filter restricts to the requested entity type across a trip and booking match`() =
        runTest {
            // "Delhi" hits the TRP-4402 route AND the MJP-5005 booking summary.
            val unfiltered = provider().search("Delhi", SearchScope.VIEW_ALL, SearchFilters())
            assertTrue(unfiltered.any { it.type == SearchEntityType.TRIP })
            assertTrue(unfiltered.any { it.type == SearchEntityType.BOOKING })

            val filtered =
                provider().search("Delhi", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.TRIP)))
            assertTrue(filtered.isNotEmpty())
            assertTrue(filtered.all { it.type == SearchEntityType.TRIP })
        }
}
