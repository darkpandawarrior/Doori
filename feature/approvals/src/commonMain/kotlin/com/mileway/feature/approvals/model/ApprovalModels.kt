package com.mileway.feature.approvals.model

import com.mileway.core.ai.model.DocumentAnalysis

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
    // PLAN_V28 P28.6: display header + a picked core:media attachment's local URI (folds in the
    // deferred V26 P-STR.1 attach surface). All optional.
    val senderName: String? = null,
    val senderRole: String? = null,
    val attachmentUrl: String? = null,
)

/** PLAN_V28 P28.6: computed-locally message-delivery indicator — never persisted (see `ChatBubble`). */
enum class DeliveryState { SENT, DELIVERED, SEEN }

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

/**
 * PLAN_V28 P28.7: one row in an approval's permanent, non-interactive comment thread — distinct
 * from [ClarificationMessage]'s private, closable back-and-forth chat. Comments are an append-only
 * audit trail (no edit/delete), backed by a sibling `approval_comments` Room table.
 */
data class ApprovalComment(
    val id: String,
    val approvalId: String,
    val authorName: String,
    val designation: String,
    val message: String,
    val timestampMs: Long,
)

/**
 * PLAN_V28 P28.8: a structured, multi-flag audit report distinct from [ApprovalItem.policyViolation]'s
 * single boolean — mock-populated (no OCR/merchant-verification backend exists yet, see PLAN_V28
 * §2 out-of-scope), but [receiptVerified] reads from a real [DocumentAnalysis] when the approval's
 * receipt was actually scanned via core:ai's DocumentIntelligence.
 */
data class AuditFlags(
    val receiptVerified: Boolean,
    val merchantVerified: Boolean,
    val flagged: Boolean,
    val violations: List<String>,
    val rejectedReason: String?,
)

/** Mock heuristic: ADVANCE requests have no receipt to verify, everything else "has one" and it reads clean. */
private fun mockReceiptVerified(item: ApprovalItem): Boolean = item.type != ApprovalType.ADVANCE

/**
 * Derives [AuditFlags] for [item]. Pass [receiptAnalysis] when core:ai actually scanned this
 * approval's receipt (e.g. via the media capture flow) — its [DocumentAnalysis.overallConfidence]
 * then backs [AuditFlags.receiptVerified] instead of the mock heuristic. No caller wires a real
 * scan into approvals yet, so every current call site passes `null`.
 */
fun ApprovalItem.toAuditFlags(receiptAnalysis: DocumentAnalysis? = null): AuditFlags {
    val receiptVerified = receiptAnalysis?.let { it.overallConfidence >= 0.7f } ?: mockReceiptVerified(this)
    val violations =
        buildList {
            if (policyViolation) add("Amount exceeds the policy limit for ${type.name.lowercase()} claims")
        }
    return AuditFlags(
        receiptVerified = receiptVerified,
        merchantVerified = !policyViolation,
        flagged = policyViolation,
        violations = violations,
        rejectedReason = if (status == ApprovalStatus.REJECTED) "Rejected by approver" else null,
    )
}

/**
 * PLAN_V28 P28.9: per-item gating for the detail scaffold's action bar — derived purely from
 * [ApprovalType]/state (never from a legacy insight-key string, see PLAN_V28 §2 out-of-scope).
 * [effectiveStatus] lets a caller pass a locally-resolved status (e.g. a just-tapped
 * Approve/Reject) instead of [ApprovalItem.status].
 */
data class DetailActionFlags(
    val canWithdraw: Boolean,
    val canEditDistance: Boolean,
    val requiresAck: Boolean,
)

fun ApprovalItem.toDetailActionFlags(effectiveStatus: ApprovalStatus = status): DetailActionFlags {
    val isOwnRequest = requesterName == "Me"
    val isPending = effectiveStatus == ApprovalStatus.PENDING
    return DetailActionFlags(
        canWithdraw = isOwnRequest && isPending,
        canEditDistance = isOwnRequest && isPending && type == ApprovalType.MILEAGE,
        requiresAck = policyViolation && isPending,
    )
}
