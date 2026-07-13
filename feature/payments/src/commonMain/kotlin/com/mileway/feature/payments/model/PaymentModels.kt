package com.mileway.feature.payments.model

/** Whether a payment sends money out (pay) or asks for money in (request) (PM). */
enum class PaymentDirection(val label: String) {
    PAY("Pay"),
    REQUEST("Request"),
}

/**
 * Lifecycle status of a UPI/QR payment (PM). [ACTIVE]/[EXPIRED] are P29.C.8's QR *advance/request*
 * lifecycle (a [PaymentDirection.REQUEST] is ACTIVE until it's paid — COMPLETED — or its validity
 * window elapses unpaid — EXPIRED, see [PaymentRecord.effectiveStatus]). [PENDING] stays for PAY's
 * still-settling case.
 */
enum class PaymentStatus(val label: String) {
    PENDING("Pending"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    EXPIRED("Expired"),
}

/** One row in the payments history (PM), a mocked UPI/QR pay or request. */
data class PaymentRecord(
    val id: String,
    val direction: PaymentDirection,
    val counterparty: String,
    val amount: Double,
    val status: PaymentStatus,
    val note: String,
    val dateMillis: Long,
    // P29.C.7: post-payment invoice attachment — set once `AttachInvoice` clears the
    // duplicate-detection gate. `invoiceOcrPrefill` holds whatever DocField the shared OCR
    // pipeline (core:ai's DocumentIntelligence, via core:media's rememberMediaCaptureLauncher)
    // managed to read off the picked image (invoice number/amount/date), keyed by DocField.name.
    val attachmentUrl: String? = null,
    val invoiceOcrPrefill: Map<String, String> = emptyMap(),
    // P29.C.8: only set for ACTIVE `REQUEST` records — the validity window after which an unpaid
    // request is treated as EXPIRED. See [effectiveStatus].
    val expiresAtMillis: Long? = null,
) {
    /** P29.C.8: ACTIVE requests past [expiresAtMillis] read as EXPIRED without mutating the stored row. */
    fun effectiveStatus(nowMillis: Long): PaymentStatus =
        if (status == PaymentStatus.ACTIVE && expiresAtMillis != null && nowMillis >= expiresAtMillis) {
            PaymentStatus.EXPIRED
        } else {
            status
        }
}

/**
 * P29.C.6: the reference app's IDLE/SUBMITTING/POLLING/SUCCESS/FAILED transaction lifecycle,
 * driven through real `delay()`-based state transitions in [com.mileway.feature.payments.viewmodel.CreatePaymentViewModel]
 * instead of one flat synchronous result.
 */
enum class PaymentTransactionStatus { IDLE, SUBMITTING, POLLING, SUCCESS, FAILED }
