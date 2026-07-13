package com.mileway.core.data.search

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * PLAN_V29 P29.S.4: [MasterSearchRepository.search]'s result — [failedTypes] is every entity type
 * a *relevant* provider threw for, so the UI can render a per-group "Couldn't load — Retry" row
 * instead of the previous silent empty-list-on-exception (indistinguishable from a real empty
 * result). A provider serving multiple types (e.g. `ExpensesSearchProvider`) reports ALL of its
 * types as failed — the UI can't know which of its types the exception happened partway through.
 */
data class SearchOutcome(
    val results: List<SearchResult>,
    val failedTypes: Set<SearchEntityType> = emptySet(),
)

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
     * Returns an empty outcome for blank / too-short queries without touching any provider.
     */
    suspend fun search(
        query: String,
        scope: SearchScope = SearchScope.VIEW_ALL,
        filters: SearchFilters = SearchFilters(),
    ): SearchOutcome {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return SearchOutcome(emptyList())

        val relevant =
            providers.filter { provider ->
                filters.types.isEmpty() || provider.types.any { it in filters.types }
            }
        if (relevant.isEmpty()) return SearchOutcome(emptyList())

        val outcomes =
            coroutineScope {
                relevant
                    .map { provider -> async { provider to runCatching { provider.search(trimmed, scope, filters) } } }
                    .awaitAll()
            }

        val failedTypes = outcomes.filter { (_, result) -> result.isFailure }.flatMapTo(linkedSetOf()) { (provider, _) -> provider.types }
        val merged = outcomes.flatMap { (_, result) -> result.getOrDefault(emptyList()) }

        return SearchOutcome(
            results = merged.distinctBy { it.type to it.id }.sortedWith(resultOrder(trimmed)),
            failedTypes = failedTypes,
        )
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
