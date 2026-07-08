package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.RewardCardDao
import com.mileway.core.data.model.db.RewardCardEntity
import com.mileway.core.data.rewards.RewardStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P5.3: covers [RewardsRepository]'s seed and the scratch (reveal + credit) behaviour.
 */
class RewardsRepositoryTest {
    private fun repo(dao: FakeRewardCardDao = FakeRewardCardDao()) = RewardsRepository(dao)

    @Test
    fun `seedIfEmpty seeds the cards once`() =
        runTest {
            val dao = FakeRewardCardDao()
            val r = repo(dao)
            r.seedIfEmpty()
            val first = r.observeAll().first().size
            r.seedIfEmpty()
            val second = r.observeAll().first().size

            assertEquals(5, first)
            assertEquals(5, second)
        }

    @Test
    fun `scratching an unscratched card reveals it and returns its credits`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            val granted = r.scratch("RWD-1")
            assertEquals(500, granted)
            val card = r.observeAll().first().single { it.id == "RWD-1" }
            assertEquals(RewardStatus.SCRATCHED, card.status)
        }

    @Test
    fun `scratching an already-scratched card grants nothing`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            // RWD-5 is seeded SCRATCHED.
            assertEquals(0, r.scratch("RWD-5"))
        }

    @Test
    fun `scratching an unknown card grants nothing`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            assertEquals(0, r.scratch("NOPE"))
        }
}

/** In-memory fake for [RewardCardDao] — mirrors the app-test fake shape. */
private class FakeRewardCardDao : RewardCardDao {
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
