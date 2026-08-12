package com.mileway.feature.approvals.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.EmptyState
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.siddharth.kmp.common.formatDecimal

/**
 * The "who is this person, and should I trust this claim" question a manager actually asks before
 * approving/rejecting — a quick track record (approved/rejected/pending/flagged counts) plus the
 * underlying requests, newest first. Read-only: this is a lookup, not another action surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimantHistorySheet(
    requesterName: String,
    items: List<ApprovalItem>,
    onDismiss: () -> Unit,
) {
    val sorted = remember(items) { items.sortedByDescending { it.timestampMs } }
    val approved = sorted.count { it.status == ApprovalStatus.APPROVED }
    val rejected = sorted.count { it.status == ApprovalStatus.REJECTED }
    val pending = sorted.count { it.status == ApprovalStatus.PENDING }
    val flagged = sorted.count { it.policyViolation }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp),
        ) {
            Text("$requesterName — request history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append("$approved approved · $rejected rejected · $pending pending")
                    if (flagged > 0) append(" · $flagged flagged")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            if (sorted.isEmpty()) {
                EmptyState(
                    title = "No prior requests",
                    subtitle = "This is $requesterName's first request on record.",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(sorted, key = { it.id }) { item -> ClaimantHistoryRow(item) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClaimantHistoryRow(item: ApprovalItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(item.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "₹${item.amountRupees.formatDecimal(2)} · ${item.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val (label, color) =
            when (item.status) {
                ApprovalStatus.PENDING -> "Pending" to MilewayColors.warning
                ApprovalStatus.APPROVED -> "Approved" to MilewayColors.success
                ApprovalStatus.REJECTED -> "Rejected" to MilewayColors.danger
            }
        Surface(shape = DesignTokens.Shape.button, color = color.copy(alpha = 0.15f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
