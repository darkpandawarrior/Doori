package com.mileway.feature.profile.viewmodel

import com.mileway.core.common.UiText
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.profile.model.AdvanceMode
import com.mileway.feature.profile.model.AdvanceRecord
import com.mileway.feature.profile.model.AdvanceStatus
import com.mileway.feature.profile.model.AdvanceType
import com.mileway.feature.profile.model.CardStatus
import com.mileway.feature.profile.model.CorporateCard
import com.mileway.feature.profile.repository.AdvanceRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class AdvanceTabFilter { ALL, PENDING, SETTLED }

data class AdvanceListData(
    val records: ScreenState<List<AdvanceRecord>> = ScreenState.Loading,
    val activeTab: AdvanceTabFilter = AdvanceTabFilter.ALL,
)

data class AdvanceFormState(
    val step: Int = 0,
    val mode: AdvanceMode = AdvanceMode.CASH,
    val selectedCardId: String? = null,
    val amountText: String = "",
    val purpose: String = "",
    val type: AdvanceType? = null,
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
    val detail: ScreenState<AdvanceRecord> = ScreenState.Loading,
)

sealed interface AdvanceAction {
    data object Refresh : AdvanceAction

    data class SetTab(val tab: AdvanceTabFilter) : AdvanceAction

    data class LoadDetail(val advanceId: String) : AdvanceAction

    data class SetMode(val mode: AdvanceMode) : AdvanceAction

    data class SelectCard(val cardId: String) : AdvanceAction

    data class SetAmount(val text: String) : AdvanceAction

    data class SetPurpose(val text: String) : AdvanceAction

    data class SetType(val type: AdvanceType?) : AdvanceAction

    data class SetRequiredByDate(val date: String) : AdvanceAction

    data class SetDeclaration(val checked: Boolean) : AdvanceAction

    data class GoToStep(val step: Int) : AdvanceAction

    data object SubmitAdvance : AdvanceAction

    data object ResetForm : AdvanceAction

    data class ToggleCardBlock(val cardId: String) : AdvanceAction

    /** "Start Trip Against This Advance" CTA on [com.mileway.feature.profile.ui.screens.AdvanceRequestDetailsScreen]. */
    data class StartTripAgainstAdvance(val advanceId: String) : AdvanceAction

    /**
     * P27.E.8: "Log expense against this advance" CTA on
     * [com.mileway.feature.profile.ui.screens.AdvanceRequestDetailsScreen].
     */
    data class LogExpenseAgainstAdvance(val advanceId: String) : AdvanceAction
}

sealed interface AdvanceEffect {
    data class ShowToast(val message: UiText) : AdvanceEffect

    /**
     * Navigate into `feature/tracking`'s trip-start flow, carrying the advance id so the new
     * trip can be linked back to it. [tripId] is the fresh route id the trip-start flow should
     * use, mirroring how `SavedTracksScreen.onStartNew` mints a new route id at the nav call site.
     */
    data class NavigateToTripStart(val advanceId: String, val tripId: String) : AdvanceEffect

    /**
     * P27.E.8: navigate into the expense-entry flow carrying this advance's
     * [ExpenseSourceContext.Advance]. Built here (not by the nav layer) so feature:profile never
     * depends on feature:logging — mirrors [NavigateToTripStart]'s existing direction.
     */
    data class NavigateToExpenseEntry(val context: ExpenseSourceContext) : AdvanceEffect
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
            is AdvanceAction.LoadDetail -> loadDetail(action.advanceId)
            is AdvanceAction.SetMode -> setMode(action.mode)
            is AdvanceAction.SelectCard -> setState { copy(form = form.copy(selectedCardId = action.cardId)) }
            is AdvanceAction.SetAmount -> setState { copy(form = form.copy(amountText = action.text)) }
            is AdvanceAction.SetPurpose -> setState { copy(form = form.copy(purpose = action.text)) }
            is AdvanceAction.SetType -> setState { copy(form = form.copy(type = action.type)) }
            is AdvanceAction.SetRequiredByDate -> setState { copy(form = form.copy(requiredByDate = action.date)) }
            is AdvanceAction.SetDeclaration -> setState { copy(form = form.copy(declarationChecked = action.checked)) }
            is AdvanceAction.GoToStep -> setState { copy(form = form.copy(step = action.step)) }
            AdvanceAction.SubmitAdvance -> submitAdvance()
            AdvanceAction.ResetForm ->
                setState { copy(form = AdvanceFormState(), submitted = false, lastSubmittedId = "", lastAutoApproved = false) }
            is AdvanceAction.ToggleCardBlock -> toggleCardBlock(action.cardId)
            is AdvanceAction.StartTripAgainstAdvance -> startTripAgainstAdvance(action.advanceId)
            is AdvanceAction.LogExpenseAgainstAdvance -> logExpenseAgainstAdvance(action.advanceId)
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

    private fun loadDetail(advanceId: String) {
        val record = repository.advanceRecords.find { it.id == advanceId }
        val detailState: ScreenState<AdvanceRecord> =
            if (record != null) ScreenState.Content(record) else ScreenState.Empty
        setState { copy(detail = detailState) }
    }

    private fun setMode(mode: AdvanceMode) {
        setState {
            copy(
                form =
                    form.copy(
                        mode = mode,
                        selectedCardId = if (mode == AdvanceMode.CASH) null else form.selectedCardId,
                    ),
            )
        }
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

    @OptIn(ExperimentalUuidApi::class)
    private fun startTripAgainstAdvance(advanceId: String) {
        val tripId = Uuid.random().toString()
        emitEffect(AdvanceEffect.NavigateToTripStart(advanceId = advanceId, tripId = tripId))
    }

    private fun logExpenseAgainstAdvance(advanceId: String) {
        val label = repository.advanceRecords.find { it.id == advanceId }?.purpose
        emitEffect(AdvanceEffect.NavigateToExpenseEntry(ExpenseSourceContext.Advance(advanceId, label)))
    }
}
