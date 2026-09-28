package com.mileway.core.data.domain.claim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The expense-report lifecycle Doori's backend slice ships: Draft -> Submitted ->
 * Approved/SentBack -> Paid. Every value carries an explicit [SerialName] — this enum is shared
 * wire contract between `:server` and every client, so the wire form must never drift with a
 * Kotlin symbol rename.
 *
 * ponytail: the full Doori spec (2026-09-25 suite plan, L1a) lists nine states (PendingApproval,
 * PendingFinanceReview, ApprovedForPayment, Rejected, Recalled...) for the multi-level approval
 * chain and finance-review journeys. This backend slice ships only the four-state loop the task
 * brief names; widen the enum (additively — new entries, never a rename) when a later lane adds
 * multi-step approval or a finance review step.
 */
@Serializable
enum class ReportLifecycleState {
    @SerialName("draft")
    DRAFT,

    @SerialName("submitted")
    SUBMITTED,

    @SerialName("approved")
    APPROVED,

    @SerialName("sent_back")
    SENT_BACK,

    @SerialName("paid")
    PAID,
}

/** The action driving a [ReportLifecycleState] transition. */
@Serializable
enum class ReportLifecycleEvent {
    @SerialName("submit")
    SUBMIT,

    @SerialName("approve")
    APPROVE,

    @SerialName("send_back")
    SEND_BACK,

    @SerialName("resubmit")
    RESUBMIT,

    @SerialName("reimburse")
    REIMBURSE,
}

/** Thrown by [ReportLifecycleStateMachine.transition] when [event] cannot fire from [from]. */
class IllegalReportTransitionException(
    val from: ReportLifecycleState,
    val event: ReportLifecycleEvent,
) : IllegalStateException("Cannot apply $event to a report in state $from")
