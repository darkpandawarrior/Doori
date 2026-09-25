package com.mileway.core.data.domain.claim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The whole expense claim: an employee's [lines] plus lifecycle + approval state. [recordVersion]
 * is a monotonic counter bumped on every persisted write (spec's optimistic-concurrency anchor for
 * a client-authoritative Draft) — this backend slice increments it but does not yet enforce a 409
 * on a stale write; that guard belongs to the offline-outbox/Room lane (L2), out of scope here.
 */
@Serializable
data class Report(
    @SerialName("id") val id: String,
    @SerialName("employeeId") val employeeId: String,
    @SerialName("lines") val lines: List<ClaimLine> = emptyList(),
    @SerialName("state") val state: ReportLifecycleState = ReportLifecycleState.DRAFT,
    @SerialName("approvalChain") val approvalChain: ApprovalChain = ApprovalChain(),
    @SerialName("recordVersion") val recordVersion: Long = 0L,
) {
    /** Total of every line's [ClaimLine.amountMinor] — the reimbursement amount a payout journals. */
    fun totalAmountMinor(): Long = lines.sumOf { it.amountMinor }

    /** The currency of this report's lines; assumes a single currency per report (no mixed-currency reports yet). */
    fun currency(): String = lines.firstOrNull()?.currency ?: "INR"
}

/** Request body for POST /api/reports/{id}/approve and .../send-back. */
@Serializable
data class ApprovalActionRequest(
    @SerialName("actedBy") val actedBy: String,
    @SerialName("comment") val comment: String? = null,
)
