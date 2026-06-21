package com.miletracker.feature.payments.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.payments.model.PaymentDirection
import com.miletracker.feature.payments.repository.PaymentDraft
import com.miletracker.feature.payments.repository.PaymentResult
import com.miletracker.feature.payments.repository.PaymentsRepository

data class CreatePaymentUiState(
    val direction: PaymentDirection = PaymentDirection.PAY,
    val counterparty: String = "",
    val amountText: String = "",
    val note: String = "",
    val isSubmitting: Boolean = false,
    val lastResult: PaymentResult? = null,
) {
    val amount: Double? get() = amountText.toDoubleOrNull()

    /** Submit is gated on a counterparty (UPI id / payee) and a positive amount (PM validation). */
    val canSubmit: Boolean
        get() = counterparty.isNotBlank() && (amount ?: 0.0) > 0.0
}

sealed interface CreatePaymentAction {
    data class SetDirection(val value: PaymentDirection) : CreatePaymentAction

    data class SetCounterparty(val value: String) : CreatePaymentAction

    data class SetAmount(val value: String) : CreatePaymentAction

    data class SetNote(val value: String) : CreatePaymentAction

    data object Submit : CreatePaymentAction
}

sealed interface CreatePaymentEffect {
    data class Completed(val id: String) : CreatePaymentEffect

    data class Pending(val id: String) : CreatePaymentEffect

    data class Failed(val reason: String) : CreatePaymentEffect
}

/** PM — QR/UPI pay-or-request reducer on the shared `FormSubmissionScaffold`. */
class CreatePaymentViewModel(
    private val repository: PaymentsRepository,
) : BaseViewModel<CreatePaymentUiState, CreatePaymentEffect, CreatePaymentAction>(CreatePaymentUiState()) {
    override fun onAction(action: CreatePaymentAction) {
        when (action) {
            is CreatePaymentAction.SetDirection -> setState { copy(direction = action.value) }
            is CreatePaymentAction.SetCounterparty -> setState { copy(counterparty = action.value) }
            is CreatePaymentAction.SetAmount -> setState { copy(amountText = action.value) }
            is CreatePaymentAction.SetNote -> setState { copy(note = action.value) }
            CreatePaymentAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submit(
                PaymentDraft(
                    direction = s.direction,
                    counterparty = s.counterparty.trim(),
                    amount = s.amount ?: 0.0,
                    note = s.note.trim(),
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        when (result) {
            is PaymentResult.Completed -> emitEffect(CreatePaymentEffect.Completed(result.id))
            is PaymentResult.Pending -> emitEffect(CreatePaymentEffect.Pending(result.id))
            is PaymentResult.Failed -> emitEffect(CreatePaymentEffect.Failed(result.reason))
        }
    }
}
