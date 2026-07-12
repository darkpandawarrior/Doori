package com.mileway.feature.approvals.model

enum class ApprovalType { MILEAGE, EXPENSE, TRAVEL, ADVANCE }

enum class ApprovalStatus { PENDING, APPROVED, REJECTED }

data class ApprovalItem(
    val id: String,
    val type: ApprovalType,
    val requesterName: String,
    val summary: String,
    val amountRupees: Double,
    val status: ApprovalStatus,
    val timestampMs: Long,
    val policyViolation: Boolean = false,
)

/** PLAN_V28 P28.2/P28.3: a persistent clarification thread's ACTIVE/CLOSED lifecycle. */
enum class ClarificationRoomStatus { ACTIVE, CLOSED }

/**
 * PLAN_V28 P28.2: one persistent clarification thread tied to an [ApprovalItem] (FK [approvalId]).
 * Created lazily on first open (see `ClarificationRepository.getOrCreateRoom`) and persisted from
 * then on, fixing the previous reset-to-seed-on-every-open bug.
 */
data class ClarificationRoom(
    val roomId: String,
    val approvalId: String,
    val status: ClarificationRoomStatus,
    val participants: List<String>,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

data class ClarificationMessage(
    val id: String,
    val roomId: String,
    val senderId: String,
    val text: String,
    val isFromRequester: Boolean,
    val timestampMs: Long,
)

/** PLAN_V28 P28.4: local-only per-room triage state — see `ClarificationRoomMetaEntity`. */
data class ClarificationRoomMeta(
    val roomId: String,
    val isSaved: Boolean = false,
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val reminderAtMs: Long? = null,
)

/** PLAN_V28 P28.4: room-summary aggregation that backs the approvals nav-hub badge. */
data class ClarificationRoomSummary(
    val activeRooms: Int = 0,
    val totalUnread: Int = 0,
)
