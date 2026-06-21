package com.miletracker.feature.payments.search

import com.miletracker.core.data.search.SearchEntityType
import com.miletracker.core.data.search.SearchFilters
import com.miletracker.core.data.search.SearchProvider
import com.miletracker.core.data.search.SearchResult
import com.miletracker.core.data.search.SearchScope
import com.miletracker.feature.payments.repository.PaymentsRepository

private const val DAY_MS = 86_400_000L

/**
 * PM: the payments module's contribution to master search (F0.5 registry). Searches the payments history and
 * returns flat [SearchResult]s (typed `QR`) with deep links. Visible under VIEW_ALL (payments has no dedicated
 * scope tab). `feature:search` resolves this via `getAll<SearchProvider>()` with zero coupling back here.
 */
class PaymentsSearchProvider(
    private val repository: PaymentsRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> = setOf(SearchEntityType.QR)

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results =
            repository.payments()
                .filter { it.id.contains(q, true) || it.counterparty.contains(q, true) || it.note.contains(q, true) }
                .map {
                    SearchResult(
                        type = SearchEntityType.QR,
                        id = it.id,
                        title = "${it.direction.label} · ${it.counterparty}",
                        subtitle = "${it.note} · ${it.status.label}",
                        status = it.status.label,
                        amount = it.amount,
                        dateEpochDay = it.dateMillis / DAY_MS,
                        deeplink = "miletracker://payments/${it.id}",
                    )
                }

        return if (filters.types.isEmpty()) results else results.filter { it.type in filters.types }
    }
}
