package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.DestinationModeEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P11.3: the per-account head-home destination store — see [DestinationModeEntity]. */
@Dao
interface DestinationModeDao {
    @Query("SELECT * FROM destination_mode WHERE accountId = :accountId LIMIT 1")
    fun observe(accountId: String): Flow<DestinationModeEntity?>

    @Query("SELECT * FROM destination_mode WHERE accountId = :accountId LIMIT 1")
    suspend fun get(accountId: String): DestinationModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DestinationModeEntity)
}
