package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.DeletionRequestEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P7.1: the single account-deletion request row. */
@Dao
interface DeletionRequestDao {
    @Query("SELECT * FROM deletion_request WHERE id = 'current' LIMIT 1")
    fun observe(): Flow<DeletionRequestEntity?>

    @Query("SELECT * FROM deletion_request WHERE id = 'current' LIMIT 1")
    suspend fun get(): DeletionRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeletionRequestEntity)

    @Query("DELETE FROM deletion_request")
    suspend fun clear()
}
