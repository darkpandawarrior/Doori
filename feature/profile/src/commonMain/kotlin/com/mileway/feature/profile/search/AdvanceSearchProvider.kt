package com.mileway.feature.profile.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchProvider
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.profile.repository.AdvanceRepository

private const val DAY_MS = 86_400_000L

/**
 * PLAN_V29 P29.S.1: the profile module's contribution to master search — the previously-dead
 * [SearchEntityType.ADVANCE] provider, over the in-memory mock advance-request ledger.
 */
class AdvanceSearchProvider(
    private val advances: AdvanceRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> = setOf(SearchEntityType.ADVANCE)

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.EXPENSES && scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        return advances.advanceRecords
            .filter { it.id.contains(q, true) || it.purpose.contains(q, true) }
            .map { record ->
                SearchResult(
                    type = SearchEntityType.ADVANCE,
                    id = record.id,
                    title = record.purpose,
                    subtitle = "${record.id} · ${record.status.name.lowercase()}",
                    status = record.status.name,
                    amount = record.amountRupees,
                    dateEpochDay = record.requestedDateMs / DAY_MS,
                    deeplink = "mileway://profile/advance/${record.id}",
                )
            }
    }
}
