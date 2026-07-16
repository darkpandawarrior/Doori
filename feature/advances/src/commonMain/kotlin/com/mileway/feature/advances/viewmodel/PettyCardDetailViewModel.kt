package com.mileway.feature.advances.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.asContent
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.model.AdvanceTransaction
import com.mileway.feature.advances.model.PettyCard
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class AdvanceDetailTab { SUMMARY, TRANSACTIONS }

data class PettyCardDetailUiState(
    val card: ScreenState<PettyCard> = ScreenState.Loading,
    val transactions: ScreenState<List<AdvanceTransaction>> = ScreenState.Loading,
    val tab: AdvanceDetailTab = AdvanceDetailTab.SUMMARY,
    val query: String = "",
    val voucherFilter: VoucherFilter = VoucherFilter.ALL,
) {
    val filteredTransactions: List<AdvanceTransaction>
        get() = (transactions as? ScreenState.Content)?.data?.filterTransactions(voucherFilter, query) ?: emptyList()
}

sealed interface PettyCardDetailAction {
    data class Load(val cardId: Long) : PettyCardDetailAction

    data class SelectTab(val tab: AdvanceDetailTab) : PettyCardDetailAction

    data class SetQuery(val query: String) : PettyCardDetailAction

    data class SetVoucherFilter(val filter: VoucherFilter) : PettyCardDetailAction
}

sealed interface PettyCardDetailEffect

/** PLAN_V35.P4: petty-card detail — card face, low/zero-balance banners, Summary + Transactions tabs. */
class PettyCardDetailViewModel(
    private val repository: AdvancesRepository,
) : BaseViewModel<PettyCardDetailUiState, PettyCardDetailEffect, PettyCardDetailAction>(PettyCardDetailUiState()) {
    private var loadJob: Job? = null

    override fun onAction(action: PettyCardDetailAction) {
        when (action) {
            is PettyCardDetailAction.Load -> load(action.cardId)
            is PettyCardDetailAction.SelectTab -> setState { copy(tab = action.tab) }
            is PettyCardDetailAction.SetQuery -> setState { copy(query = action.query) }
            is PettyCardDetailAction.SetVoucherFilter -> setState { copy(voucherFilter = action.filter) }
        }
    }

    private fun load(cardId: Long) {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                launch {
                    combine(repository.activePettyCards(), repository.pastPettyCards()) { active, past ->
                        (active + past).find { it.id == cardId }
                    }.collectLatest { card ->
                        setState { copy(card = card?.asContent() ?: ScreenState.Empty) }
                    }
                }
                launch {
                    repository.pettyTransactions(cardId.toString()).collectLatest { txns ->
                        setState { copy(transactions = txns.asContent()) }
                    }
                }
            }
    }
}
