package com.mileway.core.data.domain.claim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
