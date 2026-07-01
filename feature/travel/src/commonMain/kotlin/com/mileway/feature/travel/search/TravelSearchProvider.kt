package com.mileway.feature.travel.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchProvider
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.travel.repository.TravelHistoryRepository

private const val DAY_MS = 86_400_000L

/**
 * TR.9: the travel module's contribution to master search (F0.5 registry). Searches submitted trip requests
 * and booking requests; returns flat [SearchResult]s with deep links. `feature:search` resolves this via
 * `getAll<SearchProvider>()` with zero coupling back to this module.
 */
class TravelSearchProvider(
    private val history: TravelHistoryRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> = setOf(SearchEntityType.TRIP, SearchEntityType.BOOKING)

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.TRAVEL && scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results = mutableListOf<SearchResult>()

        history.trips()
            .filter { it.id.contains(q, true) || it.purpose.contains(q, true) || it.route.contains(q, true) }
            .forEach {
                results +=
                    SearchResult(
                        type = SearchEntityType.TRIP,
                        id = it.id,
                        title = it.purpose,
                        subtitle = "${it.route} · ${it.status.label}",
                        status = it.status.label,
                        dateEpochDay = it.dateMillis / DAY_MS,
                        deeplink = "mileway://travel/trip/${it.id}",
                    )
            }

        history.bookings()
            .filter { it.id.contains(q, true) || it.summary.contains(q, true) || it.type.label.contains(q, true) }
            .forEach {
                results +=
                    SearchResult(
                        type = SearchEntityType.BOOKING,
                        id = it.id,
                        title = "${it.type.label} · ${it.summary}",
                        subtitle = it.status.label,
                        status = it.status.label,
                        amount = it.amount,
                        dateEpochDay = it.dateMillis / DAY_MS,
                        deeplink = "mileway://travel/booking/${it.id}",
                    )
            }

        return if (filters.types.isEmpty()) results else results.filter { it.type in filters.types }
    }
}
