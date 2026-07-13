package com.mileway.feature.payments.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.payments.model.PaymentDirection
import com.mileway.feature.payments.model.PaymentTransactionStatus
import com.mileway.feature.payments.repository.PaymentDraft
import com.mileway.feature.payments.repository.PaymentResult
import com.mileway.feature.payments.repository.PaymentsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Amounts at/above this gate a PIN confirmation before the transaction is submitted (PM/P29.C.6). */
private const val PIN_REQUIRED_AMOUNT = 5_000.0
private const val PIN_LENGTH = 4
private const val SUBMITTING_DELAY_MILLIS = 400L
private const val POLLING_DELAY_MILLIS = 700L

data class CreatePaymentUiState(
    val direction: PaymentDirection = PaymentDirection.PAY,
    val counterparty: String = "",
    val amountText: String = "",
    val note: String = "",
    // P29.C.6: IDLE/SUBMITTING/POLLING/SUCCESS/FAILED, driven by real delay()s in [CreatePaymentViewModel].
    val transactionStatus: PaymentTransactionStatus = PaymentTransactionStatus.IDLE,
    val awaitingPin: Boolean = false,
    val pinVerified: Boolean = false,
    val pinText: String = "",
    val pinError: Boolean = false,
    val resultId: String? = null,
    val failureReason: String? = null,
    // P29.C.7: invoice attachment, gated on a duplicate check against PaymentsRepository history.
    val attachmentUrl: String? = null,
    val invoiceOcrPrefill: Map<String, String> = emptyMap(),
    val duplicatePrompt: DuplicateVerdict.Possible? = null,
) {
    val amount: Double? get() = amountText.toDoubleOrNull()

    /** Submit is gated on a counterparty (UPI id / payee) and a positive amount (PM validation). */
    val canSubmit: Boolean
        get() = counterparty.isNotBlank() && (amount ?: 0.0) > 0.0

    val requiresPin: Boolean get() = (amount ?: 0.0) >= PIN_REQUIRED_AMOUNT

    val isSubmitting: Boolean
        get() = transactionStatus == PaymentTransactionStatus.SUBMITTING || transactionStatus == PaymentTransactionStatus.POLLING
}

sealed interface CreatePaymentAction {
    data class SetDirection(val value: PaymentDirection) : CreatePaymentAction

    data class SetCounterparty(val value: String) : CreatePaymentAction

    data class SetAmount(val value: String) : CreatePaymentAction

    data class SetNote(val value: String) : CreatePaymentAction

    data object Submit : CreatePaymentAction

    data class SetPinDigits(val value: String) : CreatePaymentAction

    data object ConfirmPin : CreatePaymentAction

    data object CancelPin : CreatePaymentAction

    data object Retry : CreatePaymentAction

    /** P29.C.7: fired once the shared OCR pipeline (core:media's rememberMediaCaptureLauncher +
     * core:ai's DocumentIntelligence) finishes analyzing a picked invoice image. */
    data class AttachInvoice(val analysis: DocumentAnalysis, val uri: String) : CreatePaymentAction

    data object ConfirmDuplicateAttach : CreatePaymentAction

    data object CancelDuplicateAttach : CreatePaymentAction
}

sealed interface CreatePaymentEffect {
    data class Completed(val id: String) : CreatePaymentEffect

    data class Failed(val reason: String) : CreatePaymentEffect

    data object InvoiceAttached : CreatePaymentEffect

    /** DuplicateVerdict.Confirmed — a hard stop, no soft-confirm path. */
    data object InvoiceDuplicateBlocked : CreatePaymentEffect
}

/**
 * PM: QR/UPI pay-or-request reducer on the shared `FormSubmissionScaffold`.
 *
 * P29.C.6 replaces the old one-shot `Submit` with a real IDLE→SUBMITTING→POLLING→SUCCESS/FAILED
 * state machine (each stage a genuine `delay()`, not an instant flip), gated behind a PIN
 * confirmation for amounts at/above [PIN_REQUIRED_AMOUNT]. P29.C.7 adds a post-success invoice
 * attachment step wired through `core:ai`'s [DuplicateVerdict]: `Confirmed` hard-stops the
 * attach, `Possible` needs an explicit [CreatePaymentAction.ConfirmDuplicateAttach], `Unique`
 * attaches immediately with whatever fields the OCR pass managed to read.
 */
