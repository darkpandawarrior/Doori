package com.mileway.feature.events.search

import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchProvider
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import com.mileway.feature.events.repository.EventsRepository

private const val DAY_MS = 86_400_000L

/**
 * EV: the events module's contribution to master search (F0.5 registry). Searches the events history and
 * returns flat [SearchResult]s (typed `EVENT`) with deep links. Visible under VIEW_ALL (events has no dedicated
 * scope tab). `feature:search` resolves this via `getAll<SearchProvider>()` with zero coupling back here.
 */
class EventsSearchProvider(
    private val repository: EventsRepository,
) : SearchProvider {
    override val types: Set<SearchEntityType> = setOf(SearchEntityType.EVENT)

    override suspend fun search(
        query: String,
        scope: SearchScope,
        filters: SearchFilters,
    ): List<SearchResult> {
        if (scope != SearchScope.VIEW_ALL) return emptyList()
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results =
            repository.events()
                .filter { it.id.contains(q, true) || it.title.contains(q, true) || it.venue.contains(q, true) }
                .map {
                    SearchResult(
                        type = SearchEntityType.EVENT,
                        id = it.id,
                        title = it.title,
                        subtitle = "${it.venue} · ${it.status.label}",
                        status = it.status.label,
                        dateEpochDay = it.dateMillis / DAY_MS,
                        deeplink = "mileway://events/${it.id}",
                    )
                }

        return if (filters.types.isEmpty()) results else results.filter { it.type in filters.types }
    }
}
