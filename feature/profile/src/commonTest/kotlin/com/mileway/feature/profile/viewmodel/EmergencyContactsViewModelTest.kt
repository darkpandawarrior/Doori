package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.EmergencyContactDao
import com.mileway.core.data.emergency.EmergencyContactsRepository
import com.mileway.core.data.model.db.EmergencyContactEntity
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
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * PLAN_V24 P3.5: covers [EmergencyContactsViewModel]'s save/delete, the blank-name / invalid-phone
 * gates, phone normalization, and the 5-contact cap — real Room-backed persistence (via a fake
 * [EmergencyContactDao] + the shared [EmergencyContactsRepository]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyContactsViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    // Monotonic clock so 5 rapid saves mint distinct ids (the real clock could collide within a
    // millisecond and REPLACE rows, flaking the cap test).
    private fun newViewModel(dao: FakeEmergencyContactDao = FakeEmergencyContactDao()) =
        EmergencyContactsViewModel(EmergencyContactsRepository(dao, IncrementingClock()))

    @Test
    fun `save persists a valid contact with a normalized number`() =
        runTest {
            val vm = newViewModel()

            val accepted = vm.save(id = "", name = "Priya", phone = "098765 43210", countryCode = "+91")
            advanceUntilIdle()

            assertTrue(accepted)
            assertEquals(1, vm.state.value.contacts.size)
            assertEquals("Priya", vm.state.value.contacts.single().name)
            // leading 0 dropped, non-digits stripped -> 10 digits.
            assertEquals("9876543210", vm.state.value.contacts.single().phoneNo)
        }

    @Test
    fun `save with a blank name surfaces a validation error and persists nothing`() =
        runTest {
            val vm = newViewModel()

            val accepted = vm.save(id = "", name = "", phone = "9876543210", countryCode = "+91")
            advanceUntilIdle()

            assertTrue(!accepted)
            assertTrue(vm.state.value.contacts.isEmpty())
            assertTrue(vm.state.value.submitError != null)
        }

    @Test
    fun `save with a short phone is rejected`() =
        runTest {
            val vm = newViewModel()

            val accepted = vm.save(id = "", name = "Priya", phone = "12345", countryCode = "+91")
            advanceUntilIdle()

            assertTrue(!accepted)
            assertTrue(vm.state.value.contacts.isEmpty())
        }

    @Test
    fun `editing an existing contact updates in place`() =
        runTest {
            val vm = newViewModel()
            vm.save(id = "", name = "Priya", phone = "9876543210", countryCode = "+91")
            advanceUntilIdle()
            val id = vm.state.value.contacts.single().id

            vm.save(id = id, name = "Priya Sharma", phone = "9876543210", countryCode = "+91")
            advanceUntilIdle()

            assertEquals(1, vm.state.value.contacts.size)
            assertEquals("Priya Sharma", vm.state.value.contacts.single().name)
        }

    @Test
    fun `the sixth contact is rejected at the cap`() =
        runTest {
            val vm = newViewModel()
            repeat(5) { i ->
                vm.save(id = "", name = "C$i", phone = "98765432$i$i", countryCode = "+91")
                advanceUntilIdle()
            }
            assertEquals(5, vm.state.value.contacts.size)
            assertTrue(vm.state.value.isAtCapacity)

            vm.save(id = "", name = "Sixth", phone = "9000000000", countryCode = "+91")
            advanceUntilIdle()

            assertEquals(5, vm.state.value.contacts.size)
            assertTrue(vm.state.value.submitError != null)
        }

    @Test
    fun `delete removes the contact`() =
        runTest {
            val vm = newViewModel()
            vm.save(id = "", name = "Priya", phone = "9876543210", countryCode = "+91")
            advanceUntilIdle()
            val id = vm.state.value.contacts.single().id

            vm.delete(id)
            advanceUntilIdle()

            assertTrue(vm.state.value.contacts.isEmpty())
        }

    @Test
    fun `clearSubmitError resets the error without touching persisted contacts`() =
        runTest {
            val vm = newViewModel()
            vm.save(id = "", name = "", phone = "", countryCode = "+91")
            advanceUntilIdle()
            assertTrue(vm.state.value.submitError != null)

            vm.clearSubmitError()

            assertNull(vm.state.value.submitError)
        }
}

/** Monotonic clock: each [now] is one millisecond later, so minted contact ids never collide. */
private class IncrementingClock(private var ms: Long = 1_700_000_000_000L) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(ms++)
}

/** In-memory fake for [EmergencyContactDao] — mirrors [DelegationViewModelTest]'s fake shape. */
private class FakeEmergencyContactDao : EmergencyContactDao {
    private val rows = MutableStateFlow<Map<String, EmergencyContactEntity>>(emptyMap())

    override fun observeAll(): Flow<List<EmergencyContactEntity>> = rows.map { it.values.sortedBy { row -> row.createdAtMs } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsert(entity: EmergencyContactEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}
