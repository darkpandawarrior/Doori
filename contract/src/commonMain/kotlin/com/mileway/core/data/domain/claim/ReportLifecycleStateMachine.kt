package com.mileway.core.data.domain.claim

/**
 * The one transition table for [ReportLifecycleState]. `:server` and every client call
 * [transition] instead of hand-rolling a `when` over the state elsewhere, so the legal-move set
 * has exactly one definition.
 */
object ReportLifecycleStateMachine {
    /** Returns the next state, or throws [IllegalReportTransitionException] if [event] is illegal from [from]. */
    fun transition(
        from: ReportLifecycleState,
        event: ReportLifecycleEvent,
    ): ReportLifecycleState =
        when (from) {
            ReportLifecycleState.DRAFT ->
                when (event) {
                    ReportLifecycleEvent.SUBMIT -> ReportLifecycleState.SUBMITTED
                    else -> illegal(from, event)
                }
            ReportLifecycleState.SUBMITTED ->
                when (event) {
                    ReportLifecycleEvent.APPROVE -> ReportLifecycleState.APPROVED
                    ReportLifecycleEvent.SEND_BACK -> ReportLifecycleState.SENT_BACK
                    else -> illegal(from, event)
                }
            ReportLifecycleState.SENT_BACK ->
                when (event) {
                    ReportLifecycleEvent.RESUBMIT -> ReportLifecycleState.SUBMITTED
                    else -> illegal(from, event)
                }
            ReportLifecycleState.APPROVED ->
                when (event) {
                    ReportLifecycleEvent.REIMBURSE -> ReportLifecycleState.PAID
                    else -> illegal(from, event)
                }
            ReportLifecycleState.PAID -> illegal(from, event)
        }

    private fun illegal(
        from: ReportLifecycleState,
        event: ReportLifecycleEvent,
    ): Nothing = throw IllegalReportTransitionException(from, event)
}
