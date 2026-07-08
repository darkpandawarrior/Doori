package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P3.4: the persisted saved-places store — see [SavedPlaceEntity]. */
@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<SavedPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun delete(id: String)
}
