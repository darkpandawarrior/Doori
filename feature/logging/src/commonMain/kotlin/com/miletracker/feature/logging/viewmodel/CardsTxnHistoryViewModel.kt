package com.miletracker.feature.logging.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.logging.repository.CardExpenseTxn
import com.miletracker.feature.logging.repository.CardTxnStatus
import com.miletracker.feature.logging.repository.CardsTxnHistoryRepository

/** SP.3: cards-txn-history tabs (first tab is "All"). */
val CARDS_TXN_HISTORY_TABS: List<CardTxnStatus?> = listOf(null) + CardTxnStatus.entries

data class CardsTxnHistoryUiState(
    val tabIndex: Int = 0,
    val list: ScreenState<List<CardExpenseTxn>> = ScreenState.Loading,
)

sealed interface CardsTxnHistoryAction {
    data object Refresh : CardsTxnHistoryAction

    data class SelectTab(val index: Int) : CardsTxnHistoryAction
}

sealed interface CardsTxnHistoryEffect

/** SP.3: reducer for the cards-expense-transaction history surface. */
class CardsTxnHistoryViewModel(
    private val repository: CardsTxnHistoryRepository,
) : BaseViewModel<CardsTxnHistoryUiState, CardsTxnHistoryEffect, CardsTxnHistoryAction>(CardsTxnHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: CardsTxnHistoryAction) {
        when (action) {
            CardsTxnHistoryAction.Refresh -> reload()
            is CardsTxnHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
        }
    }

    private fun reload() {
        val status = CARDS_TXN_HISTORY_TABS.getOrNull(currentState.tabIndex)
        setState { copy(list = ScreenState.Content(repository.transactions(status))) }
    }
}