class CreatePaymentViewModel(
    private val repository: PaymentsRepository,
    private val clock: Clock = Clock.System,
) : BaseViewModel<CreatePaymentUiState, CreatePaymentEffect, CreatePaymentAction>(CreatePaymentUiState()) {
    private var pendingAttachment: PendingAttachment? = null

    private data class PendingAttachment(val analysis: DocumentAnalysis, val uri: String, val timestampMillis: Long)

    override fun onAction(action: CreatePaymentAction) {
        when (action) {
            is CreatePaymentAction.SetDirection -> setState { copy(direction = action.value) }
            is CreatePaymentAction.SetCounterparty -> setState { copy(counterparty = action.value) }
            is CreatePaymentAction.SetAmount -> setState { copy(amountText = action.value) }
            is CreatePaymentAction.SetNote -> setState { copy(note = action.value) }
            CreatePaymentAction.Submit -> attemptSubmit()
            is CreatePaymentAction.SetPinDigits ->
                setState { copy(pinText = action.value.filter { it.isDigit() }.take(PIN_LENGTH), pinError = false) }
            CreatePaymentAction.ConfirmPin -> confirmPin()
            CreatePaymentAction.CancelPin -> setState { copy(awaitingPin = false, pinText = "", pinError = false) }
            CreatePaymentAction.Retry -> {
                setState { copy(transactionStatus = PaymentTransactionStatus.IDLE, failureReason = null) }
                attemptSubmit()
            }
            is CreatePaymentAction.AttachInvoice -> attachInvoice(action.analysis, action.uri)
            CreatePaymentAction.ConfirmDuplicateAttach -> confirmDuplicateAttach()
            CreatePaymentAction.CancelDuplicateAttach -> {
                pendingAttachment = null
                setState { copy(duplicatePrompt = null) }
            }
        }
    }

    private fun attemptSubmit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        if (s.requiresPin && !s.pinVerified) {
            setState { copy(awaitingPin = true) }
            return
        }
        runTransaction()
    }

    private fun confirmPin() {
        val s = currentState
        if (s.pinText.length != PIN_LENGTH) {
            setState { copy(pinError = true) }
            return
        }
        setState { copy(awaitingPin = false, pinVerified = true, pinText = "", pinError = false) }
        runTransaction()
    }

    private fun runTransaction() {
        viewModelScope.launch {
            setState { copy(transactionStatus = PaymentTransactionStatus.SUBMITTING) }
            delay(SUBMITTING_DELAY_MILLIS)
            setState { copy(transactionStatus = PaymentTransactionStatus.POLLING) }
            delay(POLLING_DELAY_MILLIS)

            val s = currentState
            when (val result = repository.submit(PaymentDraft(s.direction, s.counterparty.trim(), s.amount ?: 0.0, s.note.trim()))) {
                is PaymentResult.Completed -> resolveSuccess(result.id)
                is PaymentResult.Pending -> {
                    // Still "polling" per the reference app's real polling loop — one more round
                    // before the mock resolves it.
                    delay(POLLING_DELAY_MILLIS)
                    resolveSuccess(result.id)
                }
                is PaymentResult.Failed -> {
                    setState { copy(transactionStatus = PaymentTransactionStatus.FAILED, failureReason = result.reason) }
                    emitEffect(CreatePaymentEffect.Failed(result.reason))
                }
            }
        }
    }

    private fun resolveSuccess(id: String) {
        setState { copy(transactionStatus = PaymentTransactionStatus.SUCCESS, resultId = id) }
        emitEffect(CreatePaymentEffect.Completed(id))
    }

    private fun attachInvoice(
        analysis: DocumentAnalysis,
        uri: String,
    ) {
        // The shared media-capture pipeline's own `analysis.duplicate` is always Unique here (it
        // never sees this feature's payment/invoice history — no dedup candidates are passed into
        // it). The real duplicate check against *this app's* attached invoices is
        // [PaymentsRepository.checkInvoiceDuplicate], run against the same DuplicateDetector class.
        val now = clock.now().toEpochMilliseconds()
        when (val verdict = repository.checkInvoiceDuplicate(analysis.fields, now)) {
            is DuplicateVerdict.Confirmed -> emitEffect(CreatePaymentEffect.InvoiceDuplicateBlocked)
            is DuplicateVerdict.Possible -> {
                pendingAttachment = PendingAttachment(analysis, uri, now)
                setState { copy(duplicatePrompt = verdict) }
            }
            DuplicateVerdict.Unique -> commitAttachment(analysis, uri, now)
        }
    }

    private fun confirmDuplicateAttach() {
        val pending = pendingAttachment ?: return
        pendingAttachment = null
        setState { copy(duplicatePrompt = null) }
        commitAttachment(pending.analysis, pending.uri, pending.timestampMillis)
    }

    private fun commitAttachment(
        analysis: DocumentAnalysis,
        uri: String,
        timestampMillis: Long,
    ) {
        val id = currentState.resultId ?: return
        repository.recordInvoiceAttachment(id, analysis.fields, timestampMillis)
        setState {
            copy(
                attachmentUrl = uri,
                invoiceOcrPrefill = analysis.fields.mapKeys { (field, _) -> field.name }.mapValues { it.value.value },
            )
        }
        emitEffect(CreatePaymentEffect.InvoiceAttached)
    }
}
