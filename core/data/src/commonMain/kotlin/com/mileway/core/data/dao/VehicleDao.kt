package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mileway.core.data.model.db.VehicleEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P11.2: the persisted multi-vehicle garage store — see [VehicleEntity]. */
@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAtMs ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY createdAtMs ASC")
    suspend fun getAll(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun get(id: String): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): VehicleEntity?

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE vehicles SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE vehicles SET isActive = 1 WHERE id = :id")
    suspend fun markActive(id: String)

    /** Exactly one active vehicle: clears every flag then sets [id]. */
    @Transaction
    suspend fun setActive(id: String) {
        clearActive()
        markActive(id)
    }
}
