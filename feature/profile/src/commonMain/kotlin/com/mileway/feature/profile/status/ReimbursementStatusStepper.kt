package com.mileway.feature.profile.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.ui.components.WizardProgressBar

private const val APPROVED_MILESTONE = 3
private const val PROCESSING_MILESTONE = 4
private const val PAID_MILESTONE = 5

/** Maps persisted states to the five employee milestones without inventing payout completion. */
data class ReimbursementProgress(
    val milestone: Int,
    val message: String,
)

fun reimbursementProgress(state: ReportLifecycleState): ReimbursementProgress =
    when (state) {
        ReportLifecycleState.DRAFT -> ReimbursementProgress(1, "Draft")
        ReportLifecycleState.SUBMITTED -> ReimbursementProgress(2, "Submitted for approval")
        ReportLifecycleState.APPROVED -> ReimbursementProgress(APPROVED_MILESTONE, "Approved; awaiting payment release")
        ReportLifecycleState.APPROVED_FOR_PAYMENT -> ReimbursementProgress(PROCESSING_MILESTONE, "Processing payment (simulated)")
        ReportLifecycleState.PAID -> ReimbursementProgress(PAID_MILESTONE, "Paid (simulated)")
        ReportLifecycleState.SENT_BACK -> ReimbursementProgress(1, "Sent back; revise and resubmit")
        ReportLifecycleState.RECALLED -> ReimbursementProgress(1, "Recalled; ready to edit")
        ReportLifecycleState.REJECTED -> ReimbursementProgress(2, "Rejected; payment will not proceed")
    }

/** Uses the shared segmented stepper with a readable current-state announcement. */
@Composable
fun ReimbursementStatusStepper(
    state: ReportLifecycleState,
    modifier: Modifier = Modifier,
) {
    val progress = reimbursementProgress(state)
    Column(
        modifier = modifier.semantics { contentDescription = "Reimbursement: ${progress.message}" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(progress.message, style = MaterialTheme.typography.titleSmall)
        WizardProgressBar(step = progress.milestone, total = 5)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Draft", "Submitted", "Approved", "Processing", "Paid").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
