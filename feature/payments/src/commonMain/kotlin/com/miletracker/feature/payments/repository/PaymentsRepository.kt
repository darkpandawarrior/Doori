package com.miletracker.feature.payments.repository

import com.miletracker.feature.payments.model.PaymentDirection
import com.miletracker.feature.payments.model.PaymentRecord
import com.miletracker.feature.payments.model.PaymentStatus
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
