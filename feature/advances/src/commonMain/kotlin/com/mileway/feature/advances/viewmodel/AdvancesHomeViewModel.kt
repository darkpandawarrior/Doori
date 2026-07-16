package com.mileway.feature.advances.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.asContent
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.data.QrCardsRepository
import com.mileway.feature.advances.model.PettyCard
import com.mileway.feature.advances.model.QrCard
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** PLAN_V35.P4: advances home (tabs: Petty Advances / QR Cards), each an active carousel + past section. */
data class AdvancesHomeUiState(
    val selectedTab: Int = 0,
    val activePettyCards: ScreenState<List<PettyCard>> = ScreenState.Loading,
    val pastPettyCards: ScreenState<List<PettyCard>> = ScreenState.Loading,
    val activeQrCards: ScreenState<List<QrCard>> = ScreenState.Loading,
    val pastQrCards: ScreenState<List<QrCard>> = ScreenState.Loading,
)

sealed interface AdvancesHomeAction {
    data class SelectTab(val index: Int) : AdvancesHomeAction

    data object Refresh : AdvancesHomeAction
}

sealed interface AdvancesHomeEffect

class AdvancesHomeViewModel(
    private val advancesRepository: AdvancesRepository,
    private val qrCardsRepository: QrCardsRepository,
) : BaseViewModel<AdvancesHomeUiState, AdvancesHomeEffect, AdvancesHomeAction>(AdvancesHomeUiState()) {
    init {
        load()
    }

    override fun onAction(action: AdvancesHomeAction) {
        when (action) {
            is AdvancesHomeAction.SelectTab -> setState { copy(selectedTab = action.index) }
            AdvancesHomeAction.Refresh -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            advancesRepository.activePettyCards().collectLatest { cards ->
                setState { copy(activePettyCards = cards.toScreenState()) }
            }
        }
        viewModelScope.launch {
            advancesRepository.pastPettyCards().collectLatest { cards ->
                setState { copy(pastPettyCards = cards.toScreenState()) }
            }
        }
        viewModelScope.launch {
            qrCardsRepository.activeQrCards().collectLatest { cards ->
                setState { copy(activeQrCards = cards.toScreenState()) }
            }
        }
        viewModelScope.launch {
            qrCardsRepository.pastQrCards().collectLatest { cards ->
                setState { copy(pastQrCards = cards.toScreenState()) }
            }
        }
    }
}

/** Loaded-but-empty renders the shared empty state rather than an empty carousel. */
private fun <T> List<T>.toScreenState(): ScreenState<List<T>> = if (isEmpty()) ScreenState.Empty else asContent()
