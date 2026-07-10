package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.VehicleAuditEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P12.6: per-vehicle self-audit history store — see [VehicleAuditEntity]. */
@Dao
interface VehicleAuditDao {
    @Query("SELECT * FROM vehicle_audits WHERE vehicleId = :vehicleId ORDER BY submittedAtMs DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<VehicleAuditEntity>>

    @Query("SELECT COUNT(*) FROM vehicle_audits")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VehicleAuditEntity)

    @Query("DELETE FROM vehicle_audits WHERE vehicleId = :vehicleId")
    suspend fun deleteForVehicle(vehicleId: String)
}
