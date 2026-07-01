package com.mileway.feature.payables.viewmodel

import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.payables.model.PayablesDoc
import com.mileway.feature.payables.model.PayablesDocStatus
import com.mileway.feature.payables.model.PayablesDocType
import com.mileway.feature.payables.repository.PayablesHistoryRepository

/** PB.4: payables-history type tabs (the first tab is "All"). */
val PAYABLES_HISTORY_TABS: List<PayablesDocType?> = listOf(null) + PayablesDocType.entries

data class PayablesHistoryUiState(
    val tabIndex: Int = 0,
    val statusFilter: PayablesDocStatus? = null,
    val query: String = "",
    val list: ScreenState<List<PayablesDoc>> = ScreenState.Loading,
)

sealed interface PayablesHistoryAction {
    data object Refresh : PayablesHistoryAction

    data class SelectTab(val index: Int) : PayablesHistoryAction

    data class SetStatusFilter(val status: PayablesDocStatus?) : PayablesHistoryAction

    data class SetQuery(val query: String) : PayablesHistoryAction
}

sealed interface PayablesHistoryEffect

/**
 * PB.4: reducer for the unified payables history. Loads from the offline [PayablesHistoryRepository], narrows
 * by the selected document-type tab, an optional status filter chip, and a free-text query, and exposes a
 * [ScreenState] the shared `HistoryListScaffold` renders. Covers the Invoice / PR / GIN / Park In-Out / ASN
 * families in one surface.
 */
class PayablesHistoryViewModel(
    private val repository: PayablesHistoryRepository,
) : BaseViewModel<PayablesHistoryUiState, PayablesHistoryEffect, PayablesHistoryAction>(PayablesHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: PayablesHistoryAction) {
        when (action) {
            PayablesHistoryAction.Refresh -> reload()
            is PayablesHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
            is PayablesHistoryAction.SetStatusFilter -> {
                setState { copy(statusFilter = action.status) }
                reload()
            }
            is PayablesHistoryAction.SetQuery -> {
                setState { copy(query = action.query) }
                reload()
            }
        }
    }

    private fun reload() {
        val type = PAYABLES_HISTORY_TABS.getOrNull(currentState.tabIndex)
        val q = currentState.query.trim()
        val rows =
            repository.documents(type = type, status = currentState.statusFilter).filter {
                q.isEmpty() ||
                    it.id.contains(q, ignoreCase = true) ||
                    it.title.contains(q, ignoreCase = true) ||
                    it.reference.contains(q, ignoreCase = true)
            }
        setState { copy(list = ScreenState.Content(rows)) }
    }
}
