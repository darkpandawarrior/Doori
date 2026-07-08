package com.mileway

import com.mileway.core.data.dao.RewardCardDao
import com.mileway.core.data.model.db.RewardCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [RewardCardDao] (PLAN_V24 P5.3) — lets JVM/Robolectric tests construct
 * `RewardsRepository`/`RewardsViewModel` without a Room instance. A relaxed mockk would return a
 * null Flow the ViewModel's `init` collector dereferences, so a real fake is required.
 */
class FakeRewardCardDao : RewardCardDao {
    private val rows = MutableStateFlow<Map<String, RewardCardEntity>>(emptyMap())

    override fun observeAll(): Flow<List<RewardCardEntity>> = rows.map { it.values.sortedByDescending { row -> row.grantedAtMs } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun get(id: String): RewardCardEntity? = rows.value[id]

    override suspend fun upsertAll(entities: List<RewardCardEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun upsert(entity: RewardCardEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
