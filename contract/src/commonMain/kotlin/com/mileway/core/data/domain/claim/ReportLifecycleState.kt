package com.mileway.core.data.domain.claim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Report states shared by local claims and the backend. Existing wire names stay stable. */
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

    @SerialName("approved_for_payment")
    APPROVED_FOR_PAYMENT,

    @SerialName("rejected")
    REJECTED,

    @SerialName("recalled")
    RECALLED,
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

    @SerialName("release_for_payment")
    RELEASE_FOR_PAYMENT,

    @SerialName("reject")
    REJECT,

    @SerialName("recall")
    RECALL,
}

/** Thrown by [ReportLifecycleStateMachine.transition] when [event] cannot fire from [from]. */
class IllegalReportTransitionException(
    val from: ReportLifecycleState,
    val event: ReportLifecycleEvent,
) : IllegalStateException("Cannot apply $event to a report in state $from")
