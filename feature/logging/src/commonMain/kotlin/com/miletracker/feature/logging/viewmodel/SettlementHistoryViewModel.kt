package com.miletracker.feature.logging.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.logging.repository.SettlementHistoryRepository
import com.miletracker.feature.logging.repository.SettlementRecord
import com.miletracker.feature.logging.repository.SettlementStatus

/** SP.2: settlement-history tabs (first tab is "All"). */
val SETTLEMENT_HISTORY_TABS: List<SettlementStatus?> = listOf(null) + SettlementStatus.entries

data class SettlementHistoryUiState(
    val tabIndex: Int = 0,
    val list: ScreenState<List<SettlementRecord>> = ScreenState.Loading,
)

sealed interface SettlementHistoryAction {
    data object Refresh : SettlementHistoryAction

    data class SelectTab(val index: Int) : SettlementHistoryAction
}

sealed interface SettlementHistoryEffect

/** SP.2: reducer for the settlement history surface (offline fake → [ScreenState] for HistoryListScaffold). */
class SettlementHistoryViewModel(
    private val repository: SettlementHistoryRepository,
) : BaseViewModel<SettlementHistoryUiState, SettlementHistoryEffect, SettlementHistoryAction>(SettlementHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: SettlementHistoryAction) {
        when (action) {
            SettlementHistoryAction.Refresh -> reload()
            is SettlementHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
        }
    }

    private fun reload() {
        val status = SETTLEMENT_HISTORY_TABS.getOrNull(currentState.tabIndex)
        setState { copy(list = ScreenState.Content(repository.settlements(status))) }
    }
}
