package com.mileway.feature.cards.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.asContent
import com.mileway.feature.cards.data.CardsMockDataProvider
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.cards.model.CardModel
import com.mileway.feature.cards.model.CardRequestModel
import com.siddharth.kmp.mvi.BaseViewModel

/** Q.3: Cards home (tabs: cards / requests). */
data class CardsHomeUiState(
    val virtualCards: ScreenState<List<CardModel>> = ScreenState.Loading,
    val physicalCards: ScreenState<List<CardModel>> = ScreenState.Loading,
    val requests: ScreenState<List<CardRequestModel>> = ScreenState.Loading,
    val selectedTab: Int = 0,
)

sealed interface CardsHomeAction {
    data class SelectTab(val index: Int) : CardsHomeAction

    data object Refresh : CardsHomeAction
}

sealed interface CardsHomeEffect

class CardsHomeViewModel(
    private val provider: CardsMockDataProvider = CardsMockDataProviderFactory.provider(),
) : BaseViewModel<CardsHomeUiState, CardsHomeEffect, CardsHomeAction>(CardsHomeUiState()) {
    init {
        load()
    }

    private fun load() {
        val virtual = provider.virtualCards()
        val physical = provider.physicalCards()
        val requests = provider.requests()
        setState {
            copy(
                virtualCards = if (virtual.isEmpty()) ScreenState.Empty else virtual.asContent(),
                physicalCards = if (physical.isEmpty()) ScreenState.Empty else physical.asContent(),
                requests = if (requests.isEmpty()) ScreenState.Empty else requests.asContent(),
            )
        }
    }

    override fun onAction(action: CardsHomeAction) {
        when (action) {
            is CardsHomeAction.SelectTab -> setState { copy(selectedTab = action.index) }
            CardsHomeAction.Refresh -> load()
        }
    }
}
