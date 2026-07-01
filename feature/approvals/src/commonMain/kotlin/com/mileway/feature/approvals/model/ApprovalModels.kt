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

data class ClarificationMessage(
    val text: String,
    val isFromRequester: Boolean,
    val timestampMs: Long,
)
