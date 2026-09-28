package com.mileway.core.data.domain.payout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Status of a [PendingPaymentJournal] row — mirrors PaymentsLab-KMP's shape, reimplemented here (no includeBuild edge). */
@Serializable
enum class PaymentStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("paid")
    PAID,
}

/**
 * A journaled reimbursement. `:server` writes this row (status [PaymentStatus.PENDING]) before
 * calling [PayoutBackend.payout] — journal-before-call is what makes a crash mid-payout
 * recoverable rather than silently lost or double-paid; see [PayoutBackend] for the simulator this
 * always runs against.
 */
@Serializable
data class PendingPaymentJournal(
    @SerialName("reportId") val reportId: String,
    @SerialName("amountMinor") val amountMinor: Long,
    @SerialName("currency") val currency: String,
    @SerialName("status") val status: PaymentStatus = PaymentStatus.PENDING,
    @SerialName("createdAtMillis") val createdAtMillis: Long,
)
