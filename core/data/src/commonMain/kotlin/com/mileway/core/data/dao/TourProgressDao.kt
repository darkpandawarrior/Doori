package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.TourProgressEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P12.5: the per-account training-tour progress store — see [TourProgressEntity]. */
@Dao
interface TourProgressDao {
    @Query("SELECT * FROM tour_progress WHERE accountId = :accountId LIMIT 1")
    fun observe(accountId: String): Flow<TourProgressEntity?>

    @Query("SELECT * FROM tour_progress WHERE accountId = :accountId LIMIT 1")
    suspend fun get(accountId: String): TourProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TourProgressEntity)
}
