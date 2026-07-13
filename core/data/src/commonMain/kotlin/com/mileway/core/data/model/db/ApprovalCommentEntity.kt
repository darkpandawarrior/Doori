package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PLAN_V28 P28.7: one row in an approval's permanent comment thread — a sibling table to
 * `clarification_rooms`/`clarification_messages`, not a child of them. Comments are non-interactive
 * annotations (append-only, no lifecycle), distinct from the private, closable clarification chat.
 * No foreign key to a `clarification_rooms`/approvals table: `approvalId` matches
 * [com.mileway.feature.approvals.model.ApprovalItem.id], which lives in feature:approvals' own
 * in-memory `ApprovalsRepository`, not Room.
 */
@Entity(tableName = "approval_comments", indices = [Index("approvalId")])
data class ApprovalCommentEntity(
    @PrimaryKey val id: String,
    val approvalId: String,
    val authorName: String,
    val designation: String,
    val message: String,
    val timestampMs: Long,
)
