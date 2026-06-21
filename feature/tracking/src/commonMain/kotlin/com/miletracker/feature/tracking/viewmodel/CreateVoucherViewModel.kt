package com.miletracker.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.miletracker.core.data.model.display.TrackDisplayData
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import com.miletracker.feature.tracking.repository.VoucherRecord
import com.miletracker.feature.tracking.repository.VoucherRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class CreateVoucherUiState(
    val step: Int = 0,
    val expenses: List<TrackDisplayData> = emptyList(),
    val selectedTokens: Set<String> = emptySet(),
    val title: String = "",
    val category: String = "Travel",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val submittedVoucherNumber: String? = null,
    val isLoading: Boolean = true,
)

sealed interface CreateVoucherAction {
    data class ToggleSelection(val token: String) : CreateVoucherAction

    data object SelectAll : CreateVoucherAction

    data object DeselectAll : CreateVoucherAction

    data class SetTitle(val value: String) : CreateVoucherAction

    data class SetCategory(val value: String) : CreateVoucherAction

    data class SetNotes(val value: String) : CreateVoucherAction

    data class GoToStep(val step: Int) : CreateVoucherAction

    data object Submit : CreateVoucherAction
}

sealed interface CreateVoucherEffect

class CreateVoucherViewModel(
    private val savedTrackRepository: SavedTrackRepository,
    private val voucherRepository: VoucherRepository,
) : BaseViewModel<CreateVoucherUiState, CreateVoucherEffect, CreateVoucherAction>(CreateVoucherUiState()) {
    init {
        loadExpenses()
    }

    override fun onAction(action: CreateVoucherAction) {
        when (action) {
            is CreateVoucherAction.ToggleSelection ->
                setState {
                    val selected = selectedTokens.toMutableSet()
                    if (selected.contains(action.token)) selected.remove(action.token) else selected.add(action.token)
                    copy(selectedTokens = selected)
                }
            CreateVoucherAction.SelectAll ->
                setState { copy(selectedTokens = expenses.map { it.token }.toSet()) }
            CreateVoucherAction.DeselectAll ->
                setState { copy(selectedTokens = emptySet()) }
            is CreateVoucherAction.SetTitle ->
                setState { copy(title = action.value) }
            is CreateVoucherAction.SetCategory ->
                setState { copy(category = action.value) }
            is CreateVoucherAction.SetNotes ->
                setState { copy(notes = action.value) }
            is CreateVoucherAction.GoToStep ->
                setState { copy(step = action.step) }
            CreateVoucherAction.Submit -> submit()
        }
    }

    val totalAmount: Double
        get() =
            currentState.expenses
                .filter { currentState.selectedTokens.contains(it.token) }
                .sumOf { it.reimbursableAmount }

    private fun loadExpenses() {
        viewModelScope.launch {
            val tracks =
                savedTrackRepository.completedTracksFlow().first()
                    .filter { it.isSubmitted && it.reimbursableAmount > 0 }
            val defaultTitle =
                run {
                    val ldt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val monthName = ldt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
                    "Voucher: $monthName ${ldt.year}"
                }
            setState { copy(expenses = tracks, title = defaultTitle, isLoading = false) }
        }
    }

    private fun submit() {
        val state = currentState
        setState { copy(isSubmitting = true) }
        viewModelScope.launch {
            val number = "V-${Clock.System.now().toEpochMilliseconds() % 10_000}"
            val total =
                state.expenses
                    .filter { state.selectedTokens.contains(it.token) }
                    .sumOf { it.reimbursableAmount }
            voucherRepository.save(
                VoucherRecord(
                    voucherNumber = number,
                    title = state.title,
                    category = state.category,
                    totalAmount = total,
                    notes = state.notes,
                    expenseRouteIds = state.selectedTokens.toList(),
                    createdAtMs = Clock.System.now().toEpochMilliseconds(),
                ),
            )
            setState { copy(isSubmitting = false, submittedVoucherNumber = number, step = 3) }
        }
    }
}
