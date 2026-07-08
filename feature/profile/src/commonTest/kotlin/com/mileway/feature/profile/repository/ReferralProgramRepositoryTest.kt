package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.ReferralTxnDao
import com.mileway.core.data.model.db.ReferralTxnEntity
import com.mileway.core.data.referral.ReferralStatus
import com.mileway.core.data.review.SimulatedReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P5.1: covers [ReferralProgramRepository]'s seed, status mapping, and the
 * SimulatedReviewEngine-driven PENDING→SUCCESS/FAILED resolution.
 */
class ReferralProgramRepositoryTest {
    private fun repo(
        dao: FakeReferralTxnDao = FakeReferralTxnDao(),
        delay: Long = SimulatedReviewEngine.DEFAULT_SIM_DELAY_MILLIS,
    ) = ReferralProgramRepository(dao, SimulatedReviewEngine(simDelayMillis = delay))

    @Test
    fun `seedIfEmpty seeds the ledger once with the expected status split`() =
        runTest {
            val dao = FakeReferralTxnDao()
            val r = repo(dao)

            r.seedIfEmpty()
            val first = r.observeAll().first()
            r.seedIfEmpty()
            val second = r.observeAll().first()

            assertEquals(5, first.size)
            assertEquals(5, second.size)
            assertEquals(1, first.count { it.status == ReferralStatus.SUCCESS })
            assertEquals(3, first.count { it.status == ReferralStatus.PENDING })
            assertEquals(1, first.count { it.status == ReferralStatus.FAILED })
        }

    @Test
    fun `pending referees resolve to success when the review passes`() =
        runTest {
            val dao = FakeReferralTxnDao()
            val r = repo(dao, delay = 0L)
            r.seedIfEmpty()

            r.resolveReviewablePending()

            val txns = r.observeAll().first()
            assertEquals(0, txns.count { it.status == ReferralStatus.PENDING })
            assertEquals(4, txns.count { it.status == ReferralStatus.SUCCESS })
            assertEquals(1, txns.count { it.status == ReferralStatus.FAILED })
        }

    @Test
    fun `a reject marker in the task message resolves to failed`() =
        runTest {
            val dao = FakeReferralTxnDao()
            dao.upsert(
                ReferralTxnEntity(
                    id = "REF-001", refereeName = "R", status = ReferralStatus.PENDING.name,
                    taskMessage = "reject: fraud check", processedMoney = 0.0, processedCredits = 0,
                    userNumRides = 0, nextTargetRides = 1, nextTargetMoney = 0.0, nextTargetCredits = 0,
                    submittedAtMillis = 0L,
                ),
            )
            val r = repo(dao, delay = 0L)

            r.resolveReviewablePending()

            assertEquals(ReferralStatus.FAILED, r.observeAll().first().single().status)
        }

    @Test
    fun `target progress reflects rides over the next target`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            val priya = r.observeAll().first().single { it.refereeName == "Priya Sharma" }
            assertEquals(0.4f, priya.targetProgress)
        }
}

/** In-memory fake for [ReferralTxnDao] — mirrors the app-test fake shape. */
private class FakeReferralTxnDao : ReferralTxnDao {
    private val rows = MutableStateFlow<Map<String, ReferralTxnEntity>>(emptyMap())

    override fun observeAll(): Flow<List<ReferralTxnEntity>> = rows.map { it.values.sortedByDescending { row -> row.submittedAtMillis } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun get(id: String): ReferralTxnEntity? = rows.value[id]

    override suspend fun upsertAll(entities: List<ReferralTxnEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun upsert(entity: ReferralTxnEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
