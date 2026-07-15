package com.mileway.feature.travel.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.travel.model.BookingRequest
import com.mileway.feature.travel.model.BookingType
import com.mileway.feature.travel.model.TravelReqStatus
import com.mileway.feature.travel.repository.TravelHistoryRepository
import com.siddharth.kmp.mvi.BaseViewModel

/** TR.8: booking-history type tabs (the first tab is "All"). */
val BOOKING_HISTORY_TABS: List<BookingType?> = listOf(null) + BookingType.entries

data class BookingHistoryUiState(
    val tabIndex: Int = 0,
    val statusFilter: TravelReqStatus? = null,
    val query: String = "",
    val list: ScreenState<List<BookingRequest>> = ScreenState.Loading,
)

sealed interface BookingHistoryAction {
    data object Refresh : BookingHistoryAction

    data class SelectTab(val index: Int) : BookingHistoryAction

    data class SetStatusFilter(val status: TravelReqStatus?) : BookingHistoryAction

    data class SetQuery(val query: String) : BookingHistoryAction
}

sealed interface BookingHistoryEffect

/**
 * TR.8: reducer for the unified booking-request history (Flight / Bus / Hotel / MJP / Visa). Type tab + status
 * filter chip + query → [ScreenState], on the shared `HistoryListScaffold`.
 */
class BookingHistoryViewModel(
    private val repository: TravelHistoryRepository,
) : BaseViewModel<BookingHistoryUiState, BookingHistoryEffect, BookingHistoryAction>(BookingHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: BookingHistoryAction) {
        when (action) {
            BookingHistoryAction.Refresh -> reload()
            is BookingHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
            is BookingHistoryAction.SetStatusFilter -> {
                setState { copy(statusFilter = action.status) }
                reload()
            }
            is BookingHistoryAction.SetQuery -> {
                setState { copy(query = action.query) }
                reload()
            }
        }
    }

    private fun reload() {
        val type = BOOKING_HISTORY_TABS.getOrNull(currentState.tabIndex)
        val q = currentState.query.trim()
        val rows =
            repository.bookings(type = type, status = currentState.statusFilter).filter {
                q.isEmpty() ||
                    it.id.contains(q, ignoreCase = true) ||
                    it.summary.contains(q, ignoreCase = true)
            }
        setState { copy(list = ScreenState.Content(rows)) }
    }
}
