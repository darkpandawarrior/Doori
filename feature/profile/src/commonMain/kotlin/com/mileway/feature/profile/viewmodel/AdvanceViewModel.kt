package com.mileway.feature.profile.viewmodel

import com.mileway.core.common.UiText
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.profile.model.AdvanceRecord
import com.mileway.feature.profile.model.AdvanceStatus
import com.mileway.feature.profile.model.CardStatus
import com.mileway.feature.profile.model.CorporateCard
import com.mileway.feature.profile.repository.AdvanceRepository

enum class AdvanceTabFilter { ALL, PENDING, SETTLED }

data class AdvanceListData(
    val records: ScreenState<List<AdvanceRecord>> = ScreenState.Loading,
    val activeTab: AdvanceTabFilter = AdvanceTabFilter.ALL,
)

data class AdvanceFormState(
    val step: Int = 1,
    val amountText: String = "",
    val purpose: String = "",
    val requiredByDate: String = "",
    val declarationChecked: Boolean = false,
)

data class AdvanceUiState(
    val list: AdvanceListData = AdvanceListData(),
    val form: AdvanceFormState = AdvanceFormState(),
    val cards: List<CorporateCard> = emptyList(),
    val submitted: Boolean = false,
    val lastSubmittedId: String = "",
    val lastAutoApproved: Boolean = false,
)

sealed interface AdvanceAction {
    data object Refresh : AdvanceAction

    data class SetTab(val tab: AdvanceTabFilter) : AdvanceAction

    data class SetAmount(val text: String) : AdvanceAction

    data class SetPurpose(val text: String) : AdvanceAction

    data class SetRequiredByDate(val date: String) : AdvanceAction

    data class SetDeclaration(val checked: Boolean) : AdvanceAction

    data class GoToStep(val step: Int) : AdvanceAction

    data object SubmitAdvance : AdvanceAction

    data object ResetForm : AdvanceAction

    data class ToggleCardBlock(val cardId: String) : AdvanceAction
}

sealed interface AdvanceEffect {
    data class ShowToast(val message: UiText) : AdvanceEffect
}

class AdvanceViewModel(
    private val repository: AdvanceRepository,
) : BaseViewModel<AdvanceUiState, AdvanceEffect, AdvanceAction>(AdvanceUiState()) {
    init {
        setState {
            copy(
                list = AdvanceListData(records = ScreenState.Content(repository.advanceRecords)),
                cards = repository.cards,
            )
        }
    }

    override fun onAction(action: AdvanceAction) {
        when (action) {
            AdvanceAction.Refresh ->
                setState {
                    copy(
                        list =
                            AdvanceListData(
                                records = ScreenState.Content(repository.advanceRecords),
                                activeTab = list.activeTab,
                            ),
                        cards = repository.cards,
                    )
                }
            is AdvanceAction.SetTab -> setTab(action.tab)
            is AdvanceAction.SetAmount -> setState { copy(form = form.copy(amountText = action.text)) }
            is AdvanceAction.SetPurpose -> setState { copy(form = form.copy(purpose = action.text)) }
            is AdvanceAction.SetRequiredByDate -> setState { copy(form = form.copy(requiredByDate = action.date)) }
            is AdvanceAction.SetDeclaration -> setState { copy(form = form.copy(declarationChecked = action.checked)) }
            is AdvanceAction.GoToStep -> setState { copy(form = form.copy(step = action.step)) }
            AdvanceAction.SubmitAdvance -> submitAdvance()
            AdvanceAction.ResetForm ->
                setState { copy(form = AdvanceFormState(), submitted = false, lastSubmittedId = "", lastAutoApproved = false) }
            is AdvanceAction.ToggleCardBlock -> toggleCardBlock(action.cardId)
        }
    }

    private fun setTab(tab: AdvanceTabFilter) {
        val filtered =
            when (tab) {
                AdvanceTabFilter.ALL -> repository.advanceRecords
                AdvanceTabFilter.PENDING ->
                    repository.advanceRecords.filter {
                        it.status in listOf(AdvanceStatus.PENDING, AdvanceStatus.UNDER_REVIEW, AdvanceStatus.APPROVED)
                    }
                AdvanceTabFilter.SETTLED ->
                    repository.advanceRecords.filter {
                        it.status in listOf(AdvanceStatus.DISBURSED, AdvanceStatus.REJECTED)
                    }
            }
        val recordsState: ScreenState<List<AdvanceRecord>> =
            if (filtered.isEmpty()) ScreenState.Empty else ScreenState.Content(filtered)
        setState { copy(list = AdvanceListData(recordsState, tab)) }
    }

    private fun submitAdvance() {
        val form = currentState.form
        val amount = form.amountText.toDoubleOrNull() ?: 0.0
        val id = "ADV-NEW-${(form.purpose.hashCode() and 0x7FFF_FFFF) % 9000 + 1000}"
        val autoApproved = amount < 10_000.0
        setState { copy(submitted = true, lastSubmittedId = id, lastAutoApproved = autoApproved) }
    }

    private fun toggleCardBlock(cardId: String) {
        setState {
            copy(
                cards =
                    cards.map { card ->
                        if (card.id == cardId) {
                            val newStatus =
                                if (card.status == CardStatus.ACTIVE) CardStatus.BLOCKED else CardStatus.ACTIVE
                            card.copy(status = newStatus)
                        } else {
                            card
                        }
                    },
            )
        }
        emitEffect(AdvanceEffect.ShowToast(UiText.Static("Card ${cardId.takeLast(4)} status updated")))
    }

    fun getCardById(id: String) = repository.getCardById(id)

    fun getTransactionsForCard(cardId: String) = repository.getTransactionsForCard(cardId)
}
