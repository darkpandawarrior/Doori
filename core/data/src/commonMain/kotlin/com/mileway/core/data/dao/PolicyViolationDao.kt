package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mileway.core.data.model.db.PolicyViolationEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [PolicyViolationEntity] store — a later lane's policy engine is the writer. */
@Dao
interface PolicyViolationDao {
    @Query("SELECT * FROM policy_violations WHERE reportId = :reportId ORDER BY createdAtMs ASC")
    suspend fun getByReport(reportId: String): List<PolicyViolationEntity>

    @Query("SELECT * FROM policy_violations WHERE reportId = :reportId ORDER BY createdAtMs ASC")
    fun observeByReport(reportId: String): Flow<List<PolicyViolationEntity>>

    @Insert
    suspend fun insert(entity: PolicyViolationEntity)

    @Query("DELETE FROM policy_violations WHERE reportId = :reportId")
    suspend fun deleteByReport(reportId: String)
}
