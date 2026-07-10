package com.mileway.core.data.engagement

import com.mileway.core.data.dao.TourProgressDao
import com.mileway.core.data.model.db.TourProgressEntity
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * PLAN_V24 P12.5 — the training tour's pure state machine ([tourAdvance]/[tourSkip]/[tourRestart])
 * plus its persistence through [TourRepository] over a fake DAO.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TourRepositoryTest {
    // --- Pure state machine ---------------------------------------------------------------------

    @Test
    fun `advance walks the steps in order and completes on the last`() {
        var state = TourState()
        assertEquals(TourStep.INTRO, state.step)
        val expected =
            listOf(
                TourStep.START,
                TourStep.LIVE_HUD,
                TourStep.PAUSE,
                TourStep.STOP,
                TourStep.CLASSIFY,
                TourStep.SUBMIT,
                TourStep.COMPLETE,
            )
        expected.forEach { step ->
            state = tourAdvance(state)
            assertEquals(step, state.step)
        }
        assertEquals(TourStatus.COMPLETED, state.status)
    }

    @Test
    fun `advance is a no-op once completed`() {
        var state = TourState(step = TourStep.SUBMIT)
        state = tourAdvance(state) // -> COMPLETE / COMPLETED
        val terminal = tourAdvance(state)
        assertEquals(state, terminal)
    }

    @Test
    fun `skip is terminal and keeps the current step`() {
        val skipped = tourSkip(TourState(step = TourStep.PAUSE))
        assertEquals(TourStatus.SKIPPED, skipped.status)
        assertEquals(TourStep.PAUSE, skipped.step)
        // Skipping again, or advancing after a skip, changes nothing.
        assertEquals(skipped, tourSkip(skipped))
        assertEquals(skipped, tourAdvance(skipped))
    }

    @Test
    fun `restart returns to the first in-progress step`() {
        assertEquals(TourState(TourStep.INTRO, TourStatus.IN_PROGRESS), tourRestart())
    }

    // --- Persistence ----------------------------------------------------------------------------

    @Test
    fun `advance persists the step and completion flips the completed flow`() =
        runTest {
            val repo = repo()
            assertFalse(repo.observeCompleted().first())

            repeat(TourStep.COMPLETE.ordinal) { repo.advance() }

            assertEquals(TourStep.COMPLETE, repo.observe().first().step)
            assertTrue(repo.observeCompleted().first())
        }

    @Test
    fun `skip persists the skipped outcome`() =
        runTest {
            val repo = repo()
            repo.advance() // START
            repo.skip()

            val state = repo.observe().first()
            assertEquals(TourStatus.SKIPPED, state.status)
            assertEquals(TourStep.START, state.step)
            assertFalse(repo.observeCompleted().first())
        }

    @Test
    fun `restart resets a completed tour back to the start`() =
        runTest {
            val repo = repo()
            repeat(TourStep.COMPLETE.ordinal) { repo.advance() }
            assertTrue(repo.observeCompleted().first())

            repo.restart()

            val state = repo.observe().first()
            assertEquals(TourStep.INTRO, state.step)
            assertEquals(TourStatus.IN_PROGRESS, state.status)
        }

    private fun repo(): TourRepository = TourRepository(FakeTourProgressDao(), FakeActiveAccount("ACC-1"))

    private class FakeActiveAccount(id: String?) : ActiveAccountSource {
        override val activeAccountId = MutableStateFlow(id)

        override suspend fun setActiveAccountId(accountId: String) {
            activeAccountId.value = accountId
        }
    }

    private class FakeTourProgressDao : TourProgressDao {
        private val rows = MutableStateFlow<Map<String, TourProgressEntity>>(emptyMap())

        override fun observe(accountId: String): Flow<TourProgressEntity?> = rows.map { it[accountId] }

        override suspend fun get(accountId: String): TourProgressEntity? = rows.value[accountId]

        override suspend fun upsert(entity: TourProgressEntity) {
            rows.value = rows.value + (entity.accountId to entity)
        }
    }
}
