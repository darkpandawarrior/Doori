package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.ReferralTxnEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P5.1: the persisted referral-transactions store — see [ReferralTxnEntity]. */
@Dao
interface ReferralTxnDao {
    @Query("SELECT * FROM referral_txns ORDER BY submittedAtMillis DESC")
    fun observeAll(): Flow<List<ReferralTxnEntity>>

    @Query("SELECT COUNT(*) FROM referral_txns")
    suspend fun count(): Int

    @Query("SELECT * FROM referral_txns WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ReferralTxnEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ReferralTxnEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReferralTxnEntity)
}
