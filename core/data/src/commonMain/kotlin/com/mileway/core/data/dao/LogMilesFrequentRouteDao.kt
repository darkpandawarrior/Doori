package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.LogMilesFrequentRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogMilesFrequentRouteDao {
    @Query("SELECT * FROM log_miles_frequent_routes")
    fun observeAllRoutes(): Flow<List<LogMilesFrequentRouteEntity>>

    @Query("SELECT * FROM log_miles_frequent_routes WHERE routeKey = :routeKey LIMIT 1")
    suspend fun getByKey(routeKey: String): LogMilesFrequentRouteEntity?

    @Query("SELECT * FROM log_miles_frequent_routes")
    suspend fun getAllRoutes(): List<LogMilesFrequentRouteEntity>

    @Query("DELETE FROM log_miles_frequent_routes WHERE routeKey IN (:routeKeys)")
    suspend fun deleteByKeys(routeKeys: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(route: LogMilesFrequentRouteEntity)
}
