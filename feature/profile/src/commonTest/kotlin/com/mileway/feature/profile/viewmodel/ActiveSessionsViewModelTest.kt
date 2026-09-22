package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.SessionDao
import com.mileway.core.data.model.db.SessionEntity
import com.mileway.feature.profile.repository.ActiveSessionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V22 P6.4: covers [ActiveSessionsViewModel]'s revoke/bulk-sign-out-all-others behavior and
 * the current-device revoke guard — real Room-backed persistence (via [ActiveSessionsRepository])
 * promoting `ProfileScreen`'s previous read-only `SessionsDialog`.
 *
 * `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`, so [Dispatchers.setMain] is
 * required here exactly as `DelegationViewModelTest` does for this module's `commonTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionsViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun seededDao(): FakeSessionDao =
        FakeSessionDao().apply {
            seed(
                listOf(
                    session("S1", isCurrent = true, lastActiveMillis = 3L),
                    session("S2", isCurrent = false, lastActiveMillis = 2L),
                    session("S3", isCurrent = false, lastActiveMillis = 1L),
                ),
            )
        }

    private fun session(
        id: String,
        isCurrent: Boolean,
        lastActiveMillis: Long,
    ) = SessionEntity(id = id, deviceName = "Device $id", platform = "Android 15", lastActiveMillis = lastActiveMillis, isCurrent = isCurrent)

    private fun newViewModel(dao: FakeSessionDao) = ActiveSessionsViewModel(ActiveSessionsRepository(dao))

    @Test
    fun `revoke removes the targeted session`() =
        runTest {
            val dao = seededDao()
            val vm = newViewModel(dao)
            advanceUntilIdle()

            vm.revoke("S2")
            advanceUntilIdle()

            assertEquals(
                listOf("S1", "S3"),
                vm.state.value.sessions
                    .map { it.id },
            )
        }

    @Test
    fun `revoke across process death is durable via the same dao instance`() =
        runTest {
            val dao = seededDao()
            val vm = newViewModel(dao)
            advanceUntilIdle()
            vm.revoke("S2")
            advanceUntilIdle()

            // A fresh ViewModel over the same (persisted) dao simulates process death/relaunch.
            val relaunched = newViewModel(dao)
            advanceUntilIdle()

            assertEquals(
                listOf("S1", "S3"),
                relaunched.state.value.sessions
                    .map { it.id },
            )
        }

    @Test
    fun `revoke on the current-device row is a no-op`() =
        runTest {
            val dao = seededDao()
            val vm = newViewModel(dao)
            advanceUntilIdle()

            vm.revoke("S1")
            advanceUntilIdle()

            assertEquals(
                listOf("S1", "S2", "S3"),
                vm.state.value.sessions
                    .map { it.id },
            )
        }

    @Test
    fun `revokeAllExceptCurrent leaves only the current device`() =
        runTest {
            val dao = seededDao()
            val vm = newViewModel(dao)
            advanceUntilIdle()

            vm.revokeAllExceptCurrent()
            advanceUntilIdle()

            val remaining = vm.state.value.sessions
            assertEquals(1, remaining.size)
            assertTrue(remaining.single().isCurrent)
            assertEquals("S1", remaining.single().id)
        }

    @Test
    fun `seedIfEmpty seeds the demo sessions exactly once on first launch`() =
        runTest {
            val dao = FakeSessionDao()
            val vm = newViewModel(dao)
            advanceUntilIdle()

            assertTrue(
                vm.state.value.sessions
                    .isNotEmpty(),
            )
            val seededCount = vm.state.value.sessions.size

            // A second ViewModel over the same dao must not double-seed.
            val relaunched = newViewModel(dao)
            advanceUntilIdle()

            assertEquals(seededCount, relaunched.state.value.sessions.size)
        }
}

/** In-memory fake for [SessionDao] — mirrors [SessionDaoTest]'s fake shape, plus a [seed] helper. */
private class FakeSessionDao : SessionDao {
    private val rows = MutableStateFlow<Map<String, SessionEntity>>(emptyMap())

    fun seed(sessions: List<SessionEntity>) {
        rows.value = rows.value + sessions.associateBy { it.id }
    }

    override fun observeAll(): Flow<List<SessionEntity>> = rows.map { it.values.sortedByDescending { row -> row.lastActiveMillis } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsertAll(sessions: List<SessionEntity>) {
        rows.value = rows.value + sessions.associateBy { it.id }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAllExceptCurrent() {
        rows.value = rows.value.filterValues { it.isCurrent }
    }
}
