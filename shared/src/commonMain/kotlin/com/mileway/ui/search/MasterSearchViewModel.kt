package com.mileway.ui.search

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.search.AppAction
import com.mileway.core.data.search.IdPatternMatcher
import com.mileway.core.data.search.MasterSearchRepository
import com.mileway.core.data.search.QuickActionRegistry
import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import com.mileway.core.ui.mvi.ScreenState
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** F0.5: the scope tabs the master-search bar exposes (the first one is unconstrained). */
val SEARCH_SCOPE_TABS: List<SearchScope> =
    listOf(SearchScope.VIEW_ALL, SearchScope.EXPENSES, SearchScope.PAYABLES, SearchScope.TRAVEL)

private const val DAY_MS = 86_400_000L

/** PLAN_V29 P29.S.3: date-range filter chip presets over [SearchResult.dateEpochDay]. */
enum class DateRangePreset(val labelKey: String) {
    ALL("All time"),
    TODAY("Today"),
    WEEK("Last 7 days"),
    MONTH("Last 30 days"),
    ;

    /** [from, to] inclusive epoch-day bounds, or `null, null` for [ALL] (unconstrained). */
    fun toRange(todayEpochDay: Long): Pair<Long?, Long?> =
        when (this) {
            ALL -> null to null
            TODAY -> todayEpochDay to todayEpochDay
            WEEK -> (todayEpochDay - 6) to todayEpochDay
            MONTH -> (todayEpochDay - 29) to todayEpochDay
        }
}

/** Grouped results for one [SearchEntityType], ready for the UI to render under a section header. */
data class SearchResultGroup(
    val type: SearchEntityType,
    val results: List<SearchResult>,
)

data class MasterSearchUiState(
    val query: String = "",
    val scopeIndex: Int = 0,
    val activeTypes: Set<SearchEntityType> = emptySet(),
    /** Loading once a non-trivial query is in flight; Content (possibly empty) once it resolves. */
    val results: ScreenState<List<SearchResultGroup>> = ScreenState.Content(emptyList()),
    // PLAN_V29 P29.S.2: id-pattern auto-detection — types [query]'s shape matches, for a dismissible
    // "Searching as <Type>" hint. Cleared whenever the query changes past the dismissed one.
    val detectedTypes: Set<SearchEntityType> = emptySet(),
    val dismissedDetectionForQuery: String? = null,
    // PLAN_V29 P29.S.3: status/date-range filter chips — [availableStatuses] is derived from the
    // last unfiltered result set so a chip stays selectable even once it narrows the list.
    val activeStatuses: Set<String> = emptySet(),
    val availableStatuses: Set<String> = emptySet(),
    val dateRangePreset: DateRangePreset = DateRangePreset.ALL,
    // PLAN_V29 P29.S.4: entity types whose provider threw on the last search — drives a per-group
    // "Couldn't load — Retry" row instead of an indistinguishable empty section.
    val failedTypes: Set<SearchEntityType> = emptySet(),
    // PLAN_V29 P29.S.5: command-palette "Quick actions" section, distinct from data-record results.
    val quickActions: List<AppAction> = emptyList(),
) {
    val scope: SearchScope get() = SEARCH_SCOPE_TABS.getOrElse(scopeIndex) { SearchScope.VIEW_ALL }
    val showDetectionHint: Boolean get() = detectedTypes.isNotEmpty() && query.trim() != dismissedDetectionForQuery
}

sealed interface MasterSearchAction {
    data class SetQuery(val query: String) : MasterSearchAction

    data class SelectScope(val index: Int) : MasterSearchAction

    data class ToggleType(val type: SearchEntityType) : MasterSearchAction

    data class ToggleStatus(val status: String) : MasterSearchAction

    data class SelectDateRange(val preset: DateRangePreset) : MasterSearchAction

    data object DismissDetectionHint : MasterSearchAction

    data object ClearFilters : MasterSearchAction

    data object Refresh : MasterSearchAction
}

/** One-shot navigation effects — a tapped result or a tapped quick action. */
sealed interface MasterSearchEffect {
    data class OpenResult(val result: SearchResult) : MasterSearchEffect

    data class OpenAction(val action: AppAction) : MasterSearchEffect
}

/**
 * F0.5: reducer for the cross-feature master-search surface. Debounces the query, fans it out through
 * [MasterSearchRepository] (which fans out to every feature's `SearchProvider`), and groups the merged hits
 * by entity type for display. Filters and scope re-trigger the search immediately; typing is debounced.
 */
