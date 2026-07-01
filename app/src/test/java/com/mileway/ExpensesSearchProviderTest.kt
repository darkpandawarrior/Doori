package com.mileway

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.logging.repository.CardsTxnHistoryRepository
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.repository.SettlementHistoryRepository
import com.mileway.feature.logging.repository.VoucherHistoryRepository
import com.mileway.feature.logging.search.ExpensesSearchProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/** SP.4 (V17): the Spends SearchProvider, scope gating, min-length, multi-entity hits, type filter. */
class ExpensesSearchProviderTest {

    private fun provider() =
        ExpensesSearchProvider(
            ExpenseRepository(),
            VoucherHistoryRepository(),
            SettlementHistoryRepository(),
            CardsTxnHistoryRepository(),
        )

    @Test
    fun `returns nothing for a foreign scope`() = runTest {
        val results = provider().search("a", SearchScope.TRAVEL, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `returns nothing for a one-character query`() = runTest {
        val results = provider().search("V", SearchScope.EXPENSES, SearchFilters())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `finds vouchers by id in EXPENSES scope`() = runTest {
        val results = provider().search("VCH-1000", SearchScope.EXPENSES, SearchFilters())
        assertTrue(results.any { it.type == SearchEntityType.VOUCHER && it.id == "VCH-1000" })
    }

    @Test
    fun `type filter restricts to the requested entity types`() = runTest {
        val results =
            provider().search("STL", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.SETTLEMENT)))
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == SearchEntityType.SETTLEMENT })
    }
}
