package com.mileway.feature.profile.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.profile.repository.AdvanceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** PLAN_V29 P29.S.1: the previously-dead SearchEntityType.ADVANCE provider, now bound in profileModule. */
class AdvanceSearchProviderTest {
    private fun provider() = AdvanceSearchProvider(AdvanceRepository())

    @Test
    fun `returns nothing for a foreign scope`() =
        runTest {
            assertTrue(provider().search("nashik", SearchScope.TRAVEL, SearchFilters()).isEmpty())
        }

    @Test
    fun `returns nothing for a one-character query`() =
        runTest {
            assertTrue(provider().search("n", SearchScope.EXPENSES, SearchFilters()).isEmpty())
        }

    @Test
    fun `finds an advance by id`() =
        runTest {
            val results = provider().search("ADV-001", SearchScope.EXPENSES, SearchFilters())
            assertTrue(results.any { it.type == SearchEntityType.ADVANCE && it.id == "ADV-001" })
        }

    @Test
    fun `finds an advance by purpose text`() =
        runTest {
            val results = provider().search("Nashik", SearchScope.VIEW_ALL, SearchFilters())
            assertTrue(results.any { it.id == "ADV-001" })
        }
}
