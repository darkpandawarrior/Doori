package com.mileway

import com.mileway.core.data.search.MasterSearchRepository
import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchProvider
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * F0.5: the master-search aggregator: fan-out, de-duplication by (type,id), ordering (exact > recency >
 * title), min-length gating, type-filtered provider skipping, and per-provider failure isolation
 * (PLAN_V29 P29.S.4's [com.mileway.core.data.search.SearchOutcome.failedTypes]).
 */
class MasterSearchRepositoryTest {

    private fun result(
        type: SearchEntityType,
        id: String,
        title: String,
        date: Long = 0L,
    ) = SearchResult(
        type = type,
        id = id,
        title = title,
        subtitle = "sub-$id",
        dateEpochDay = date,
        deeplink = "mileway://x/$id",
    )

    private class FakeProvider(
        override val types: Set<SearchEntityType>,
        private val results: List<SearchResult>,
        private val throwOnSearch: Boolean = false,
    ) : SearchProvider {
        override suspend fun search(
            query: String,
            scope: SearchScope,
            filters: SearchFilters,
        ): List<SearchResult> {
            if (throwOnSearch) error("provider boom")
            return results.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    @Test
    fun `short queries never hit a provider`() = runTest {
        val repo = MasterSearchRepository(listOf(FakeProvider(setOf(SearchEntityType.TRIP), listOf(result(SearchEntityType.TRIP, "1", "Goa trip")))))
        assertTrue(repo.search("g").results.isEmpty())
        assertTrue(repo.search("  ").results.isEmpty())
    }

    @Test
    fun `fans out across providers and merges`() = runTest {
        val repo =
            MasterSearchRepository(
                listOf(
                    FakeProvider(setOf(SearchEntityType.TRIP), listOf(result(SearchEntityType.TRIP, "t1", "Goa trip"))),
                    FakeProvider(setOf(SearchEntityType.QR), listOf(result(SearchEntityType.QR, "p1", "Goa payment"))),
                ),
            )
        val hits = repo.search("goa").results
        assertEquals(2, hits.size)
        assertTrue(hits.any { it.type == SearchEntityType.TRIP })
        assertTrue(hits.any { it.type == SearchEntityType.QR })
    }

    @Test
    fun `de-duplicates identical type and id across providers`() = runTest {
        val shared = result(SearchEntityType.EVENT, "e1", "Summit")
        val repo =
            MasterSearchRepository(
                listOf(
                    FakeProvider(setOf(SearchEntityType.EVENT), listOf(shared)),
                    FakeProvider(setOf(SearchEntityType.EVENT), listOf(shared)),
                ),
            )
        assertEquals(1, repo.search("summit").results.size)
    }

    @Test
    fun `orders exact title match first then by recency`() = runTest {
        val repo =
            MasterSearchRepository(
                listOf(
                    FakeProvider(
                        setOf(SearchEntityType.TRIP),
                        listOf(
                            result(SearchEntityType.TRIP, "old", "Goa weekend", date = 10),
                            result(SearchEntityType.TRIP, "new", "Goa weekend", date = 99),
                            result(SearchEntityType.TRIP, "exact", "Goa", date = 1),
                        ),
                    ),
                ),
            )
        val hits = repo.search("goa").results
        assertEquals("exact", hits[0].id, "exact title match should sort first")
        assertEquals("new", hits[1].id, "newer of the partial matches comes before the older one")
        assertEquals("old", hits[2].id)
    }

    @Test
    fun `type filter skips providers that serve none of the requested types`() = runTest {
        val tripProvider = FakeProvider(setOf(SearchEntityType.TRIP), listOf(result(SearchEntityType.TRIP, "t1", "Goa trip")))
        val qrProvider = FakeProvider(setOf(SearchEntityType.QR), listOf(result(SearchEntityType.QR, "p1", "Goa pay")))
        val repo = MasterSearchRepository(listOf(tripProvider, qrProvider))

        val hits = repo.search("goa", filters = SearchFilters(types = setOf(SearchEntityType.TRIP))).results
        assertEquals(1, hits.size)
        assertEquals(SearchEntityType.TRIP, hits.single().type)
    }

    @Test
    fun `a failing provider does not sink the whole search`() = runTest {
        val repo =
            MasterSearchRepository(
                listOf(
                    FakeProvider(setOf(SearchEntityType.QR), emptyList(), throwOnSearch = true),
                    FakeProvider(setOf(SearchEntityType.TRIP), listOf(result(SearchEntityType.TRIP, "t1", "Goa trip"))),
                ),
            )
        val outcome = repo.search("goa")
        assertEquals(1, outcome.results.size)
        assertEquals(SearchEntityType.TRIP, outcome.results.single().type)
    }

    @Test
    fun `a failing provider's types surface in failedTypes for a retry affordance`() = runTest {
        val repo =
            MasterSearchRepository(
                listOf(
                    FakeProvider(setOf(SearchEntityType.QR), emptyList(), throwOnSearch = true),
                    FakeProvider(setOf(SearchEntityType.TRIP), listOf(result(SearchEntityType.TRIP, "t1", "Goa trip"))),
                ),
            )
        val outcome = repo.search("goa")
        assertEquals(setOf(SearchEntityType.QR), outcome.failedTypes)
    }

    @Test
    fun `a healthy search reports no failed types`() = runTest {
        val repo = MasterSearchRepository(listOf(FakeProvider(setOf(SearchEntityType.TRIP), listOf(result(SearchEntityType.TRIP, "t1", "Goa trip")))))
        assertTrue(repo.search("goa").failedTypes.isEmpty())
    }

    @Test
    fun `availableTypes is the union of every provider's types`() {
        val repo =
            MasterSearchRepository(
                listOf(
                    FakeProvider(setOf(SearchEntityType.TRIP, SearchEntityType.BOOKING), emptyList()),
                    FakeProvider(setOf(SearchEntityType.QR), emptyList()),
                ),
            )
        assertEquals(
            setOf(SearchEntityType.TRIP, SearchEntityType.BOOKING, SearchEntityType.QR),
            repo.availableTypes,
        )
    }
}
