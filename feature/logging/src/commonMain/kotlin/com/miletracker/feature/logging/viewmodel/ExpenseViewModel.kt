package com.miletracker.feature.logging.viewmodel

import com.miletracker.core.common.UiText
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.core.ui.mvi.ScreenState
import com.miletracker.feature.logging.model.ExpenseCategory
import com.miletracker.feature.logging.model.ExpenseRecord
import com.miletracker.feature.logging.model.ExpenseStatus
import com.miletracker.feature.logging.repository.ExpenseRepository

enum class ExpenseFilter { ALL, DRAFTS, PENDING, SETTLED }

/** Sort key for the expense history list (SHEETS.B). */
enum class ExpenseSort { DATE, AMOUNT, MERCHANT }

data class ExpenseListData(
    val records: List<ExpenseRecord> = emptyList(),
    val activeFilter: ExpenseFilter = ExpenseFilter.ALL,
    val activeSort: ExpenseSort = ExpenseSort.DATE,
    /** Category drill-down filter (SHEETS.A) — empty means no category constraint. */
    val selectedCategories: Set<ExpenseCategory> = emptySet(),
)

data class ExpenseFormState(
    val step: Int = 1,
    val category: ExpenseCategory? = null,
    val amountText: String = "",
    val merchantName: String = "",
    val note: String = "",
)

data class ExpenseUiState(
    val listState: ScreenState<ExpenseListData> = ScreenState.Loading,
    val form: ExpenseFormState = ExpenseFormState(),
    val lastSubmittedId: String = "",
    val lastSubmittedAmount: Double = 0.0,
    val detailState: ScreenState<ExpenseRecord> = ScreenState.Empty,
)

sealed interface ExpenseAction {
    data object Refresh : ExpenseAction

    data class SetFilter(val filter: ExpenseFilter) : ExpenseAction

    data class SetSort(val sort: ExpenseSort) : ExpenseAction

    data class SetCategories(val categories: Set<ExpenseCategory>) : ExpenseAction

    data class SelectCategory(val category: ExpenseCategory) : ExpenseAction

    data class SetAmount(val text: String) : ExpenseAction

    data class SetMerchant(val name: String) : ExpenseAction

    data class SetNote(val note: String) : ExpenseAction

    data object SubmitExpense : ExpenseAction

    data object ResetForm : ExpenseAction

    data class OpenDetail(val id: String) : ExpenseAction
}

sealed interface ExpenseEffect {
    data class ShowToast(val message: UiText) : ExpenseEffect

    data class NavigateToSuccess(val id: String) : ExpenseEffect

    data object NavigateBack : ExpenseEffect
}

class ExpenseViewModel(
    private val repository: ExpenseRepository,
) : BaseViewModel<ExpenseUiState, ExpenseEffect, ExpenseAction>(ExpenseUiState()) {
    init {
        refresh(ExpenseFilter.ALL, ExpenseSort.DATE, emptySet())
    }

    override fun onAction(action: ExpenseAction) {
        when (action) {
            ExpenseAction.Refresh ->
                refresh(currentState.listState.activeFilter(), currentState.listState.activeSort(), currentState.listState.activeCategories())
            is ExpenseAction.SetFilter ->
                refresh(action.filter, currentState.listState.activeSort(), currentState.listState.activeCategories())
            is ExpenseAction.SetSort ->
                refresh(currentState.listState.activeFilter(), action.sort, currentState.listState.activeCategories())
            is ExpenseAction.SetCategories ->
                refresh(currentState.listState.activeFilter(), currentState.listState.activeSort(), action.categories)
            is ExpenseAction.SelectCategory ->
                setState { copy(form = form.copy(category = action.category, step = 2)) }
            is ExpenseAction.SetAmount -> setState { copy(form = form.copy(amountText = action.text)) }
            is ExpenseAction.SetMerchant -> setState { copy(form = form.copy(merchantName = action.name)) }
            is ExpenseAction.SetNote -> setState { copy(form = form.copy(note = action.note)) }
            ExpenseAction.SubmitExpense -> submitExpense()
            ExpenseAction.ResetForm ->
                setState { copy(form = ExpenseFormState(), lastSubmittedId = "", lastSubmittedAmount = 0.0) }
            is ExpenseAction.OpenDetail -> openDetail(action.id)
        }
    }

    private fun ScreenState<ExpenseListData>.activeFilter(): ExpenseFilter = (this as? ScreenState.Content)?.data?.activeFilter ?: ExpenseFilter.ALL

    private fun ScreenState<ExpenseListData>.activeSort(): ExpenseSort = (this as? ScreenState.Content)?.data?.activeSort ?: ExpenseSort.DATE

    private fun ScreenState<ExpenseListData>.activeCategories(): Set<ExpenseCategory> = (this as? ScreenState.Content)?.data?.selectedCategories ?: emptySet()

    private fun refresh(
        filter: ExpenseFilter,
        sort: ExpenseSort,
        categories: Set<ExpenseCategory>,
    ) {
        val byStatus =
            when (filter) {
                ExpenseFilter.ALL -> repository.getAll()
                ExpenseFilter.DRAFTS -> repository.filterByStatus(ExpenseStatus.DRAFT)
                ExpenseFilter.PENDING -> repository.filterByStatus(ExpenseStatus.PENDING)
                ExpenseFilter.SETTLED -> repository.filterByStatus(ExpenseStatus.APPROVED)
            }
        val filtered = if (categories.isEmpty()) byStatus else byStatus.filter { it.category in categories }
        val records =
            when (sort) {
                ExpenseSort.DATE -> filtered.sortedByDescending { it.dateMs }
                ExpenseSort.AMOUNT -> filtered.sortedByDescending { it.amountRupees }
                ExpenseSort.MERCHANT -> filtered.sortedBy { it.merchantName.lowercase() }
            }
        setState { copy(listState = ScreenState.Content(ExpenseListData(records, filter, sort, categories))) }
    }

    private fun submitExpense() {
        val form = currentState.form
        val amount = form.amountText.toDoubleOrNull() ?: 0.0
        val id = "EXP-NEW-${(form.merchantName.hashCode() and 0x7FFF_FFFF) % 9000 + 1000}"
        setState { copy(lastSubmittedId = id, lastSubmittedAmount = amount) }
        emitEffect(ExpenseEffect.NavigateToSuccess(id))
    }

    private fun openDetail(id: String) {
        val record = repository.getById(id)
        setState { copy(detailState = record?.let { ScreenState.Content(it) } ?: ScreenState.Empty) }
    }
}
