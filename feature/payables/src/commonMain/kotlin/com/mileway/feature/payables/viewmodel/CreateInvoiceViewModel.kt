package com.mileway.feature.payables.viewmodel

import com.mileway.feature.payables.repository.InvoiceDraft
import com.mileway.feature.payables.repository.InvoiceRepository
import com.mileway.feature.payables.repository.InvoiceSubmissionResult
import com.siddharth.kmp.mvi.BaseViewModel

data class CreateInvoiceUiState(
    val invoiceNumber: String = "",
    val vendor: String = "",
    val amountText: String = "",
    val taxPercentText: String = "",
    val glCode: String = "",
    val isSubmitting: Boolean = false,
    val lastResult: InvoiceSubmissionResult? = null,
) {
    val amount: Double? get() = amountText.toDoubleOrNull()

    /** Submit is gated on the three required fields + a positive amount (PB.1 validation). */
    val canSubmit: Boolean
        get() = invoiceNumber.isNotBlank() && vendor.isNotBlank() && (amount ?: 0.0) > 0.0
}

sealed interface CreateInvoiceAction {
    data class SetInvoiceNumber(val value: String) : CreateInvoiceAction

    data class SetVendor(val value: String) : CreateInvoiceAction

    data class SetAmount(val value: String) : CreateInvoiceAction

    data class SetTax(val value: String) : CreateInvoiceAction

    data class SetGlCode(val value: String) : CreateInvoiceAction

    data object Submit : CreateInvoiceAction
}

sealed interface CreateInvoiceEffect {
    data class Success(val id: String) : CreateInvoiceEffect

    data class NeedsApproval(val id: String) : CreateInvoiceEffect

    data class Violation(val messages: List<String>) : CreateInvoiceEffect
}

/**
 * PB.1: Create-Invoice reducer. Drives the [com.mileway.core.ui.components.scaffold.FormSubmissionScaffold]:
 * field setters, [CreateInvoiceUiState.canSubmit] gating, and a submit that runs the rotating-status fake and
 * emits a one-shot effect (success / approval / violation) for the screen to route or toast.
 */
class CreateInvoiceViewModel(
    private val repository: InvoiceRepository,
) : BaseViewModel<CreateInvoiceUiState, CreateInvoiceEffect, CreateInvoiceAction>(CreateInvoiceUiState()) {
    override fun onAction(action: CreateInvoiceAction) {
        when (action) {
            is CreateInvoiceAction.SetInvoiceNumber -> setState { copy(invoiceNumber = action.value) }
            is CreateInvoiceAction.SetVendor -> setState { copy(vendor = action.value) }
            is CreateInvoiceAction.SetAmount -> setState { copy(amountText = action.value) }
            is CreateInvoiceAction.SetTax -> setState { copy(taxPercentText = action.value) }
            is CreateInvoiceAction.SetGlCode -> setState { copy(glCode = action.value) }
            CreateInvoiceAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submit(
                InvoiceDraft(
                    invoiceNumber = s.invoiceNumber.trim(),
                    vendor = s.vendor.trim(),
                    amount = s.amount ?: 0.0,
                    taxPercent = s.taxPercentText.toDoubleOrNull() ?: 0.0,
                    glCode = s.glCode.trim(),
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        when (result) {
            is InvoiceSubmissionResult.Submitted -> emitEffect(CreateInvoiceEffect.Success(result.id))
            is InvoiceSubmissionResult.NeedsApproval -> emitEffect(CreateInvoiceEffect.NeedsApproval(result.id))
            is InvoiceSubmissionResult.PolicyViolation -> emitEffect(CreateInvoiceEffect.Violation(result.messages))
        }
    }
}
