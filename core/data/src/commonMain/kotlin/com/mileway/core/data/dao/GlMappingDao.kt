package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.GlMappingEntity
import kotlinx.coroutines.flow.Flow

/** L2: the persisted [GlMappingEntity] store — category -> GL account code. */
@Dao
interface GlMappingDao {
    @Query("SELECT * FROM gl_mappings")
    fun observeAll(): Flow<List<GlMappingEntity>>

    @Query("SELECT * FROM gl_mappings WHERE category = :category LIMIT 1")
    suspend fun get(category: String): GlMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GlMappingEntity)
}
