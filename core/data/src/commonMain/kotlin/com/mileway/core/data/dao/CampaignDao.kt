package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.CampaignEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P5.4: the persisted campaigns store — see [CampaignEntity]. */
@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY startedOnMs DESC")
    fun observeAll(): Flow<List<CampaignEntity>>

    @Query("SELECT COUNT(*) FROM campaigns")
    suspend fun count(): Int

    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CampaignEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CampaignEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CampaignEntity)
}
