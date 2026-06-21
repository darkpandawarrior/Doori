package com.miletracker.feature.logging.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.logging.repository.VoucherHistoryRepository
import com.miletracker.feature.logging.repository.VoucherStatus
import com.miletracker.feature.logging.ui.model.SubmittedVoucher

/** SP.1 — voucher-history tabs (the first tab is "All"). */
val VOUCHER_HISTORY_TABS: List<VoucherStatus?> = listOf(null) + VoucherStatus.entries

data class VoucherHistoryUiState(
    val tabIndex: Int = 0,
    val query: String = "",
    val list: ScreenState<List<SubmittedVoucher>> = ScreenState.Loading,
)

sealed interface VoucherHistoryAction {
    data object Refresh : VoucherHistoryAction

    data class SelectTab(val index: Int) : VoucherHistoryAction

    data class SetQuery(val query: String) : VoucherHistoryAction
}

sealed interface VoucherHistoryEffect

/**
 * SP.1 — reducer for the voucher history surface. Loads from the offline [VoucherHistoryRepository], filters
 * by the selected status tab + free-text query, and exposes a [ScreenState] the shared `HistoryListScaffold`
 * renders.
 */
class VoucherHistoryViewModel(
    private val repository: VoucherHistoryRepository,
) : BaseViewModel<VoucherHistoryUiState, VoucherHistoryEffect, VoucherHistoryAction>(VoucherHistoryUiState()) {
    init {
        reload()
    }

    override fun onAction(action: VoucherHistoryAction) {
        when (action) {
            VoucherHistoryAction.Refresh -> reload()
            is VoucherHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
            is VoucherHistoryAction.SetQuery -> {
                setState { copy(query = action.query) }
                reload()
            }
        }
    }

    private fun reload() {
        val status = VOUCHER_HISTORY_TABS.getOrNull(currentState.tabIndex)
        val q = currentState.query.trim()
        val rows =
            repository.vouchers(status).filter {
                q.isEmpty() ||
                    it.id.contains(q, ignoreCase = true) ||
                    it.serviceTag.contains(q, ignoreCase = true) ||
                    it.office.contains(q, ignoreCase = true)
            }
        setState { copy(list = ScreenState.Content(rows)) }
    }
}
