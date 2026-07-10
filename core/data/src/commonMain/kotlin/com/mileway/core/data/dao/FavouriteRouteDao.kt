package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.FavouriteRouteEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P12.8: the pinned favourite-routes store — see [FavouriteRouteEntity]. */
@Dao
interface FavouriteRouteDao {
    @Query("SELECT * FROM favourite_routes ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<FavouriteRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavouriteRouteEntity)

    @Query("SELECT * FROM favourite_routes WHERE id = :id LIMIT 1")
    suspend fun get(id: String): FavouriteRouteEntity?

    @Query("UPDATE favourite_routes SET name = :name WHERE id = :id")
    suspend fun rename(
        id: String,
        name: String,
    )

    @Query("DELETE FROM favourite_routes WHERE id = :id")
    suspend fun delete(id: String)
}
