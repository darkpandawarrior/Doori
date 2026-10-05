package com.mileway.feature.travel.request

import com.mileway.core.data.claim.ApprovalReview
import com.mileway.core.data.claim.FINANCE_ROLE
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.data.domain.notify.ReportLifecycleNotification
import com.mileway.core.data.domain.notify.ReportLifecycleNotifier
import com.mileway.core.data.domain.travel.TravelRequest
import com.mileway.core.data.model.db.ApprovalStepEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Session-only requests. No report, claim, approval-step or payout DAO is used by this store. */
class TravelRequestStore(
    private val notify: suspend (ReportLifecycleNotification) -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val rows = MutableStateFlow<List<TravelRequest>>(emptyList())
    val requests = rows.asStateFlow()

    /** Reuses the report review's manager/finance routing, with transient history projections only. */
    fun nextRole(request: TravelRequest): String =
        ApprovalReview(
            request.lifecycleReport(),
            request.approvalChain.steps.map { step ->
                ApprovalStepEntity(
                    reportId = request.id,
                    stepIndex = step.stepIndex,
                    role = step.role,
                    thresholdMinor = step.thresholdMinor,
                    actedBy = step.actedBy,
                    onBehalfOf = step.onBehalfOf,
                    action = step.action.name,
                    comment = step.comment,
                    actedAtMillis = step.actedAtMillis,
                )
            },
        ).nextRole

    suspend fun submit(draft: TravelRequest): TravelRequest =
        mutex.withLock {
            require(rows.value.none { it.id == draft.id }) { "Request already exists" }
            require(draft.state == ReportLifecycleState.DRAFT && draft.approvalChain.steps.isEmpty()) { "Submit a new draft" }
            save(draft, draft.transition(ReportLifecycleEvent.SUBMIT))
        }

    /** Optimistic version checks and the existing chain keep local review ordered and replay-safe. */
    suspend fun act(
        id: String,
        expectedVersion: Long,
        actor: String,
        role: String,
        action: ApprovalAction,
        comment: String,
    ): TravelRequest =
        mutex.withLock {
            val request = requireNotNull(rows.value.find { it.id == id }) { "Request not found" }
            require(request.recordVersion == expectedVersion) { "Request changed; reload before reviewing" }
            require(request.state == ReportLifecycleState.SUBMITTED) { "Request is not awaiting review" }
            require(actor.isNotBlank() && actor.trim() != request.employeeId) { "Self approval is not allowed" }
            require(comment.isNotBlank()) { "A review comment is required" }
            require(role == nextRole(request)) { "Review requires the ${nextRole(request)} step" }
            val event =
                when (action) {
                    ApprovalAction.APPROVE -> ReportLifecycleEvent.APPROVE
                    ApprovalAction.SEND_BACK -> ReportLifecycleEvent.SEND_BACK
                    ApprovalAction.REJECT -> ReportLifecycleEvent.REJECT
                }
            require(request.recordVersion < Long.MAX_VALUE) { "Request version exhausted" }
            val reviewed =
                if (action == ApprovalAction.APPROVE && role != FINANCE_ROLE) {
                    request.copy(recordVersion = request.recordVersion + 1)
                } else {
                    request.transition(event)
                }
            val step =
                ApprovalStep(
                    request.approvalChain.steps.size,
                    role,
                    request.estimate.amountMinor,
                    actor.trim(),
                    action = action,
                    comment = comment.trim(),
                    actedAtMillis = now(),
                )
            save(request, reviewed.copy(approvalChain = ApprovalChain(request.approvalChain.steps + step)))
        }

    suspend fun transition(
        id: String,
        expectedVersion: Long,
        event: ReportLifecycleEvent,
    ): TravelRequest =
        mutex.withLock {
            require(
                event in setOf(ReportLifecycleEvent.RECALL, ReportLifecycleEvent.RESUBMIT, ReportLifecycleEvent.SUBMIT),
            ) { "Use review actions for approval" }
            val request = requireNotNull(rows.value.find { it.id == id })
            require(request.recordVersion == expectedVersion) { "Request changed; reload before acting" }
            save(request, request.transition(event))
        }

    private suspend fun save(
        before: TravelRequest,
        after: TravelRequest,
    ): TravelRequest {
        val notification =
            ReportLifecycleNotifier.map(before.state, after.lifecycleReport(), now())
                ?: if (after.approvalChain != before.approvalChain) {
                    ReportLifecycleNotifier.map(ReportLifecycleState.DRAFT, after.lifecycleReport(), now())
                } else {
                    null
                }
        notification?.let {
            notify(
                it.copy(
                    id = "travel:${after.id}:${after.recordVersion}",
                    title = if (before.state == after.state) "Travel request: finance review required" else it.title.replace("Report", "Travel request"),
                    body = "${after.purpose}: ${formatMinorCurrency(
                        after.estimate.amountMinor,
                        after.estimate.currency,
                    )}${if (after.estimate.approximate) " (approximate)" else " (OSRM route)"}. Session only.",
                    // Session-only requests have no persisted report detail route.
                    deeplink = "",
                ),
            )
        }
        rows.value = rows.value.filterNot { it.id == after.id } + after
        return after
    }
}
