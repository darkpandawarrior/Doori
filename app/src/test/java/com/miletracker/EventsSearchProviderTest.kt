package com.miletracker

import com.miletracker.core.data.search.SearchEntityType
import com.miletracker.core.data.search.SearchFilters
import com.miletracker.core.data.search.SearchScope
import com.miletracker.feature.events.repository.EventsRepository
import com.miletracker.feature.events.search.EventsSearchProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/** EV (V17): the events SearchProvider — VIEW_ALL gating, min-length, title hits, type filter. */
class EventsSearchProviderTest {

    private fun provider() = EventsSearchProvider(EventsRepository())

    @Test
    fun `returns nothing outside VIEW_ALL`() = runTest {
        val results = provider().search("town", SearchScope.TRAVEL, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `returns nothing for a one-character query`() = runTest {
        val results = provider().search("t", SearchScope.VIEW_ALL, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `finds events by title in VIEW_ALL`() = runTest {
        val results = provider().search("Town Hall", SearchScope.VIEW_ALL, SearchFilters())
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == SearchEntityType.EVENT })
    }

    @Test
    fun `type filter restricts to EVENT`() = runTest {
        val results =
            provider().search("Sprint", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.EVENT)))
        assertTrue(results.all { it.type == SearchEntityType.EVENT })
    }
}
