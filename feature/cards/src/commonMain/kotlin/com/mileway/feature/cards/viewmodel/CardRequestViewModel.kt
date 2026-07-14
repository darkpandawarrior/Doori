package com.mileway.feature.cards.viewmodel

import com.siddharth.kmp.common.UiText
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.cards.data.CardsMockDataProvider
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.cards.model.CardTypeModel

/** Q.3: multi-step "request a card" flow (select type → reason → confirm → success). */
data class CardRequestUiState(
    val step: Int = 0,
    val cardTypes: List<CardTypeModel> = emptyList(),
    val selectedCardTypeId: Long? = null,
    val reason: String = "",
    val agreeToPolicies: Boolean = false,
    val isSubmitting: Boolean = false,
    val submittedRequestId: Long? = null,
) {
    val totalSteps: Int get() = 4

    val canAdvance: Boolean
        get() =
            when (step) {
                1 -> selectedCardTypeId != null
                2 -> reason.isNotBlank()
                3 -> agreeToPolicies
                else -> true
            }
}

sealed interface CardRequestAction {
    data object Next : CardRequestAction

    data object Back : CardRequestAction

    data class SelectType(val id: Long) : CardRequestAction

    data class SetReason(val text: String) : CardRequestAction

    data class SetAgree(val agree: Boolean) : CardRequestAction

    data object Submit : CardRequestAction
}

sealed interface CardRequestEffect {
    data class ShowToast(val message: UiText) : CardRequestEffect
}

class CardRequestViewModel(
    private val provider: CardsMockDataProvider = CardsMockDataProviderFactory.provider(),
) : BaseViewModel<CardRequestUiState, CardRequestEffect, CardRequestAction>(CardRequestUiState()) {
    init {
        val types = provider.cardTypes()
        setState { copy(cardTypes = types, selectedCardTypeId = types.firstOrNull { it.isDefault }?.id) }
    }

    override fun onAction(action: CardRequestAction) {
        when (action) {
            CardRequestAction.Next -> if (currentState.canAdvance) setState { copy(step = (step + 1).coerceAtMost(totalSteps - 1)) }
            CardRequestAction.Back -> setState { copy(step = (step - 1).coerceAtLeast(0)) }
            is CardRequestAction.SelectType -> setState { copy(selectedCardTypeId = action.id) }
            is CardRequestAction.SetReason -> setState { copy(reason = action.text) }
            is CardRequestAction.SetAgree -> setState { copy(agreeToPolicies = action.agree) }
            CardRequestAction.Submit -> submit()
        }
    }

    private fun submit() {
        if (!currentState.agreeToPolicies || currentState.selectedCardTypeId == null) return
        setState { copy(isSubmitting = false, submittedRequestId = 203L) }
        emitEffect(CardRequestEffect.ShowToast(UiText.Dynamic("Card request submitted")))
    }
}
