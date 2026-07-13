package com.mileway

import com.mileway.core.data.dao.ApprovalCommentDao
import com.mileway.core.data.model.db.ApprovalCommentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [ApprovalCommentDao] (PLAN_V28 P28.7) — same tone as [FakeClarificationDao]:
 * a relaxed mockk would hand back a null-backed Flow and crash ApprovalsViewModel's eager
 * `observeComments(...).collect` in `openDetail`.
 */
class FakeApprovalCommentDao : ApprovalCommentDao {
    private val comments = MutableStateFlow<Map<String, ApprovalCommentEntity>>(emptyMap())

    override fun observeComments(approvalId: String): Flow<List<ApprovalCommentEntity>> =
        comments.map { it.values.filter { c -> c.approvalId == approvalId }.sortedBy { c -> c.timestampMs } }

    override suspend fun insertComment(comment: ApprovalCommentEntity) {
        comments.value = comments.value + (comment.id to comment)
    }
}
