package com.mileway.core.data.domain.claim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What an [ApprovalStep] recorded. */
@Serializable
enum class ApprovalAction {
    @SerialName("approve")
    APPROVE,

    @SerialName("send_back")
    SEND_BACK,
}

/**
 * One recorded approver action against a [Report]. [role] is the approver's role at the point of
 * action (e.g. "manager", "finance"); [thresholdMinor] is the amount threshold that routed the
 * report to this role, when the chain is threshold-driven; [onBehalfOf] is set when [actedBy]
 * acted as a delegate for another approver.
 */
@Serializable
data class ApprovalStep(
    @SerialName("stepIndex") val stepIndex: Int,
    @SerialName("role") val role: String = "manager",
    @SerialName("thresholdMinor") val thresholdMinor: Long? = null,
    @SerialName("actedBy") val actedBy: String,
    @SerialName("onBehalfOf") val onBehalfOf: String? = null,
    @SerialName("action") val action: ApprovalAction,
    @SerialName("comment") val comment: String? = null,
    @SerialName("actedAtMillis") val actedAtMillis: Long,
)

/**
 * The ordered approval history for a [Report]. Single-step in this backend slice (one approver,
 * no delegate/finance escalation) — ponytail: the full spec's multi-level routing (L1a/L11) widens
 * [steps] to more than one role; nothing here forecloses that, it just isn't built yet.
 */
@Serializable
data class ApprovalChain(
    @SerialName("steps") val steps: List<ApprovalStep> = emptyList(),
)
