package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.PerDiemRateEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [PerDiemRateEntity] rate card — a later lane's per-diem engine reads this. */
@Dao
interface PerDiemRateDao {
    @Query("SELECT * FROM per_diem_rates")
    fun observeAll(): Flow<List<PerDiemRateEntity>>

    @Query("SELECT * FROM per_diem_rates WHERE region = :region AND grade = :grade LIMIT 1")
    suspend fun get(
        region: String,
        grade: String,
    ): PerDiemRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PerDiemRateEntity)
}
