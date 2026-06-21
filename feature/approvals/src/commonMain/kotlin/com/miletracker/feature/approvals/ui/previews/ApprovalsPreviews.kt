@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.feature.approvals.ui.previews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.common.formatDecimal
import com.miletracker.core.ui.previews.PreviewLightDark
import com.miletracker.core.ui.previews.PreviewMatrix
import com.miletracker.core.ui.previews.PreviewSurface
import com.miletracker.core.ui.previews.SampleData
import com.miletracker.feature.approvals.model.ApprovalItem
import com.miletracker.feature.approvals.model.ApprovalStatus
import com.miletracker.feature.approvals.model.ApprovalType

// ---------------------------------------------------------------------------
// Phase 9.1, Approvals feature preview matrix.
//
// ApprovalsScreen and ApprovalDetailsScreen both require koinViewModel() at
// runtime. The standalone data-driven previews below use only model types and
// PreviewSurface so the preview panel can render without a DI graph.
// ---------------------------------------------------------------------------

// ── Approval item summary card ───────────────────────────────────────────────

/** Inline approximation of what the approvals list looks like for a single pending item. */
@Composable
private fun ApprovalItemSummary(item: ApprovalItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.type.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.requesterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val statusColor =
                when (item.status) {
                    ApprovalStatus.PENDING -> Color(0xFFF59E0B)
                    ApprovalStatus.APPROVED -> Color(0xFF22C55E)
                    ApprovalStatus.REJECTED -> Color(0xFFEF4444)
                }
            Text(
                text = "₹${item.amountRupees.formatDecimal(2)}  •  ${item.status.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Pending approval item ────────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewApprovalItemPending() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalItemSummary(
                item =
                    ApprovalItem(
                        id = "A001",
                        type = ApprovalType.MILEAGE,
                        requesterName = SampleData.Approval.approverName,
                        summary = "Client visit: Hinjewadi · 48 km",
                        amountRupees = 576.0,
                        status = ApprovalStatus.PENDING,
                        timestampMs = SampleData.Trip.startTimeMs,
                        policyViolation = false,
                    ),
            )
        }
    }
}

// ── Approved item with policy violation ─────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewApprovalItemWithViolation() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalItemSummary(
                item =
                    ApprovalItem(
                        id = "A002",
                        type = ApprovalType.TRAVEL,
                        requesterName = "Aisha Khan",
                        summary = "Bangalore–Pune return flight",
                        amountRupees = 8400.0,
                        status = ApprovalStatus.PENDING,
                        timestampMs = SampleData.Trip.startTimeMs,
                        policyViolation = true,
                    ),
            )
        }
    }
}

// ── Full matrix, approved expense ───────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewApprovalItemApproved() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalItemSummary(
                item =
                    ApprovalItem(
                        id = "A003",
                        type = ApprovalType.EXPENSE,
                        requesterName = "Neha Patel",
                        summary = "Office supplies: monthly restock",
                        amountRupees = SampleData.Approval.amount,
                        status = ApprovalStatus.APPROVED,
                        timestampMs = SampleData.Trip.startTimeMs,
                    ),
            )
        }
    }
}

// ── Full matrix, rejected advance ──────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewApprovalItemRejected() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalItemSummary(
                item =
                    ApprovalItem(
                        id = "A004",
                        type = ApprovalType.ADVANCE,
                        requesterName = "Rohan Verma",
                        summary = "Travel advance: Q3 road show",
                        amountRupees = 12000.0,
                        status = ApprovalStatus.REJECTED,
                        timestampMs = SampleData.Trip.startTimeMs,
                    ),
            )
        }
    }
}
