package com.mileway

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.payables.repository.PayablesHistoryRepository
import com.mileway.feature.payables.search.PayablesSearchProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/** PB.5 (V17): the payables SearchProvider, scope gating, min-length, multi-family hits, type filter. */
class PayablesSearchProviderTest {

    private fun provider() = PayablesSearchProvider(PayablesHistoryRepository())

    @Test
    fun `returns nothing for a foreign scope`() = runTest {
        val results = provider().search("inv", SearchScope.TRAVEL, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `returns nothing for a one-character query`() = runTest {
        val results = provider().search("I", SearchScope.PAYABLES, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `finds documents by vendor in PAYABLES scope`() = runTest {
        val results = provider().search("Sunrise", SearchScope.PAYABLES, SearchFilters())
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.title.contains("Sunrise", ignoreCase = true) })
    }

    @Test
    fun `type filter restricts to the requested entity types`() = runTest {
        val results =
            provider().search("PO-", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.GIN)))
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == SearchEntityType.GIN })
    }
}
