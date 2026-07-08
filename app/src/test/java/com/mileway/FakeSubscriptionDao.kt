package com.mileway

import com.mileway.core.data.dao.SubscriptionDao
import com.mileway.core.data.model.db.ActiveSubscriptionEntity
import com.mileway.core.data.model.db.SubscriptionPlanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [SubscriptionDao] (PLAN_V24 P6.2) — lets JVM/Robolectric tests construct
 * `SubscriptionRepository`/`SubscriptionViewModel` (which seeds on init) without a Room instance.
 */
class FakeSubscriptionDao : SubscriptionDao {
    private val plans = MutableStateFlow<Map<String, SubscriptionPlanEntity>>(emptyMap())
    private val active = MutableStateFlow<ActiveSubscriptionEntity?>(null)

    override fun observePlans(): Flow<List<SubscriptionPlanEntity>> = plans.map { it.values.sortedBy { row -> row.tierRank } }

    override suspend fun planCount(): Int = plans.value.size

    override suspend fun getPlan(id: String): SubscriptionPlanEntity? = plans.value[id]

    override suspend fun upsertPlans(entities: List<SubscriptionPlanEntity>) {
        plans.value = plans.value + entities.associateBy { it.id }
    }

    override fun observeActive(): Flow<ActiveSubscriptionEntity?> = active

    override suspend fun getActive(): ActiveSubscriptionEntity? = active.value

    override suspend fun upsertActive(entity: ActiveSubscriptionEntity) {
        active.value = entity
    }

    override suspend fun clearActive() {
        active.value = null
    }
}
