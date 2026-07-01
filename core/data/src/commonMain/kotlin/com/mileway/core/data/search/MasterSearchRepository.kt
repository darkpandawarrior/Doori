package com.mileway.core.data.search

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * F0.5: the master-search aggregator. Holds every feature's [SearchProvider] (resolved by the app graph via
 * Koin's `getAll<SearchProvider>()`, so features never depend on this class) and fans a single query out to
 * all of them concurrently, then merges, de-duplicates and orders the combined result set.
 *
 * Ordering contract (most-useful-first):
 *  1. exact title matches (case-insensitive) before partial matches,
 *  2. then by recency ([SearchResult.dateEpochDay] descending),
 *  3. then by title (stable, case-insensitive) so the order is deterministic for tests and screenshots.
 *
 * De-duplication is by (`type`, `id`): a single entity surfaced by two providers appears once.
 */
class MasterSearchRepository(
    private val providers: List<SearchProvider>,
) {
    /** The entity types any registered provider can return, used by the UI to build its filter set. */
    val availableTypes: Set<SearchEntityType>
        get() = providers.flatMapTo(linkedSetOf()) { it.types }

    /**
     * Run [query] across every provider that can still contribute under [filters] and [scope], then merge.
     * Returns an empty list for blank / too-short queries without touching any provider.
     */
    suspend fun search(
        query: String,
        scope: SearchScope = SearchScope.VIEW_ALL,
        filters: SearchFilters = SearchFilters(),
    ): List<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return emptyList()

        val relevant =
            providers.filter { provider ->
                filters.types.isEmpty() || provider.types.any { it in filters.types }
            }
        if (relevant.isEmpty()) return emptyList()

        val merged =
            coroutineScope {
                relevant
                    .map { provider -> async { runCatching { provider.search(trimmed, scope, filters) }.getOrDefault(emptyList()) } }
                    .awaitAll()
                    .flatten()
            }

        return merged
            .distinctBy { it.type to it.id }
            .sortedWith(resultOrder(trimmed))
    }

    private fun resultOrder(query: String): Comparator<SearchResult> =
        compareByDescending<SearchResult> { it.title.equals(query, ignoreCase = true) }
            .thenByDescending { it.dateEpochDay }
            .thenBy { it.title.lowercase() }

    companion object {
        /** Queries shorter than this never hit a provider (matches each provider's own guard). */
        const val MIN_QUERY_LENGTH = 2
    }
}
