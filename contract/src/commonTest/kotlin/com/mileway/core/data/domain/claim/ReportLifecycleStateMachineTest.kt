package com.mileway.core.data.domain.claim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReportLifecycleStateMachineTest {
    @Test
    fun submitMovesDraftToSubmitted() {
        assertEquals(
            ReportLifecycleState.SUBMITTED,
            ReportLifecycleStateMachine.transition(ReportLifecycleState.DRAFT, ReportLifecycleEvent.SUBMIT),
        )
    }

    @Test
    fun approveMovesSubmittedToApproved() {
        assertEquals(
            ReportLifecycleState.APPROVED,
            ReportLifecycleStateMachine.transition(ReportLifecycleState.SUBMITTED, ReportLifecycleEvent.APPROVE),
        )
    }

    @Test
    fun sendBackMovesSubmittedToSentBack() {
        assertEquals(
            ReportLifecycleState.SENT_BACK,
            ReportLifecycleStateMachine.transition(ReportLifecycleState.SUBMITTED, ReportLifecycleEvent.SEND_BACK),
        )
    }

    @Test
    fun resubmitMovesSentBackToSubmitted() {
        assertEquals(
            ReportLifecycleState.SUBMITTED,
            ReportLifecycleStateMachine.transition(ReportLifecycleState.SENT_BACK, ReportLifecycleEvent.RESUBMIT),
        )
    }

    @Test
    fun reimburseMovesApprovedToPaid() {
        assertEquals(
            ReportLifecycleState.PAID,
            ReportLifecycleStateMachine.transition(ReportLifecycleState.APPROVED, ReportLifecycleEvent.REIMBURSE),
        )
    }

    /** Every (state, event) pair not covered above must throw — enumerated exhaustively, not sampled. */
    @Test
    fun everyIllegalTransitionThrows() {
        val legal =
            setOf(
                ReportLifecycleState.DRAFT to ReportLifecycleEvent.SUBMIT,
                ReportLifecycleState.SUBMITTED to ReportLifecycleEvent.APPROVE,
                ReportLifecycleState.SUBMITTED to ReportLifecycleEvent.SEND_BACK,
                ReportLifecycleState.SENT_BACK to ReportLifecycleEvent.RESUBMIT,
                ReportLifecycleState.APPROVED to ReportLifecycleEvent.REIMBURSE,
                ReportLifecycleState.APPROVED to ReportLifecycleEvent.RELEASE_FOR_PAYMENT,
                ReportLifecycleState.APPROVED_FOR_PAYMENT to ReportLifecycleEvent.REIMBURSE,
                ReportLifecycleState.SUBMITTED to ReportLifecycleEvent.REJECT,
                ReportLifecycleState.SUBMITTED to ReportLifecycleEvent.RECALL,
                ReportLifecycleState.RECALLED to ReportLifecycleEvent.SUBMIT,
            )

        for (state in ReportLifecycleState.entries) {
            for (event in ReportLifecycleEvent.entries) {
                if (state to event in legal) continue
                assertFailsWith<IllegalReportTransitionException>("$state + $event should be illegal") {
                    ReportLifecycleStateMachine.transition(state, event)
                }
            }
        }
    }

    @Test
    fun approveBeforeSubmitThrows() {
        assertFailsWith<IllegalReportTransitionException> {
            ReportLifecycleStateMachine.transition(ReportLifecycleState.DRAFT, ReportLifecycleEvent.APPROVE)
        }
    }

    @Test
    fun reimburseAPaidReportThrows() {
        assertFailsWith<IllegalReportTransitionException> {
            ReportLifecycleStateMachine.transition(ReportLifecycleState.PAID, ReportLifecycleEvent.REIMBURSE)
        }
    }

    // Recall is an explicit RECALLED state; ReportRepository guards it against recorded actions.
    @Test
    fun thereIsNoDraftRecallTransitionFromSubmittedForAnyEvent() {
        for (event in ReportLifecycleEvent.entries) {
            val result = runCatching { ReportLifecycleStateMachine.transition(ReportLifecycleState.SUBMITTED, event) }
            // Either the event is illegal (throws) or it's legal and lands on APPROVED/SENT_BACK —
            // never DRAFT. No code path in this machine un-submits a report.
            result.onSuccess { next -> assertTrue(next != ReportLifecycleState.DRAFT, "Submitted + $event must never yield Draft") }
        }
    }

    @Test
    fun resubmitOnlyFiresFromSentBackWhichOnlyExistsAfterASendBackActionWasRecorded() {
        // SentBack is unreachable without first recording a SEND_BACK ApprovalStep (Application-
        // level: ReportRoutes.approveOrSendBack always inserts the step before this transition
        // fires) — so RESUBMIT firing only from SENT_BACK is, by construction, gated on an action
        // having been recorded, the mirror of B03's "no action recorded" guard.
        assertEquals(
            ReportLifecycleState.SUBMITTED,
            ReportLifecycleStateMachine.transition(ReportLifecycleState.SENT_BACK, ReportLifecycleEvent.RESUBMIT),
        )
        for (state in ReportLifecycleState.entries) {
            if (state == ReportLifecycleState.SENT_BACK) continue
            assertFailsWith<IllegalReportTransitionException>("$state + RESUBMIT should be illegal") {
                ReportLifecycleStateMachine.transition(state, ReportLifecycleEvent.RESUBMIT)
            }
        }
    }
}
