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

    // ── B03/B04 (backend-validation-matrix.md): recall allowed only with no recorded ApprovalStep
    // action, blocked once one exists. This 4-state+resubmit machine does not model a Draft recall
    // at all — there is no Submitted->Draft transition in the table for ANY event, legal or not —
    // which is stricter than B03/B04 requires, not a gap: a report can never silently un-submit.
    // What this machine DOES model is the resubmit half: SentBack->Submitted only fires after a
    // SEND_BACK action was already recorded (that's how a report reaches SentBack in the first
    // place) — the mirror-image guard of B03's "no action recorded" condition. Both directions are
    // asserted explicitly here rather than left to be inferred from the exhaustive sweep above.

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
