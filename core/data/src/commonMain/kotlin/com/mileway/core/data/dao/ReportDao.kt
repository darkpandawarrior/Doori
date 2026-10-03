package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.ReportEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [ReportEntity] store — see [com.mileway.core.data.claim.ReportRepository]. */
@Dao
interface ReportDao {
    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun get(id: String): ReportEntity?

    @Query("SELECT * FROM reports WHERE id = :id")
    fun observe(id: String): Flow<ReportEntity?>

    @Query("SELECT * FROM reports WHERE employeeId = :employeeId ORDER BY updatedAtMs DESC")
    fun observeByEmployee(employeeId: String): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun delete(id: String)
}
