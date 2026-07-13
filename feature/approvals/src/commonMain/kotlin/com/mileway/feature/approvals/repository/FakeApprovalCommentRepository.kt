package com.mileway.feature.approvals.repository

import com.mileway.feature.approvals.model.ApprovalComment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Fully offline, deterministic in-memory [ApprovalCommentRepository] — same tone as [FakeClarificationRepository]. */
class FakeApprovalCommentRepository : ApprovalCommentRepository {
    private val comments = MutableStateFlow<Map<String, List<ApprovalComment>>>(emptyMap())

    override fun observeComments(approvalId: String): Flow<List<ApprovalComment>> = comments.map { it[approvalId].orEmpty() }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addComment(
        approvalId: String,
        authorName: String,
        designation: String,
        message: String,
    ) {
        val comment =
            ApprovalComment(
                id = Uuid.random().toString(),
                approvalId = approvalId,
                authorName = authorName,
                designation = designation,
                message = message,
                timestampMs = 0L,
            )
        comments.value = comments.value + (approvalId to ((comments.value[approvalId].orEmpty()) + comment))
    }
}
