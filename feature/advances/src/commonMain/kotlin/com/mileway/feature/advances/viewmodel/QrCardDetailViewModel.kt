package com.mileway.feature.advances.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.asContent
import com.mileway.feature.advances.data.QrCardsRepository
import com.mileway.feature.advances.model.AdvanceTransaction
import com.mileway.feature.advances.model.QrCard
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class QrCardDetailUiState(
    val card: ScreenState<QrCard> = ScreenState.Loading,
    // "Transactions" tab = the QR wallet's recharge/top-up log (QrCardsRepository has no separate
    // spend-txn feed — recharges are the only ledger a QR wallet exposes today).
    val transactions: ScreenState<List<AdvanceTransaction>> = ScreenState.Loading,
    val tab: AdvanceDetailTab = AdvanceDetailTab.SUMMARY,
    val query: String = "",
    val voucherFilter: VoucherFilter = VoucherFilter.ALL,
) {
    val filteredTransactions: List<AdvanceTransaction>
        get() = (transactions as? ScreenState.Content)?.data?.filterTransactions(voucherFilter, query) ?: emptyList()
}

sealed interface QrCardDetailAction {
    data class Load(val cardId: Long) : QrCardDetailAction

    data class SelectTab(val tab: AdvanceDetailTab) : QrCardDetailAction

    data class SetQuery(val query: String) : QrCardDetailAction

    data class SetVoucherFilter(val filter: VoucherFilter) : QrCardDetailAction
}

sealed interface QrCardDetailEffect

/** PLAN_V35.P4: QR-card detail — card face, low/zero-balance banners, Summary + Transactions tabs. */
class QrCardDetailViewModel(
    private val repository: QrCardsRepository,
) : BaseViewModel<QrCardDetailUiState, QrCardDetailEffect, QrCardDetailAction>(QrCardDetailUiState()) {
    private var loadJob: Job? = null

    override fun onAction(action: QrCardDetailAction) {
        when (action) {
            is QrCardDetailAction.Load -> load(action.cardId)
            is QrCardDetailAction.SelectTab -> setState { copy(tab = action.tab) }
            is QrCardDetailAction.SetQuery -> setState { copy(query = action.query) }
            is QrCardDetailAction.SetVoucherFilter -> setState { copy(voucherFilter = action.filter) }
        }
    }

    private fun load(cardId: Long) {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                launch {
                    combine(repository.activeQrCards(), repository.pastQrCards()) { active, past ->
                        (active + past).find { it.id == cardId }
                    }.collectLatest { card ->
                        setState { copy(card = card?.asContent() ?: ScreenState.Empty) }
                    }
                }
                launch {
                    repository.topUpHistory(cardId.toString()).collectLatest { txns ->
                        setState { copy(transactions = txns.asContent()) }
                    }
                }
            }
    }
}
