package com.miletracker.feature.payables.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.payables.repository.GinDraft
import com.miletracker.feature.payables.repository.GinRepository
import com.miletracker.feature.payables.repository.PayablesSubmissionResult

data class CreateGinUiState(
    val ginNumber: String = "",
    val poReference: String = "",
    val vendor: String = "",
    val warehouse: String = "",
    val receivedQtyText: String = "",
    val remarks: String = "",
    val isSubmitting: Boolean = false,
    val lastResult: PayablesSubmissionResult? = null,
) {
    val receivedQty: Int? get() = receivedQtyText.toIntOrNull()

    /** Submit is gated on the GIN number, a PO reference, and a positive received quantity (PB.2 validation). */
    val canSubmit: Boolean
        get() = ginNumber.isNotBlank() && poReference.isNotBlank() && (receivedQty ?: 0) > 0
}

sealed interface CreateGinAction {
    data class SetGinNumber(val value: String) : CreateGinAction

    data class SetPoReference(val value: String) : CreateGinAction

    data class SetVendor(val value: String) : CreateGinAction

    data class SetWarehouse(val value: String) : CreateGinAction

    data class SetReceivedQty(val value: String) : CreateGinAction

    data class SetRemarks(val value: String) : CreateGinAction

    data object Submit : CreateGinAction
}

sealed interface CreateGinEffect {
    data class Success(val id: String) : CreateGinEffect

    data class NeedsApproval(val id: String) : CreateGinEffect

    data class Violation(val messages: List<String>) : CreateGinEffect
}

/**
 * PB.2: Create-GIN (Goods Inward Note) reducer. Drives the shared
 * [com.miletracker.core.ui.components.scaffold.FormSubmissionScaffold]: field setters,
 * [CreateGinUiState.canSubmit] gating, and a submit that runs the rotating-status fake and emits a one-shot
 * effect (receipt acknowledged / approval / QC hold) for the screen to route or toast.
 */
class CreateGinViewModel(
    private val repository: GinRepository,
) : BaseViewModel<CreateGinUiState, CreateGinEffect, CreateGinAction>(CreateGinUiState()) {
    override fun onAction(action: CreateGinAction) {
        when (action) {
            is CreateGinAction.SetGinNumber -> setState { copy(ginNumber = action.value) }
            is CreateGinAction.SetPoReference -> setState { copy(poReference = action.value) }
            is CreateGinAction.SetVendor -> setState { copy(vendor = action.value) }
            is CreateGinAction.SetWarehouse -> setState { copy(warehouse = action.value) }
            is CreateGinAction.SetReceivedQty -> setState { copy(receivedQtyText = action.value) }
            is CreateGinAction.SetRemarks -> setState { copy(remarks = action.value) }
            CreateGinAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submit(
                GinDraft(
                    ginNumber = s.ginNumber.trim(),
                    poReference = s.poReference.trim(),
                    vendor = s.vendor.trim(),
                    warehouse = s.warehouse.trim(),
                    receivedQty = s.receivedQty ?: 0,
                    remarks = s.remarks.trim(),
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        when (result) {
            is PayablesSubmissionResult.Submitted -> emitEffect(CreateGinEffect.Success(result.id))
            is PayablesSubmissionResult.NeedsApproval -> emitEffect(CreateGinEffect.NeedsApproval(result.id))
            is PayablesSubmissionResult.PolicyViolation -> emitEffect(CreateGinEffect.Violation(result.messages))
        }
    }
}
