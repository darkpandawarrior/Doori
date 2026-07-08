package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.DeletionRequestDao
import com.mileway.core.data.lifecycle.DeletionRequestRepository
import com.mileway.core.data.lifecycle.DeletionStatus
import com.mileway.core.data.model.db.DeletionRequestEntity
import com.mileway.core.data.review.SimulatedReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private class MutableClock(var ms: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(ms)
}

/**
 * PLAN_V24 P7.1: covers [DeletionRequestRepository]'s state machine — request, cancel, and the
 * time-gated REQUESTED→PROCESSING advance.
 */
class DeletionRequestRepositoryTest {
    private fun setup(startMs: Long = 1_000L): Triple<DeletionRequestRepository, FakeDeletionRequestDao, MutableClock> {
        val dao = FakeDeletionRequestDao()
        val clock = MutableClock(startMs)
        val repo = DeletionRequestRepository(dao, SimulatedReviewEngine(clock), clock)
        return Triple(repo, dao, clock)
    }

    @Test
    fun `request moves NONE to REQUESTED`() =
        runTest {
            val (repo, _, _) = setup()
            assertEquals(DeletionStatus.NONE, repo.observe().first().status)

            repo.request("changing jobs")
            val state = repo.observe().first()

            assertEquals(DeletionStatus.REQUESTED, state.status)
            assertEquals("changing jobs", state.reason)
        }

    @Test
    fun `cancel clears a pending request`() =
        runTest {
            val (repo, _, _) = setup()
            repo.request(null)

            repo.cancel()

            assertEquals(DeletionStatus.NONE, repo.observe().first().status)
        }

    @Test
    fun `advance is a no-op before the review delay elapses`() =
        runTest {
            val (repo, _, _) = setup()
            repo.request(null)

            val processing = repo.advance()

            assertEquals(false, processing)
            assertEquals(DeletionStatus.REQUESTED, repo.observe().first().status)
        }

    @Test
    fun `advance moves REQUESTED to PROCESSING once reviewed`() =
        runTest {
            val (repo, _, clock) = setup()
            repo.request(null)

            clock.ms += 7_000L // past the 6s sim-review delay
            val processing = repo.advance()

            assertEquals(true, processing)
            assertEquals(DeletionStatus.PROCESSING, repo.observe().first().status)
        }

    @Test
    fun `advance on an empty store does nothing`() =
        runTest {
            val (repo, _, _) = setup()
            assertEquals(false, repo.advance())
        }
}

/** In-memory single-row fake for [DeletionRequestDao]. */
private class FakeDeletionRequestDao : DeletionRequestDao {
    private val row = MutableStateFlow<DeletionRequestEntity?>(null)

    override fun observe(): Flow<DeletionRequestEntity?> = row

    override suspend fun get(): DeletionRequestEntity? = row.value

    override suspend fun upsert(entity: DeletionRequestEntity) {
        row.value = entity
    }

    override suspend fun clear() {
        row.value = null
    }
}
