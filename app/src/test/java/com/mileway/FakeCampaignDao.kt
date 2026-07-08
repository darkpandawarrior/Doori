package com.mileway

import com.mileway.core.data.dao.CampaignDao
import com.mileway.core.data.model.db.CampaignEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [CampaignDao] (PLAN_V24 P5.4) — lets JVM/Robolectric tests construct
 * `CampaignRepository`/`MarketingHubViewModel` (and render the HomeScreen strip) without a Room
 * instance. A relaxed mockk would return a null Flow the collectors dereference, so a real fake is
 * required.
 */
class FakeCampaignDao : CampaignDao {
    private val rows = MutableStateFlow<Map<String, CampaignEntity>>(emptyMap())

    override fun observeAll(): Flow<List<CampaignEntity>> = rows.map { it.values.sortedByDescending { row -> row.startedOnMs } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun get(id: String): CampaignEntity? = rows.value[id]

    override suspend fun upsertAll(entities: List<CampaignEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun upsert(entity: CampaignEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
