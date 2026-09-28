package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.PeriodLockEntity

/** L2: the persisted [PeriodLockEntity] store — no row for a period means "open". */
@Dao
interface PeriodLockDao {
    @Query("SELECT * FROM period_locks WHERE periodKey = :periodKey LIMIT 1")
    suspend fun get(periodKey: String): PeriodLockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PeriodLockEntity)

    @Query("DELETE FROM period_locks WHERE periodKey = :periodKey")
    suspend fun unlock(periodKey: String)
}
