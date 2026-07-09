package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.PaymentWalletEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P8.1: the persisted external payment-wallet list — see [PaymentWalletEntity]. */
@Dao
interface PaymentWalletDao {
    @Query("SELECT * FROM payment_wallets ORDER BY providerName ASC")
    fun observeAll(): Flow<List<PaymentWalletEntity>>

    @Query("SELECT COUNT(*) FROM payment_wallets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PaymentWalletEntity>)

    @Query("UPDATE payment_wallets SET isLinked = :isLinked, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun setLinked(
        id: String,
        isLinked: Boolean,
        updatedAtMs: Long,
    )
}