@OptIn(FlowPreview::class)
class MasterSearchViewModel(
    private val repository: MasterSearchRepository,
    private val clock: Clock = Clock.System,
) : BaseViewModel<MasterSearchUiState, MasterSearchEffect, MasterSearchAction>(MasterSearchUiState()) {
    /** The full set of types any provider can serve, drives the filter chips. */
    val availableTypes: List<SearchEntityType> = repository.availableTypes.toList()

    private val queryFlow = MutableStateFlow("")

    init {
        queryFlow
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { runSearch() }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: MasterSearchAction) {
        when (action) {
            is MasterSearchAction.SetQuery -> {
                val detected = IdPatternMatcher.detect(action.query)
                setState {
                    copy(
                        query = action.query,
                        detectedTypes = detected,
                        quickActions = quickActionsFor(action.query, scope),
                    )
                }
                if (action.query.trim().length < MasterSearchRepository.MIN_QUERY_LENGTH) {
                    // Clear immediately rather than waiting on the debounce when the query becomes trivial.
                    setState { copy(results = ScreenState.Content(emptyList())) }
                }
                queryFlow.value = action.query
            }
            is MasterSearchAction.SelectScope -> {
                setState { copy(scopeIndex = action.index, quickActions = quickActionsFor(query, SEARCH_SCOPE_TABS.getOrElse(action.index) { scope })) }
                runSearch()
            }
            is MasterSearchAction.ToggleType -> {
                setState {
                    val next = if (action.type in activeTypes) activeTypes - action.type else activeTypes + action.type
                    copy(activeTypes = next)
                }
                runSearch()
            }
            is MasterSearchAction.ToggleStatus -> {
                setState {
                    val next = if (action.status in activeStatuses) activeStatuses - action.status else activeStatuses + action.status
                    copy(activeStatuses = next)
                }
                runSearch()
            }
            is MasterSearchAction.SelectDateRange -> {
                setState { copy(dateRangePreset = action.preset) }
                runSearch()
            }
            MasterSearchAction.DismissDetectionHint -> setState { copy(dismissedDetectionForQuery = query.trim()) }
            MasterSearchAction.ClearFilters -> {
                setState { copy(activeTypes = emptySet(), activeStatuses = emptySet(), dateRangePreset = DateRangePreset.ALL) }
                runSearch()
            }
            MasterSearchAction.Refresh -> runSearch()
        }
    }

    fun openResult(result: SearchResult) = emitEffect(MasterSearchEffect.OpenResult(result))

    fun openAction(action: AppAction) = emitEffect(MasterSearchEffect.OpenAction(action))

    private fun quickActionsFor(
        query: String,
        scope: SearchScope,
    ): List<AppAction> {
        val q = query.trim()
        val scoped = QuickActionRegistry.forScope(scope)
        return if (q.isEmpty()) scoped else scoped.filter { it.label.contains(q, ignoreCase = true) }
    }

    private fun todayEpochDay(): Long = clock.now().toEpochMilliseconds() / DAY_MS

    private fun runSearch() {
        val s = currentState
        val q = s.query.trim()
        if (q.length < MasterSearchRepository.MIN_QUERY_LENGTH) {
            setState { copy(results = ScreenState.Content(emptyList()), failedTypes = emptySet(), availableStatuses = emptySet()) }
            return
        }
        setState { copy(results = ScreenState.Loading) }
        viewModelScope.launch {
            val (dateFrom, dateTo) = s.dateRangePreset.toRange(todayEpochDay())
            val outcome =
                repository.search(
                    query = q,
                    scope = s.scope,
                    filters = SearchFilters(types = s.activeTypes),
                )
            val dateFiltered =
                outcome.results.filter { r ->
                    (dateFrom == null || r.dateEpochDay >= dateFrom) && (dateTo == null || r.dateEpochDay <= dateTo)
                }
            val availableStatuses = dateFiltered.mapNotNull { it.status }.distinct().sorted().toSet()
            val visible = if (s.activeStatuses.isEmpty()) dateFiltered else dateFiltered.filter { it.status in s.activeStatuses }
            val grouped =
                visible.groupBy { it.type }
                    .map { (type, results) -> SearchResultGroup(type, results) }
                    .sortedByDescending { it.results.size }
            setState {
                copy(
                    results = ScreenState.Content(grouped),
                    failedTypes = outcome.failedTypes,
                    availableStatuses = availableStatuses,
                )
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 250L
    }
}
