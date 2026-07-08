package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.RewardCardEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P5.3: the persisted scratch-card rewards store — see [RewardCardEntity]. */
@Dao
interface RewardCardDao {
    @Query("SELECT * FROM reward_cards ORDER BY grantedAtMs DESC")
    fun observeAll(): Flow<List<RewardCardEntity>>

    @Query("SELECT COUNT(*) FROM reward_cards")
    suspend fun count(): Int

    @Query("SELECT * FROM reward_cards WHERE id = :id LIMIT 1")
    suspend fun get(id: String): RewardCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<RewardCardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RewardCardEntity)
}
