package com.miletracker.feature.travel.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.travel.model.TravelReqStatus
import com.miletracker.feature.travel.model.TripRecord
import com.miletracker.feature.travel.repository.TravelHistoryRepository

/** TR.8: trip-history status tabs (the first tab is "All"). */
val TRIP_HISTORY_TABS: List<TravelReqStatus?> = listOf(null) + TravelReqStatus.entries

data class TripHistoryUiState(
    val tabIndex: Int = 0,
    val query: String = "",
    val list: ScreenState<List<TripRecord>> = ScreenState.Loading,
)

sealed interface TripHistoryAction {
    data object Refresh : TripHistoryAction

    data class SelectTab(val index: Int) : TripHistoryAction

    data class SetQuery(val query: String) : TripHistoryAction
}

sealed interface TripHistoryEffect

/** TR.8: reducer for the trip-request history surface, on the shared `HistoryListScaffold`. */
class TripHistoryViewModel(
    private val repository: TravelHistoryRepository,
) : BaseViewModel<TripHistoryUiState, TripHistoryEffect, TripHistoryAction>(TripHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: TripHistoryAction) {
        when (action) {
            TripHistoryAction.Refresh -> reload()
            is TripHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
            is TripHistoryAction.SetQuery -> {
                setState { copy(query = action.query) }
                reload()
            }
        }
    }

    private fun reload() {
        val status = TRIP_HISTORY_TABS.getOrNull(currentState.tabIndex)
        val q = currentState.query.trim()
        val rows =
            repository.trips(status).filter {
                q.isEmpty() ||
                    it.id.contains(q, ignoreCase = true) ||
                    it.purpose.contains(q, ignoreCase = true) ||
                    it.route.contains(q, ignoreCase = true)
            }
        setState { copy(list = ScreenState.Content(rows)) }
    }
}
