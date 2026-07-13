package com.mileway.feature.payments.repository

import com.mileway.core.ai.DuplicateDetector
import com.mileway.core.ai.model.DedupCandidate
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.feature.payments.model.PaymentDirection
import com.mileway.feature.payments.model.PaymentRecord
import com.mileway.feature.payments.model.PaymentStatus
import kotlin.time.Clock

/** A QR/UPI pay-or-request form payload (PM). */
data class PaymentDraft(
    val direction: PaymentDirection,
    val counterparty: String,
    val amount: Double,
    val note: String,
)

/** Rotating submission outcome for the QR/UPI flow (PM). */
sealed interface PaymentResult {
    data class Completed(val id: String) : PaymentResult

    data class Pending(val id: String) : PaymentResult

    data class Failed(val reason: String) : PaymentResult
}

/**
 * Offline fake UPI/QR payments store (PM), seeds a deterministic history (Clock-injected, no `Math.random`)
 * and returns a **rotating** [PaymentResult] (completed / pending / failed) across repeated submits. Every UPI
 * call is mocked theater; no real PSP. Mirrors the PB/TR fake-repo pattern.
 */
class PaymentsRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L
    private val submitted = mutableListOf<PaymentDraft>()
    private var counter = 0

    // P29.C.7: local invoice-duplicate heuristic (core:ai's DuplicateDetector — same class the
    // receipt-scan pipeline uses; default 5-minute window, i.e. "attached the same invoice image
    // twice in the same session" rather than a long-term dupe). Seeded with one already-attached
    // invoice, one minute ago, so the Possible/Confirmed branches are actually reachable in a demo
    // or test, not just theoretical.
    private val duplicateDetector = DuplicateDetector()
    private val attachedInvoices =
        mutableListOf(
            DedupCandidate(ref = "PAY-5003", merchant = "amazon", total = "2499.0", timestampMillis = clock.now().toEpochMilliseconds() - 60_000L),
        )

    fun submit(draft: PaymentDraft): PaymentResult {
        submitted += draft
        val id = "PAY-${4100 + submitted.size}"
        return when (counter++ % 3) {
            0 -> PaymentResult.Completed(id)
            1 -> PaymentResult.Pending(id)
            else -> PaymentResult.Failed("Beneficiary bank declined the collect request")
        }
    }

    fun count(): Int = submitted.size

    /** Checks a picked invoice's OCR fields against previously-attached invoices, no network lookup. */
    fun checkInvoiceDuplicate(
        fields: Map<DocField, ExtractedValue>,
        timestampMillis: Long,
    ): DuplicateVerdict = duplicateDetector.check(fields, timestampMillis, attachedInvoices)

    /** Records a cleared attachment so later attaches can be checked against it too. */
    fun recordInvoiceAttachment(
        paymentId: String,
        fields: Map<DocField, ExtractedValue>,
        timestampMillis: Long,
    ) {
        attachedInvoices +=
            DedupCandidate(
                ref = paymentId,
                merchant = fields[DocField.MERCHANT]?.value,
                total = fields[DocField.TOTAL]?.value,
                timestampMillis = timestampMillis,
            )
    }

    private fun all(): List<PaymentRecord> {
        val now = clock.now().toEpochMilliseconds()
        val spec =
            listOf(
                Spec(PaymentDirection.PAY, "chai@stall", 60.0, PaymentStatus.COMPLETED, "Morning chai", 0L),
                Spec(PaymentDirection.PAY, "ola@upi", 340.0, PaymentStatus.COMPLETED, "Airport cab", 1L),
                Spec(PaymentDirection.REQUEST, "rahul@okhdfc", 1200.0, PaymentStatus.PENDING, "Team lunch split", 2L),
                Spec(PaymentDirection.PAY, "amazon@apl", 2499.0, PaymentStatus.FAILED, "Office supplies", 4L),
                Spec(PaymentDirection.REQUEST, "priya@okaxis", 800.0, PaymentStatus.COMPLETED, "Cab share", 9L),
            )
        return spec.mapIndexed { index, sp ->
            PaymentRecord(
                id = "PAY-${5000 + index}",
                direction = sp.direction,
                counterparty = sp.counterparty,
                amount = sp.amount,
                status = sp.status,
                note = sp.note,
                dateMillis = now - sp.daysAgo * dayMs,
            )
        }
    }

    /** All payments, or just those in [status] when non-null, newest first. */
    fun payments(status: PaymentStatus? = null): List<PaymentRecord> =
        all().filter { status == null || it.status == status }.sortedByDescending { it.dateMillis }

    private data class Spec(
        val direction: PaymentDirection,
        val counterparty: String,
        val amount: Double,
        val status: PaymentStatus,
        val note: String,
        val daysAgo: Long,
    )
}
