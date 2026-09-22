package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.SupportTicketDao
import com.mileway.core.data.model.db.SupportTicketEntity
import com.mileway.feature.profile.repository.SupportTicketRepository
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLAN_V22 P6.8: covers [SupportTicketViewModel]'s submit/list behavior and the blank-subject/
 * blank-body validation gate — real Room-backed persistence (via [SupportTicketRepository])
 * replacing `HelpScreen`'s previous fire-and-forget snackbar-only "Contact Support"/"Report a Bug"
 * taps.
 *
 * `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`, so [Dispatchers.setMain] is
 * required here exactly as `DelegationViewModelTest` does for this module's `commonTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupportTicketViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(dao: FakeSupportTicketDao = FakeSupportTicketDao()) = SupportTicketViewModel(SupportTicketRepository(dao))

    @Test
    fun `submit persists a new ticket visible in state`() =
        runTest {
            val vm = newViewModel()

            vm.submit(subject = "Can't submit expense", body = "The submit button does nothing")
            advanceUntilIdle()

            assertEquals(1, vm.state.value.tickets.size)
            assertEquals(
                "Can't submit expense",
                vm.state.value.tickets
                    .single()
                    .subject,
            )
        }

    @Test
    fun `submit across process death is durable via the same dao instance`() =
        runTest {
            val dao = FakeSupportTicketDao()
            val vm = newViewModel(dao)
            vm.submit(subject = "GPS drift", body = "Route looks noisy on rural roads")
            advanceUntilIdle()

            // A fresh ViewModel over the same (persisted) dao simulates process death/relaunch.
            val relaunched = newViewModel(dao)
            advanceUntilIdle()

            assertEquals(1, relaunched.state.value.tickets.size)
            assertEquals(
                "GPS drift",
                relaunched.state.value.tickets
                    .single()
                    .subject,
            )
        }

    @Test
    fun `submit with a blank subject surfaces a validation error and persists nothing`() =
        runTest {
            val vm = newViewModel()

            vm.submit(subject = "", body = "Something is broken")
            advanceUntilIdle()

            assertTrue(
                vm.state.value.tickets
                    .isEmpty(),
            )
            assertTrue(vm.state.value.submitError != null)
        }

    @Test
    fun `submit with a blank body surfaces a validation error and persists nothing`() =
        runTest {
            val vm = newViewModel()

            vm.submit(subject = "Bug report", body = "")
            advanceUntilIdle()

            assertTrue(
                vm.state.value.tickets
                    .isEmpty(),
            )
            assertTrue(vm.state.value.submitError != null)
        }

    @Test
    fun `clearSubmitError resets the error without touching persisted tickets`() =
        runTest {
            val vm = newViewModel()
            vm.submit(subject = "", body = "")
            advanceUntilIdle()
            assertTrue(vm.state.value.submitError != null)

            vm.clearSubmitError()

            assertNull(vm.state.value.submitError)
        }
}

/** In-memory fake for [SupportTicketDao] — mirrors [SupportTicketDaoTest]'s fake shape. */
private class FakeSupportTicketDao : SupportTicketDao {
    private val rows = MutableStateFlow<Map<String, SupportTicketEntity>>(emptyMap())

    override fun observeAll(): Flow<List<SupportTicketEntity>> = rows.map { it.values.sortedByDescending { row -> row.createdAtMs } }

    override suspend fun upsert(entity: SupportTicketEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
