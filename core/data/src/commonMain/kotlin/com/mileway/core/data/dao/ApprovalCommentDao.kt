package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.ApprovalCommentEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V28 P28.7: the persisted approval-comments store — see [ApprovalCommentEntity]. */
@Dao
interface ApprovalCommentDao {
    @Query("SELECT * FROM approval_comments WHERE approvalId = :approvalId ORDER BY timestampMs ASC")
    fun observeComments(approvalId: String): Flow<List<ApprovalCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: ApprovalCommentEntity)
}
