package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.DelegateAssignmentEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [DelegateAssignmentEntity] store — approver-to-approver authority grants. */
@Dao
interface DelegateAssignmentDao {
    @Query("SELECT * FROM delegate_assignments WHERE delegatorAccountId = :accountId ORDER BY createdAtMs DESC")
    fun observeByDelegator(accountId: String): Flow<List<DelegateAssignmentEntity>>

    @Query("SELECT * FROM delegate_assignments WHERE delegateAccountId = :accountId AND isActive = 1")
    suspend fun getActiveForDelegate(accountId: String): List<DelegateAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DelegateAssignmentEntity)

    @Query("DELETE FROM delegate_assignments WHERE id = :id")
    suspend fun delete(id: String)
}
