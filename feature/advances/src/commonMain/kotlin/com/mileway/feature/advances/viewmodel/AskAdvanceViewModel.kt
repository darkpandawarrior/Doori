package com.mileway.feature.advances.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.model.AdvanceType
import com.mileway.feature.advances.validation.PettyRequestError
import com.mileway.feature.advances.validation.PettyRequestValidator
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.launch

/**
 * PLAN_V35.P4: Ask-Advance (petty) request form. [requireDateRange] mirrors the reference app's
 * `enableTimeRangeForPettyCash` plugin flag — the flag itself is wired to `PluginRegistry` when the
 * screen is hooked into Home (a separate task); this ViewModel just needs a boolean, so it's a
 * constructor default for now, swap the call site once that wiring lands.
 */
data class AskAdvanceUiState(
    val types: List<AdvanceType> = emptyList(),
    val selectedType: String? = null,
    val amountText: String = "",
    val title: String = "",
    val description: String = "",
    val requireDateRange: Boolean = false,
    val dateRangeStartMs: Long? = null,
    val dateRangeEndMs: Long? = null,
    val declarationAccepted: Boolean = false,
    val errors: List<PettyRequestError> = emptyList(),
    val isSubmitting: Boolean = false,
    val submittedPermissionId: Long? = null,
) {
    val isSuccess: Boolean get() = (submittedPermissionId ?: 0L) > 0L
}

sealed interface AskAdvanceAction {
    data class SelectType(val type: String) : AskAdvanceAction

    data class SetAmount(val text: String) : AskAdvanceAction

    data class SetTitle(val value: String) : AskAdvanceAction

    data class SetDescription(val value: String) : AskAdvanceAction

    data class SetDateRange(val startMs: Long?, val endMs: Long?) : AskAdvanceAction

    data class SetDeclaration(val accepted: Boolean) : AskAdvanceAction

    data object Submit : AskAdvanceAction
}

sealed interface AskAdvanceEffect

class AskAdvanceViewModel(
    private val repository: AdvancesRepository,
    requireDateRange: Boolean = false,
) : BaseViewModel<AskAdvanceUiState, AskAdvanceEffect, AskAdvanceAction>(
        AskAdvanceUiState(types = repository.pettyTypes(), requireDateRange = requireDateRange),
    ) {
    override fun onAction(action: AskAdvanceAction) {
        when (action) {
            is AskAdvanceAction.SelectType -> setState { copy(selectedType = action.type) }
            is AskAdvanceAction.SetAmount -> setState { copy(amountText = action.text) }
            is AskAdvanceAction.SetTitle -> setState { copy(title = action.value) }
            is AskAdvanceAction.SetDescription -> setState { copy(description = action.value) }
            is AskAdvanceAction.SetDateRange -> setState { copy(dateRangeStartMs = action.startMs, dateRangeEndMs = action.endMs) }
            is AskAdvanceAction.SetDeclaration -> setState { copy(declarationAccepted = action.accepted) }
            AskAdvanceAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (s.isSubmitting) return
        val amount = s.amountText.toDoubleOrNull() ?: 0.0
        val errors =
            PettyRequestValidator.validate(
                type = s.selectedType,
                types = s.types,
                amount = amount,
                title = s.title,
                description = s.description,
                requireDateRange = s.requireDateRange,
                dateRangeStartMs = s.dateRangeStartMs,
                dateRangeEndMs = s.dateRangeEndMs,
                declarationAccepted = s.declarationAccepted,
            )
        setState { copy(errors = errors) }
        if (errors.isNotEmpty()) return

        setState { copy(isSubmitting = true) }
        viewModelScope.launch {
            val result =
                repository.submitPettyRequest(
                    type = s.selectedType,
                    amount = amount,
                    title = s.title.trim(),
                    description = s.description.trim(),
                    startMs = s.dateRangeStartMs,
                    endMs = s.dateRangeEndMs,
                    declarationAccepted = s.declarationAccepted,
                )
            setState {
                copy(
                    isSubmitting = false,
                    submittedPermissionId = result.getOrNull()?.permissionId,
                )
            }
        }
    }
}
