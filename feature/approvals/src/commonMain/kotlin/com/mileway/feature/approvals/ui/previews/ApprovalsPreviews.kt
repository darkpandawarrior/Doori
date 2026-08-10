@file:Suppress("ktlint:standard:function-naming")

package com.mileway.feature.approvals.ui.previews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewMatrix
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.previews.SampleData
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.approvals.ui.screens.ApprovalCard
import com.mileway.feature.approvals.ui.screens.ApprovalListTab

// ---------------------------------------------------------------------------
// Approvals feature preview matrix.
//
// ApprovalsScreen and ApprovalDetailsScreen both require koinViewModel() at
// runtime. The previews below render the REAL production ApprovalCard/
// ApprovalListTab composables directly with plain data — no DI graph needed,
// and (unlike the reimplemented mini-card this file used to carry) what a
// capture of these previews shows is exactly what the shipped screen renders.
// ---------------------------------------------------------------------------

// ── Single card states — names kept stable, ScreenshotCatalogTest.kt already captures these ──

@PreviewLightDark
@Composable
fun PreviewApprovalItemPending() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalCard(
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
                selectionMode = false,
                isSelected = false,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewApprovalItemWithViolation() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalCard(
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
                selectionMode = false,
                isSelected = false,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

@PreviewMatrix
@Composable
fun PreviewApprovalItemApproved() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalCard(
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
                selectionMode = false,
                isSelected = false,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

@PreviewMatrix
@Composable
fun PreviewApprovalItemRejected() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            ApprovalCard(
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
                selectionMode = false,
                isSelected = false,
                onClick = {},
                onLongClick = {},
            )
        }
    }
}

// ── "To Approve" queue — filled/empty/selection, the manager's actual inbox ──

/** FILLED: the pending queue with realistic variety — ages, amounts, and two policy violations. */
@PreviewMatrix
@Composable
fun PreviewApprovalsQueueFilled() {
    PreviewSurface {
        ApprovalListTab(
            items = ApprovalsRepository.all.filter { it.status == ApprovalStatus.PENDING },
            emptyTitle = "You're all caught up",
            emptySubtitle = "Nothing is waiting for your approval right now.",
            onOpenDetail = {},
            selectionMode = false,
            selectedIds = emptySet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}

/** EMPTY: the positive "caught up" case — previously never captured on its own. */
@PreviewLightDark
@Composable
fun PreviewApprovalsQueueEmpty() {
    PreviewSurface {
        ApprovalListTab(
            items = emptyList(),
            emptyTitle = "You're all caught up",
            emptySubtitle = "Nothing is waiting for your approval right now.",
            onOpenDetail = {},
            selectionMode = false,
            selectedIds = emptySet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}

/** EMPTY: the SAVED filter chip matched nothing — a distinct reason from "queue is empty". */
@PreviewLightDark
@Composable
fun PreviewApprovalsQueueSavedFilterEmpty() {
    PreviewSurface {
        ApprovalListTab(
            items = emptyList(),
            emptyTitle = "No saved conversations",
            emptySubtitle = "Approvals you save from the clarification chat will show up here.",
            onOpenDetail = {},
            selectionMode = false,
            selectedIds = emptySet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}

/** Selection mode with a subset checked, ready for the bulk approve/reject bar. */
@PreviewMatrix
@Composable
fun PreviewApprovalsQueueSelectionMode() {
    PreviewSurface {
        val pending = ApprovalsRepository.all.filter { it.status == ApprovalStatus.PENDING }
        ApprovalListTab(
            items = pending,
            emptyTitle = "You're all caught up",
            emptySubtitle = "Nothing is waiting for your approval right now.",
            onOpenDetail = {},
            selectionMode = true,
            selectedIds = pending.take(2).map { it.id }.toSet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}

// ── Team tab — filled/empty ──────────────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewApprovalsTeamFilled() {
    PreviewSurface {
        ApprovalListTab(
            items = ApprovalsRepository.teamItems,
            emptyTitle = "No team activity yet",
            emptySubtitle = "Approvals submitted by your team will appear here.",
            onOpenDetail = {},
            selectionMode = false,
            selectedIds = emptySet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}

@PreviewLightDark
@Composable
fun PreviewApprovalsTeamEmpty() {
    PreviewSurface {
        ApprovalListTab(
            items = emptyList(),
            emptyTitle = "No team activity yet",
            emptySubtitle = "Approvals submitted by your team will appear here.",
            onOpenDetail = {},
            selectionMode = false,
            selectedIds = emptySet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}

// ── My Requests tab — filled/empty ───────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewApprovalsMyRequestsFilled() {
    PreviewSurface {
        ApprovalListTab(
            items = ApprovalsRepository.myRequests,
            emptyTitle = "No requests yet",
            emptySubtitle = "Submit a mileage log, expense, or travel request and its approval status will show up here.",
            onOpenDetail = {},
            selectionMode = false,
            selectedIds = emptySet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}

@PreviewLightDark
@Composable
fun PreviewApprovalsMyRequestsEmpty() {
    PreviewSurface {
        ApprovalListTab(
            items = emptyList(),
            emptyTitle = "No requests yet",
            emptySubtitle = "Submit a mileage log, expense, or travel request and its approval status will show up here.",
            onOpenDetail = {},
            selectionMode = false,
            selectedIds = emptySet(),
            onLongPress = {},
            onToggleSelect = {},
        )
    }
}
