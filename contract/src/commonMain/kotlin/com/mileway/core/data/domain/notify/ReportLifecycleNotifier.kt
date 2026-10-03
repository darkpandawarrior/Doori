package com.mileway.core.data.domain.notify

import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState

/** Local inbox payload. The Room adapter lives in core:data, keeping contract platform free. */
data class ReportLifecycleNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val createdAtMs: Long,
    val deeplink: String,
)

/** Maps one persisted lifecycle transition to one stable, replay-safe Notification Centre row. */
object ReportLifecycleNotifier {
    fun map(
        from: ReportLifecycleState,
        report: Report,
        atMillis: Long,
    ): ReportLifecycleNotification? {
        if (from == report.state) return null
        val label =
            when (report.state) {
                ReportLifecycleState.DRAFT -> "Draft"
                ReportLifecycleState.SUBMITTED -> "Submitted"
                ReportLifecycleState.APPROVED -> "Approved"
                ReportLifecycleState.SENT_BACK -> "Sent back"
                ReportLifecycleState.PAID -> "Paid (simulated)"
                ReportLifecycleState.APPROVED_FOR_PAYMENT -> "Approved for payment"
                ReportLifecycleState.REJECTED -> "Rejected"
                ReportLifecycleState.RECALLED -> "Recalled"
            }
        return ReportLifecycleNotification(
            id = "report:${report.id}:${report.recordVersion}",
            title = "Report $label",
            body = "Report ${report.id}: $label",
            type = if (report.state == ReportLifecycleState.PAID) "PAYABLES" else "APPROVAL",
            createdAtMs = atMillis,
            deeplink = "mileway://approvals/detail/report:${report.id}",
        )
    }
}
