package com.mileway.feature.payables.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.payables.repository.PayablesHistoryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PB.5: the payables module's SearchProvider — scope gating, min-length, multi-entity hits, type filter. */
class PayablesSearchProviderTest {
    private fun provider() = PayablesSearchProvider(PayablesHistoryRepository())

    @Test
    fun `serves all five payables entity types`() {
        val expected =
            setOf(
                SearchEntityType.INVOICE,
                SearchEntityType.PURCHASE_REQUEST,
                SearchEntityType.GIN,
                SearchEntityType.PARKING,
                SearchEntityType.ASN,
            )
        assertEquals(expected, provider().types)
    }

    @Test
    fun `returns nothing for a foreign scope`() =
        runTest {
            assertTrue(provider().search("apex", SearchScope.TRAVEL, SearchFilters()).isEmpty())
        }

    @Test
    fun `returns nothing for a one-character query`() =
        runTest {
            assertTrue(provider().search("a", SearchScope.PAYABLES, SearchFilters()).isEmpty())
        }

    @Test
    fun `finds a document by id`() =
        runTest {
            val results = provider().search("INV-9000", SearchScope.PAYABLES, SearchFilters())
            assertTrue(results.any { it.type == SearchEntityType.INVOICE && it.id == "INV-9000" })
        }

    @Test
    fun `finds documents sharing a reference across doc families`() =
        runTest {
            // INV-9000 and GIN-9006 both carry reference PO-4821.
            val results = provider().search("PO-4821", SearchScope.VIEW_ALL, SearchFilters())
            assertTrue(results.any { it.id == "INV-9000" })
            assertTrue(results.any { it.id == "GIN-9006" })
        }

    @Test
    fun `type filter restricts to the requested entity type across a multi-type match`() =
        runTest {
            // "Apex Logistics" is the title on an INVOICE, a GIN and an ASN doc.
            val unfiltered = provider().search("Apex Logistics", SearchScope.VIEW_ALL, SearchFilters())
            assertTrue(unfiltered.map { it.type }.toSet().size > 1)

            val filtered =
                provider().search(
                    "Apex Logistics",
                    SearchScope.VIEW_ALL,
                    SearchFilters(types = setOf(SearchEntityType.ASN)),
                )
            assertTrue(filtered.isNotEmpty())
            assertTrue(filtered.all { it.type == SearchEntityType.ASN })
        }
}
