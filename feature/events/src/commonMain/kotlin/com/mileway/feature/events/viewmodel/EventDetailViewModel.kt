package com.mileway.feature.events.viewmodel

import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.events.model.EventCategory
import com.mileway.feature.events.model.EventRecord
import com.mileway.feature.events.model.EventStatus
import com.mileway.feature.events.model.LinkedExpense
import com.mileway.feature.events.repository.EventsRepository
import com.siddharth.kmp.common.UiText
import com.siddharth.kmp.mvi.BaseViewModel

data class EventDetailUiState(
    val eventId: String = "",
    val event: ScreenState<EventRecord> = ScreenState.Loading,
    val showDeleteConfirm: Boolean = false,
    val showEditSheet: Boolean = false,
    val editCategory: EventCategory = EventCategory.OTHER,
    val editBudgetText: String = "",
    val showLinkSheet: Boolean = false,
    val availableToLink: List<LinkedExpense> = emptyList(),
    val selectedToLink: Set<String> = emptySet(),
)

sealed interface EventDetailAction {
    data object Refresh : EventDetailAction

    data object Approve : EventDetailAction

    data object Reject : EventDetailAction

    data object RequestDelete : EventDetailAction

    data object ConfirmDelete : EventDetailAction

    data object DismissDelete : EventDetailAction

    data object OpenEdit : EventDetailAction

    data class SetEditCategory(val category: EventCategory) : EventDetailAction

    data class SetEditBudgetText(val value: String) : EventDetailAction

    data object SaveEdit : EventDetailAction

    data object DismissEdit : EventDetailAction

    data object OpenLinkSheet : EventDetailAction

    data class ToggleLinkSelection(val expenseId: String) : EventDetailAction

    data object ConfirmLink : EventDetailAction

    data object DismissLinkSheet : EventDetailAction

    data object LogExpense : EventDetailAction
}

sealed interface EventDetailEffect {
    data object Deleted : EventDetailEffect

    data class NavigateToExpenseEntry(val context: ExpenseSourceContext) : EventDetailEffect
}

/**
 * MVI reducer for the event-detail screen (V29 P29.E.1), the seam that unblocks E.2/E.3/E.5/E.6/E.7/E.8:
 * capacity/budget variance, mock approve/reject workflow, read-only-historical-field edit, guarded delete,
 * and linked-expense multi-select bulk-link — all against the in-memory [EventsRepository].
 */
class EventDetailViewModel(
    eventId: String,
    private val repository: EventsRepository,
) : BaseViewModel<EventDetailUiState, EventDetailEffect, EventDetailAction>(EventDetailUiState(eventId = eventId)) {
    init {
        load()
    }

    override fun onAction(action: EventDetailAction) {
        when (action) {
            EventDetailAction.Refresh -> load()
            EventDetailAction.Approve -> setStatus(EventStatus.PUBLISHED)
            EventDetailAction.Reject -> setStatus(EventStatus.CANCELLED)
            EventDetailAction.RequestDelete -> setState { copy(showDeleteConfirm = true) }
            EventDetailAction.DismissDelete -> setState { copy(showDeleteConfirm = false) }
            EventDetailAction.ConfirmDelete -> delete()
            EventDetailAction.OpenEdit -> openEdit()
            is EventDetailAction.SetEditCategory -> setState { copy(editCategory = action.category) }
            is EventDetailAction.SetEditBudgetText -> setState { copy(editBudgetText = action.value) }
            EventDetailAction.SaveEdit -> saveEdit()
            EventDetailAction.DismissEdit -> setState { copy(showEditSheet = false) }
            EventDetailAction.OpenLinkSheet -> openLinkSheet()
            is EventDetailAction.ToggleLinkSelection -> toggleLinkSelection(action.expenseId)
            EventDetailAction.ConfirmLink -> confirmLink()
            EventDetailAction.DismissLinkSheet -> setState { copy(showLinkSheet = false, selectedToLink = emptySet()) }
            EventDetailAction.LogExpense -> logExpense()
        }
    }

    private fun load() {
        val record = repository.get(currentState.eventId)
        setState {
            copy(event = record?.let { ScreenState.Content(it) } ?: ScreenState.Error(UiText.Dynamic("Event not found")))
        }
    }

    private fun setStatus(status: EventStatus) {
        repository.setStatus(currentState.eventId, status)
        load()
    }

    private fun delete() {
        val deleted = repository.delete(currentState.eventId)
        setState { copy(showDeleteConfirm = false) }
        if (deleted) emitEffect(EventDetailEffect.Deleted) else load()
    }

    private fun openEdit() {
        val event = (currentState.event as? ScreenState.Content)?.data ?: return
        setState {
            copy(
                showEditSheet = true,
                editCategory = event.category,
                editBudgetText = (event.budgetedAmountMinor / 100.0).toString(),
            )
        }
    }

    private fun saveEdit() {
        val budgetMinor = ((currentState.editBudgetText.toDoubleOrNull() ?: 0.0) * 100).toLong()
        repository.updateEditableFields(currentState.eventId, currentState.editCategory, budgetMinor)
        setState { copy(showEditSheet = false) }
        load()
    }

    private fun openLinkSheet() {
        setState {
            copy(
                showLinkSheet = true,
                availableToLink = repository.availableExpensesToLink(eventId),
                selectedToLink = emptySet(),
            )
        }
    }

    private fun toggleLinkSelection(expenseId: String) {
        setState {
            copy(selectedToLink = if (expenseId in selectedToLink) selectedToLink - expenseId else selectedToLink + expenseId)
        }
    }

    private fun confirmLink() {
        val toLink = currentState.availableToLink.filter { it.id in currentState.selectedToLink }
        if (toLink.isNotEmpty()) repository.linkExpenses(currentState.eventId, toLink)
        setState { copy(showLinkSheet = false, selectedToLink = emptySet()) }
        load()
    }

    private fun logExpense() {
        val event = (currentState.event as? ScreenState.Content)?.data ?: return
        emitEffect(EventDetailEffect.NavigateToExpenseEntry(ExpenseSourceContext.Event(event.id, event.title)))
    }
}
