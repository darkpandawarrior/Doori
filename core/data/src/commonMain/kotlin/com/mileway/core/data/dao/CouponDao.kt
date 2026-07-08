package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.CouponEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P5.2: the persisted coupons store — see [CouponEntity]. */
@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons ORDER BY status ASC, title ASC")
    fun observeAll(): Flow<List<CouponEntity>>

    @Query("SELECT COUNT(*) FROM coupons")
    suspend fun count(): Int

    @Query("SELECT * FROM coupons WHERE code = :code COLLATE NOCASE LIMIT 1")
    suspend fun findByCode(code: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CouponEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CouponEntity)
}
