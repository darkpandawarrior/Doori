package com.mileway.feature.payments.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.payments.model.PaymentRecord
import com.mileway.feature.payments.model.PaymentStatus
import com.mileway.feature.payments.repository.PaymentsRepository
import com.siddharth.kmp.mvi.BaseViewModel

/** PM: payments-history status tabs (the first tab is "All"). */
val PAYMENTS_HISTORY_TABS: List<PaymentStatus?> = listOf(null) + PaymentStatus.entries

data class PaymentsHistoryUiState(
    val tabIndex: Int = 0,
    val query: String = "",
    val list: ScreenState<List<PaymentRecord>> = ScreenState.Loading,
)

sealed interface PaymentsHistoryAction {
    data object Refresh : PaymentsHistoryAction

    data class SelectTab(val index: Int) : PaymentsHistoryAction

    data class SetQuery(val query: String) : PaymentsHistoryAction
}

sealed interface PaymentsHistoryEffect

/** PM: reducer for the payments-history surface, on the shared `HistoryListScaffold`. */
class PaymentsHistoryViewModel(
    private val repository: PaymentsRepository,
) : BaseViewModel<PaymentsHistoryUiState, PaymentsHistoryEffect, PaymentsHistoryAction>(PaymentsHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: PaymentsHistoryAction) {
        when (action) {
            PaymentsHistoryAction.Refresh -> reload()
            is PaymentsHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
            is PaymentsHistoryAction.SetQuery -> {
                setState { copy(query = action.query) }
                reload()
            }
        }
    }

    private fun reload() {
        val status = PAYMENTS_HISTORY_TABS.getOrNull(currentState.tabIndex)
        val q = currentState.query.trim()
        val rows =
            repository.payments(status).filter {
                q.isEmpty() ||
                    it.id.contains(q, ignoreCase = true) ||
                    it.counterparty.contains(q, ignoreCase = true) ||
                    it.note.contains(q, ignoreCase = true)
            }
        setState { copy(list = ScreenState.Content(rows)) }
    }
}
