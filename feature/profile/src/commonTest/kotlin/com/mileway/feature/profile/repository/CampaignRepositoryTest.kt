package com.mileway.feature.profile.repository

import com.mileway.core.data.campaign.CampaignRepository
import com.mileway.core.data.campaign.CampaignStatus
import com.mileway.core.data.dao.CampaignDao
import com.mileway.core.data.model.db.CampaignEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P5.4: covers [CampaignRepository]'s seed and the one-shot interest capture.
 */
class CampaignRepositoryTest {
    private fun repo(dao: FakeCampaignDao = FakeCampaignDao()) = CampaignRepository(dao)

    @Test
    fun `seedIfEmpty seeds the campaigns once`() =
        runTest {
            val dao = FakeCampaignDao()
            val r = repo(dao)
            r.seedIfEmpty()
            val first = r.observeAll().first().size
            r.seedIfEmpty()
            val second = r.observeAll().first().size

            assertEquals(5, first)
            assertEquals(5, second)
        }

    @Test
    fun `captureInterest flips the one-shot flag`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            assertFalse(r.observeAll().first().single { it.id == "CMP-A" }.interestCaptured)

            r.captureInterest("CMP-A")

            assertTrue(r.observeAll().first().single { it.id == "CMP-A" }.interestCaptured)
        }

    @Test
    fun `seeded statuses map across live, upcoming and ended`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            val campaigns = r.observeAll().first()

            assertTrue(campaigns.any { it.status == CampaignStatus.LIVE })
            assertTrue(campaigns.any { it.status == CampaignStatus.UPCOMING })
            assertTrue(campaigns.any { it.status == CampaignStatus.ENDED })
        }
}

/** In-memory fake for [CampaignDao] — mirrors the app-test fake shape. */
private class FakeCampaignDao : CampaignDao {
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
