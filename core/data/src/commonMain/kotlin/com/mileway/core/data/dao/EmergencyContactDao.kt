package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P3.5: the persisted emergency-contacts store — see [EmergencyContactEntity]. */
@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY createdAtMs ASC")
    fun observeAll(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun delete(id: String)
}
