package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.ClaimLineEntity
import kotlinx.coroutines.flow.Flow

/**
 * L2: the persisted [ClaimLineEntity] store. [getBySourceTripId] is the idempotency check
 * [LegacyMileageBackfillWorker] and L4's auto-draft both call before inserting — the UNIQUE index
 * on `sourceTripId` would reject a duplicate insert anyway, but checking first avoids relying on a
 * caught constraint-violation exception as control flow.
 */
@Dao
interface ClaimLineDao {
    @Query("SELECT * FROM claim_lines WHERE reportId = :reportId ORDER BY createdAtMs ASC")
    fun observeByReport(reportId: String): Flow<List<ClaimLineEntity>>

    @Query("SELECT * FROM claim_lines WHERE reportId = :reportId ORDER BY createdAtMs ASC")
    suspend fun getByReport(reportId: String): List<ClaimLineEntity>

    @Query("SELECT * FROM claim_lines WHERE sourceTripId = :sourceTripId LIMIT 1")
    suspend fun getBySourceTripId(sourceTripId: String): ClaimLineEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ClaimLineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClaimLineEntity)

    @Query("DELETE FROM claim_lines WHERE id = :id")
    suspend fun delete(id: String)
}
