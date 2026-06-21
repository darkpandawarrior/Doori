package com.miletracker.feature.events.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.events.model.EventRecord
import com.miletracker.feature.events.model.EventStatus
import com.miletracker.feature.events.repository.EventsRepository

/** EV: events-history status tabs (the first tab is "All"). */
val EVENTS_HISTORY_TABS: List<EventStatus?> = listOf(null) + EventStatus.entries

data class EventsHistoryUiState(
    val tabIndex: Int = 0,
    val query: String = "",
    val list: ScreenState<List<EventRecord>> = ScreenState.Loading,
)

sealed interface EventsHistoryAction {
    data object Refresh : EventsHistoryAction

    data class SelectTab(val index: Int) : EventsHistoryAction

    data class SetQuery(val query: String) : EventsHistoryAction
}

sealed interface EventsHistoryEffect

/** EV: reducer for the events-history surface, on the shared `HistoryListScaffold`. */
class EventsHistoryViewModel(
    private val repository: EventsRepository,
) : BaseViewModel<EventsHistoryUiState, EventsHistoryEffect, EventsHistoryAction>(EventsHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: EventsHistoryAction) {
        when (action) {
            EventsHistoryAction.Refresh -> reload()
            is EventsHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
            is EventsHistoryAction.SetQuery -> {
                setState { copy(query = action.query) }
                reload()
            }
        }
    }

    private fun reload() {
        val status = EVENTS_HISTORY_TABS.getOrNull(currentState.tabIndex)
        val q = currentState.query.trim()
        val rows =
            repository.events(status).filter {
                q.isEmpty() ||
                    it.id.contains(q, ignoreCase = true) ||
                    it.title.contains(q, ignoreCase = true) ||
                    it.venue.contains(q, ignoreCase = true)
            }
        setState { copy(list = ScreenState.Content(rows)) }
    }
}
