package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mileway.core.data.model.db.ApprovalStepEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [ApprovalStepEntity] store — one report's approval chain, ordered by stepIndex. */
@Dao
interface ApprovalStepDao {
    @Query("SELECT * FROM approval_steps WHERE reportId = :reportId ORDER BY stepIndex ASC")
    fun observeByReport(reportId: String): Flow<List<ApprovalStepEntity>>

    @Query("SELECT * FROM approval_steps WHERE reportId = :reportId ORDER BY stepIndex ASC")
    suspend fun getByReport(reportId: String): List<ApprovalStepEntity>

    @Insert
    suspend fun insert(entity: ApprovalStepEntity)

    @Query("DELETE FROM approval_steps WHERE reportId = :reportId")
    suspend fun deleteByReport(reportId: String)
}
