package com.mileway

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.payments.repository.PaymentsRepository
import com.mileway.feature.payments.search.PaymentsSearchProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/** PM (V17): the payments SearchProvider, VIEW_ALL gating, min-length, counterparty hits, type filter. */
class PaymentsSearchProviderTest {

    private fun provider() = PaymentsSearchProvider(PaymentsRepository())

    @Test
    fun `returns nothing outside VIEW_ALL`() = runTest {
        val results = provider().search("chai", SearchScope.TRAVEL, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `returns nothing for a one-character query`() = runTest {
        val results = provider().search("c", SearchScope.VIEW_ALL, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `finds payments by counterparty in VIEW_ALL`() = runTest {
        val results = provider().search("chai", SearchScope.VIEW_ALL, SearchFilters())
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == SearchEntityType.QR })
    }

    @Test
    fun `type filter restricts to QR`() = runTest {
        val results =
            provider().search("upi", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.QR)))
        assertTrue(results.all { it.type == SearchEntityType.QR })
    }
}
