package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.ActiveSubscriptionEntity
import com.mileway.core.data.model.db.SubscriptionPlanEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V24 P6.2: the persisted plans catalogue + the single active-subscription row. */
@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription_plans ORDER BY tierRank ASC")
    fun observePlans(): Flow<List<SubscriptionPlanEntity>>

    @Query("SELECT COUNT(*) FROM subscription_plans")
    suspend fun planCount(): Int

    @Query("SELECT * FROM subscription_plans WHERE id = :id LIMIT 1")
    suspend fun getPlan(id: String): SubscriptionPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlans(entities: List<SubscriptionPlanEntity>)

    @Query("SELECT * FROM active_subscription WHERE id = 'current' LIMIT 1")
    fun observeActive(): Flow<ActiveSubscriptionEntity?>

    @Query("SELECT * FROM active_subscription WHERE id = 'current' LIMIT 1")
    suspend fun getActive(): ActiveSubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActive(entity: ActiveSubscriptionEntity)

    @Query("DELETE FROM active_subscription")
    suspend fun clearActive()
}
