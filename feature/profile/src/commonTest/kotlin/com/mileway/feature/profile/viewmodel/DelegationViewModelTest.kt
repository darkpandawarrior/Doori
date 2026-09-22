package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.DelegationDao
import com.mileway.core.data.model.db.DelegationEntity
import com.mileway.feature.profile.repository.DelegationRepository
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
 * PLAN_V22 P6.3: covers [DelegationViewModel]'s add/revoke/toggle behavior and the blank-name/
 * blank-scope validation gate — real Room-backed persistence (via [DelegationRepository]) replacing
 * `DelegationScreen`'s previous `mutableStateListOf` seed, which reset on navigation away.
 *
 * `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`, so [Dispatchers.setMain] is
 * required here exactly as `SwitchAccountViewModelTest` does for this module's `commonTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DelegationViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(dao: FakeDelegationDao = FakeDelegationDao()) = DelegationViewModel(DelegationRepository(dao))

    @Test
    fun `add persists a new delegation visible in state`() =
        runTest {
            val vm = newViewModel()

            vm.add(delegateName = "Priya Sharma", scope = "Mileage & Expense", expiresAtMillis = 1_800_000_000_000L)
            advanceUntilIdle()

            assertEquals(1, vm.state.value.delegations.size)
            assertEquals(
                "Priya Sharma",
                vm.state.value.delegations
                    .single()
                    .delegateName,
            )
            assertTrue(
                vm.state.value.delegations
                    .single()
                    .isActive,
            )
        }

    @Test
    fun `add across process death is durable via the same dao instance`() =
        runTest {
            val dao = FakeDelegationDao()
            val vm = newViewModel(dao)
            vm.add(delegateName = "Rahul Mehra", scope = "Travel only", expiresAtMillis = 1_800_000_000_000L)
            advanceUntilIdle()

            // A fresh ViewModel over the same (persisted) dao simulates process death/relaunch.
            val relaunched = newViewModel(dao)
            advanceUntilIdle()

            assertEquals(1, relaunched.state.value.delegations.size)
            assertEquals(
                "Rahul Mehra",
                relaunched.state.value.delegations
                    .single()
                    .delegateName,
            )
        }

    @Test
    fun `add with a blank delegate name surfaces a validation error and persists nothing`() =
        runTest {
            val vm = newViewModel()

            vm.add(delegateName = "", scope = "Mileage & Expense", expiresAtMillis = 1_800_000_000_000L)
            advanceUntilIdle()

            assertTrue(
                vm.state.value.delegations
                    .isEmpty(),
            )
            assertTrue(vm.state.value.submitError != null)
        }

    @Test
    fun `add with a blank scope surfaces a validation error and persists nothing`() =
        runTest {
            val vm = newViewModel()

            vm.add(delegateName = "Priya Sharma", scope = "", expiresAtMillis = 1_800_000_000_000L)
            advanceUntilIdle()

            assertTrue(
                vm.state.value.delegations
                    .isEmpty(),
            )
            assertTrue(vm.state.value.submitError != null)
        }

    @Test
    fun `clearSubmitError resets the error without touching persisted delegations`() =
        runTest {
            val vm = newViewModel()
            vm.add(delegateName = "", scope = "", expiresAtMillis = 0L)
            advanceUntilIdle()
            assertTrue(vm.state.value.submitError != null)

            vm.clearSubmitError()

            assertNull(vm.state.value.submitError)
        }

    @Test
    fun `revoke removes the delegation`() =
        runTest {
            val vm = newViewModel()
            vm.add(delegateName = "Priya Sharma", scope = "Mileage & Expense", expiresAtMillis = 1_800_000_000_000L)
            advanceUntilIdle()
            val id =
                vm.state.value.delegations
                    .single()
                    .id

            vm.revoke(id)
            advanceUntilIdle()

            assertTrue(
                vm.state.value.delegations
                    .isEmpty(),
            )
        }

    @Test
    fun `setActive toggles the row without removing it`() =
        runTest {
            val vm = newViewModel()
            vm.add(delegateName = "Priya Sharma", scope = "Mileage & Expense", expiresAtMillis = 1_800_000_000_000L)
            advanceUntilIdle()
            val id =
                vm.state.value.delegations
                    .single()
                    .id

            vm.setActive(id, false)
            advanceUntilIdle()

            assertEquals(1, vm.state.value.delegations.size)
            assertEquals(
                false,
                vm.state.value.delegations
                    .single()
                    .isActive,
            )
        }
}

/** In-memory fake for [DelegationDao] — mirrors [DelegationDaoTest]'s fake shape. */
private class FakeDelegationDao : DelegationDao {
    private val rows = MutableStateFlow<Map<String, DelegationEntity>>(emptyMap())

    override fun observeAll(): Flow<List<DelegationEntity>> = rows.map { it.values.sortedBy { row -> row.createdAt } }

    override suspend fun upsert(entity: DelegationEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun setActive(
        id: String,
        isActive: Boolean,
    ) {
        val existing = rows.value[id] ?: return
        rows.value = rows.value + (id to existing.copy(isActive = isActive))
    }
}
