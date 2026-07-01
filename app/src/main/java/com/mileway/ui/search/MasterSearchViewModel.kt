package com.mileway.ui.search

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.search.MasterSearchRepository
import com.mileway.core.data.search.SearchEntityType
import com.mileway.core.data.search.SearchFilters
import com.mileway.core.data.search.SearchResult
import com.mileway.core.data.search.SearchScope
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** F0.5: the scope tabs the master-search bar exposes (the first one is unconstrained). */
val SEARCH_SCOPE_TABS: List<SearchScope> =
    listOf(SearchScope.VIEW_ALL, SearchScope.EXPENSES, SearchScope.PAYABLES, SearchScope.TRAVEL)

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
) {
    val scope: SearchScope get() = SEARCH_SCOPE_TABS.getOrElse(scopeIndex) { SearchScope.VIEW_ALL }
}

sealed interface MasterSearchAction {
    data class SetQuery(val query: String) : MasterSearchAction

    data class SelectScope(val index: Int) : MasterSearchAction

    data class ToggleType(val type: SearchEntityType) : MasterSearchAction

    data object ClearFilters : MasterSearchAction

    data object Refresh : MasterSearchAction
}

/** One-shot navigation effect when a result row is tapped (handled by the host nav graph). */
sealed interface MasterSearchEffect {
    data class OpenResult(val result: SearchResult) : MasterSearchEffect
}

/**
 * F0.5: reducer for the cross-feature master-search surface. Debounces the query, fans it out through
 * [MasterSearchRepository] (which fans out to every feature's `SearchProvider`), and groups the merged hits
 * by entity type for display. Filters and scope re-trigger the search immediately; typing is debounced.
 */
@OptIn(FlowPreview::class)
class MasterSearchViewModel(
    private val repository: MasterSearchRepository,
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
                setState { copy(query = action.query) }
                if (action.query.trim().length < MasterSearchRepository.MIN_QUERY_LENGTH) {
                    // Clear immediately rather than waiting on the debounce when the query becomes trivial.
                    setState { copy(results = ScreenState.Content(emptyList())) }
                }
                queryFlow.value = action.query
            }
            is MasterSearchAction.SelectScope -> {
                setState { copy(scopeIndex = action.index) }
                runSearch()
            }
            is MasterSearchAction.ToggleType -> {
                setState {
                    val next = if (action.type in activeTypes) activeTypes - action.type else activeTypes + action.type
                    copy(activeTypes = next)
                }
                runSearch()
            }
            MasterSearchAction.ClearFilters -> {
                setState { copy(activeTypes = emptySet()) }
                runSearch()
            }
            MasterSearchAction.Refresh -> runSearch()
        }
    }

    fun openResult(result: SearchResult) = emitEffect(MasterSearchEffect.OpenResult(result))

    private fun runSearch() {
        val s = currentState
        val q = s.query.trim()
        if (q.length < MasterSearchRepository.MIN_QUERY_LENGTH) {
            setState { copy(results = ScreenState.Content(emptyList())) }
            return
        }
        setState { copy(results = ScreenState.Loading) }
        viewModelScope.launch {
            val hits =
                repository.search(
                    query = q,
                    scope = s.scope,
                    filters = SearchFilters(types = s.activeTypes),
                )
            val grouped =
                hits.groupBy { it.type }
                    .map { (type, results) -> SearchResultGroup(type, results) }
                    .sortedByDescending { it.results.size }
            setState { copy(results = ScreenState.Content(grouped)) }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 250L
    }
}
