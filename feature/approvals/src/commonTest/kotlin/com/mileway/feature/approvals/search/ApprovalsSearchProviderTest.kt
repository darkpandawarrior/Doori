package com.mileway.feature.approvals.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.approvals.repository.FakeClarificationRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V29 P29.S.1: the previously-dead SearchEntityType.APPROVAL/CLARIFICATION providers, now
 * bound in approvalsModule as one class (see [ApprovalsSearchProvider]'s doc for why they're one
 * Koin binding).
 */
class ApprovalsSearchProviderTest {
    private fun provider(repo: FakeClarificationRepository = FakeClarificationRepository()) = ApprovalsSearchProvider(repo)

    @Test
    fun `serves both APPROVAL and CLARIFICATION`() {
        assertEquals(setOf(SearchEntityType.APPROVAL, SearchEntityType.CLARIFICATION), provider().types)
    }

    @Test
    fun `returns nothing for a foreign scope`() =
        runTest {
            assertTrue(provider().search("priya", SearchScope.EXPENSES, SearchFilters()).isEmpty())
        }

    @Test
    fun `returns nothing for a one-character query`() =
        runTest {
            assertTrue(provider().search("p", SearchScope.VIEW_ALL, SearchFilters()).isEmpty())
        }

    @Test
    fun `finds an approval by requester name and by id`() =
        runTest {
            val results = provider().search("priya", SearchScope.VIEW_ALL, SearchFilters())
            assertTrue(results.any { it.type == SearchEntityType.APPROVAL && it.id == "A001" })
            assertTrue(provider().search("A001", SearchScope.VIEW_ALL, SearchFilters()).any { it.id == "A001" })
        }

    @Test
    fun `finds a clarification room by its linked approval's requester name and by approval id`() =
        runTest {
            // A001 belongs to Priya Sharma in ApprovalsRepository's seed data.
            val repo = FakeClarificationRepository().apply { getOrCreateRoom("A001", listOf("approver", "requester")) }
            val results = provider(repo).search("priya", SearchScope.VIEW_ALL, SearchFilters())
            assertTrue(results.any { it.type == SearchEntityType.CLARIFICATION && it.id == "room_A001" })
            assertTrue(provider(repo).search("A001", SearchScope.VIEW_ALL, SearchFilters()).isNotEmpty())
        }

    @Test
    fun `type filter restricts to the requested entity type`() =
        runTest {
            val repo = FakeClarificationRepository().apply { getOrCreateRoom("A001", listOf("approver", "requester")) }
            val results = provider(repo).search("A001", SearchScope.VIEW_ALL, SearchFilters(types = setOf(SearchEntityType.CLARIFICATION)))
            assertTrue(results.isNotEmpty())
            assertTrue(results.all { it.type == SearchEntityType.CLARIFICATION })
        }
}
