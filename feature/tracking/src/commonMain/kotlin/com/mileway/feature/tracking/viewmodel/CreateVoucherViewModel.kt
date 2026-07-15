package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.VoucherRecord
import com.mileway.feature.tracking.repository.VoucherRepository
import com.siddharth.kmp.mvi.BaseViewModel
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
    val category: VoucherCategory = VoucherCategory.MILEAGE,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val submittedVoucherNumber: String? = null,
    val isLoading: Boolean = true,
    val declarationAcknowledged: Boolean = false,
)

/**
 * P3.5: mirrors a common server-configured declaration content pattern without any network call — a small
 * stub so the confirmation copy is data-driven rather than hardcoded in the composable.
 */
data class VoucherDeclaration(
    val title: String = "Declaration",
    val message: String = "I confirm these expenses are valid and complete.",
)

sealed interface CreateVoucherAction {
    data class ToggleSelection(val token: String) : CreateVoucherAction

    data object SelectAll : CreateVoucherAction

    data object DeselectAll : CreateVoucherAction

    data class SetTitle(val value: String) : CreateVoucherAction

    data class SetCategory(val value: VoucherCategory) : CreateVoucherAction

    data class SetNotes(val value: String) : CreateVoucherAction

    data class GoToStep(val step: Int) : CreateVoucherAction

    data class ToggleDeclaration(val checked: Boolean) : CreateVoucherAction

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
            is CreateVoucherAction.ToggleDeclaration ->
                setState { copy(declarationAcknowledged = action.checked) }
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
                    // P3.3: already-claimed guard — a trip already inside a voucher can't fund
                    // a second one (mirrors a common server-side remaining-voucher-count check).
                    .filter { it.isSubmitted && it.reimbursableAmount > 0 && it.claimedByVoucherNumber == null }
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
        // P3.5: the declaration checkbox is now MVI state, not local Compose `remember` — gate
        // here too, not just on the composable's Button `enabled`, so submit() is a no-op if
        // called without it (e.g. a future non-UI caller, or a test).
        if (!state.declarationAcknowledged) return
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
            // P3.2: a voucher isn't useful sitting in DRAFT forever — move it to PENDING as part
            // of the same submit flow.
            voucherRepository.moveToApproval(number)
            // P3.3: already-claimed guard — stamp every selected trip with this voucher's number
            // so it can't be selected into a second voucher later.
            savedTrackRepository.markClaimedByVoucher(state.selectedTokens.toList(), number)
            setState { copy(isSubmitting = false, submittedVoucherNumber = number, step = 3) }
        }
    }
}
