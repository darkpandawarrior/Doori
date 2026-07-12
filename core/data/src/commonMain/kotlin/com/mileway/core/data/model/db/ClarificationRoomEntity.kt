package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V28 P28.2: one persistent thread per approval — `status` is `ClarificationRoomStatus.name`
 * (ACTIVE/CLOSED, see feature:approvals' domain enum). Replaces the hardcoded seed thread that
 * [com.mileway.feature.approvals.repository.ApprovalsRepository] used to reset on every screen
 * open; a room is created lazily on first open and persists across app restarts from then on.
 * `participantsCsv` is a plain comma-joined id list — no json lib needed for a handful of ids.
 */
@Entity(tableName = "clarification_rooms")
data class ClarificationRoomEntity(
    @PrimaryKey val roomId: String,
    val approvalId: String,
    val status: String,
    val participantsCsv: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
