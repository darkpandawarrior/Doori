package com.mileway.feature.approvals.repository

import com.mileway.core.data.dao.ApprovalCommentDao
import com.mileway.core.data.model.db.ApprovalCommentEntity
import com.mileway.feature.approvals.model.ApprovalComment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * PLAN_V28 P28.7: the approval-comments store — a permanent, non-interactive annotation thread per
 * approval, distinct from [ClarificationRepository]'s private, closable chat. Interface +
 * [RoomApprovalCommentRepository] (real, Room-backed) + [FakeApprovalCommentRepository] (in-memory,
 * for tests), same shape as [ClarificationRepository]/[RoomClarificationRepository].
 */
interface ApprovalCommentRepository {
    fun observeComments(approvalId: String): Flow<List<ApprovalComment>>

    suspend fun addComment(
        approvalId: String,
        authorName: String,
        designation: String,
        message: String,
    )
}

class RoomApprovalCommentRepository(
    private val dao: ApprovalCommentDao,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : ApprovalCommentRepository {
    override fun observeComments(approvalId: String): Flow<List<ApprovalComment>> =
        dao.observeComments(approvalId).map { comments -> comments.map { it.toDomain() } }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun addComment(
        approvalId: String,
        authorName: String,
        designation: String,
        message: String,
    ) {
        dao.insertComment(
            ApprovalCommentEntity(
                id = "comment_${Uuid.random()}",
                approvalId = approvalId,
                authorName = authorName,
                designation = designation,
                message = message,
                timestampMs = nowMs(),
            ),
        )
    }

    private fun ApprovalCommentEntity.toDomain() =
        ApprovalComment(
            id = id,
            approvalId = approvalId,
            authorName = authorName,
            designation = designation,
            message = message,
            timestampMs = timestampMs,
        )
}
