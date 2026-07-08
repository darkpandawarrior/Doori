package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.feature.profile.model.SavedPlaceType
import com.mileway.feature.profile.repository.SavedPlacesRepository
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
 * PLAN_V24 P3.4: covers [SavedPlacesViewModel]'s save/delete behavior, the blank-field and
 * bad-coordinate validation gates, and coordinate parsing — real Room-backed persistence (via a
 * fake [SavedPlaceDao]) exactly like [DelegationViewModelTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedPlacesViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(dao: FakeSavedPlaceDao = FakeSavedPlaceDao()) = SavedPlacesViewModel(SavedPlacesRepository(dao))

    @Test
    fun `save persists a new place visible in state`() =
        runTest {
            val vm = newViewModel()

            val accepted = vm.save(id = "", type = SavedPlaceType.HOME, label = "Home", address = "12 MG Road", latText = "", lngText = "")
            advanceUntilIdle()

            assertTrue(accepted)
            assertEquals(1, vm.state.value.places.size)
            assertEquals("Home", vm.state.value.places.single().label)
            assertEquals(SavedPlaceType.HOME, vm.state.value.places.single().type)
            assertNull(vm.state.value.places.single().lat)
        }

    @Test
    fun `save with valid coordinates keeps them`() =
        runTest {
            val vm = newViewModel()

            vm.save(id = "", type = SavedPlaceType.WORK, label = "Office", address = "Tower B", latText = "18.52", lngText = "73.85")
            advanceUntilIdle()

            assertEquals(18.52, vm.state.value.places.single().lat)
            assertEquals(73.85, vm.state.value.places.single().lng)
        }

    @Test
    fun `save is durable across process death via the same dao`() =
        runTest {
            val dao = FakeSavedPlaceDao()
            val vm = newViewModel(dao)
            vm.save(id = "", type = SavedPlaceType.OTHER, label = "Gym", address = "Baner", latText = "", lngText = "")
            advanceUntilIdle()

            val relaunched = newViewModel(dao)
            advanceUntilIdle()

            assertEquals(1, relaunched.state.value.places.size)
            assertEquals("Gym", relaunched.state.value.places.single().label)
        }

    @Test
    fun `save with a blank label surfaces a validation error and persists nothing`() =
        runTest {
            val vm = newViewModel()

            val accepted = vm.save(id = "", type = SavedPlaceType.HOME, label = "", address = "12 MG Road", latText = "", lngText = "")
            advanceUntilIdle()

            assertTrue(!accepted)
            assertTrue(vm.state.value.places.isEmpty())
            assertTrue(vm.state.value.submitError != null)
        }

    @Test
    fun `save with a partial or out-of-range coordinate is rejected`() =
        runTest {
            val vm = newViewModel()

            // lat present, lng blank -> partial pair rejected.
            assertTrue(!vm.save(id = "", type = SavedPlaceType.HOME, label = "Home", address = "A", latText = "18.5", lngText = ""))
            // out-of-range latitude rejected.
            assertTrue(!vm.save(id = "", type = SavedPlaceType.HOME, label = "Home", address = "A", latText = "120", lngText = "70"))
            advanceUntilIdle()

            assertTrue(vm.state.value.places.isEmpty())
        }

    @Test
    fun `editing an existing place updates in place rather than adding`() =
        runTest {
            val vm = newViewModel()
            vm.save(id = "", type = SavedPlaceType.HOME, label = "Home", address = "Old", latText = "", lngText = "")
            advanceUntilIdle()
            val id = vm.state.value.places.single().id

            vm.save(id = id, type = SavedPlaceType.HOME, label = "Home", address = "New address", latText = "", lngText = "")
            advanceUntilIdle()

            assertEquals(1, vm.state.value.places.size)
            assertEquals("New address", vm.state.value.places.single().address)
        }

    @Test
    fun `delete removes the place`() =
        runTest {
            val vm = newViewModel()
            vm.save(id = "", type = SavedPlaceType.HOME, label = "Home", address = "12 MG Road", latText = "", lngText = "")
            advanceUntilIdle()
            val id = vm.state.value.places.single().id

            vm.delete(id)
            advanceUntilIdle()

            assertTrue(vm.state.value.places.isEmpty())
        }

    @Test
    fun `clearSubmitError resets the error without touching persisted places`() =
        runTest {
            val vm = newViewModel()
            vm.save(id = "", type = SavedPlaceType.HOME, label = "", address = "", latText = "", lngText = "")
            advanceUntilIdle()
            assertTrue(vm.state.value.submitError != null)

            vm.clearSubmitError()

            assertNull(vm.state.value.submitError)
        }

    @Test
    fun `parseCoordinates enforces range and pairing`() {
        assertEquals(18.5 to 73.8, parseCoordinates("18.5", "73.8"))
        assertNull(parseCoordinates("18.5", ""))
        assertNull(parseCoordinates("", "73.8"))
        assertNull(parseCoordinates("abc", "73.8"))
        assertNull(parseCoordinates("91", "73.8"))
        assertNull(parseCoordinates("18.5", "181"))
    }
}

/** In-memory fake for [SavedPlaceDao] — mirrors the fake shape in [DelegationViewModelTest]. */
private class FakeSavedPlaceDao : SavedPlaceDao {
    private val rows = MutableStateFlow<Map<String, SavedPlaceEntity>>(emptyMap())

    override fun observeAll(): Flow<List<SavedPlaceEntity>> = rows.map { it.values.sortedByDescending { row -> row.updatedAtMs } }

    override suspend fun upsert(entity: SavedPlaceEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}
