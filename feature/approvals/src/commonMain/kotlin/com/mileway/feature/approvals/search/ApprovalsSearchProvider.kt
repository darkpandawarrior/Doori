package com.mileway.feature.approvals.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchProvider
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.approvals.repository.ClarificationRepository
import kotlinx.coroutines.flow.first

private const val DAY_MS = 86_400_000L

/**
 * PLAN_V29 P29.S.1: the approvals module's contribution to master search — 2 of the 5
 * previously-dead providers, [SearchEntityType.APPROVAL] (the in-memory mock approval queue) and
 * [SearchEntityType.CLARIFICATION] (the Room-backed clarification-room store). One class serving
 * both types, not two — Koin's `single<T>` is keyed by type with no qualifier, so a second
 * `single<SearchProvider>` in the same module silently overrides the first instead of adding to it;
 * `getAll<SearchProvider>()` then only sees the last one. Matches every other feature's
 * one-provider-per-module shape (`ExpensesSearchProvider`, `PayablesSearchProvider`, …).
 */
class ApprovalsSearchProvider(
    private val clarifications: ClarificationRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> = setOf(SearchEntityType.APPROVAL, SearchEntityType.CLARIFICATION)

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results = mutableListOf<SearchResult>()

        ApprovalsRepository.all
            .filter { it.id.contains(q, true) || it.requesterName.contains(q, true) || it.summary.contains(q, true) }
            .forEach { item ->
                results +=
                    SearchResult(
                        type = SearchEntityType.APPROVAL,
                        id = item.id,
                        title = item.requesterName,
                        subtitle = "${item.summary} · ${item.status.name.lowercase()}",
                        status = item.status.name,
                        amount = item.amountRupees,
                        dateEpochDay = item.timestampMs / DAY_MS,
                        deeplink = "mileway://approvals/${item.id}",
                    )
            }

        val approvalsById = ApprovalsRepository.all.associateBy { it.id }
        clarifications.observeAllRooms().first()
            .filter { room ->
                val approval = approvalsById[room.approvalId]
                room.approvalId.contains(q, true) ||
                    room.participants.any { it.contains(q, true) } ||
                    approval?.requesterName?.contains(q, true) == true
            }
            .forEach { room ->
                val approval = approvalsById[room.approvalId]
                results +=
                    SearchResult(
                        type = SearchEntityType.CLARIFICATION,
                        id = room.roomId,
                        title = approval?.requesterName ?: room.approvalId,
                        subtitle = "Clarification · ${room.status.name.lowercase()}",
                        status = room.status.name,
                        dateEpochDay = room.updatedAtMs / DAY_MS,
                        deeplink = "mileway://approvals/${room.approvalId}/clarify",
                    )
            }

        return if (filters.types.isEmpty()) results else results.filter { it.type in filters.types }
    }
}
