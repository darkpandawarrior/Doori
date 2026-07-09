package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.plugin.EmptyPersonaPresetProvider
import com.mileway.core.data.plugin.InMemoryPluginDebugForceSource
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.ActiveAccountSource
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P10.6 — [ManagerReporteesViewModel] over a real [PluginRegistry] with in-memory fakes:
 * the `trackMileageManagerView` gate (off ⇒ disabled; on ⇒ enabled) and the deterministic seeded
 * summaries/trips.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagerReporteesViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private val account = "ACC-001"

    private class FakeActiveAccount(id: String?) : ActiveAccountSource {
        override val activeAccountId = MutableStateFlow(id)

        override suspend fun setActiveAccountId(accountId: String) {
            activeAccountId.value = accountId
        }
    }

    private class FakeOverrideDao(rows: List<PluginOverrideEntity> = emptyList()) : PluginOverrideDao {
        val state = MutableStateFlow(rows)

        override fun observeForAccount(accountId: String): Flow<List<PluginOverrideEntity>> = state.map { list -> list.filter { it.accountId == accountId } }

        override suspend fun upsert(entity: PluginOverrideEntity) {
            state.value = state.value.filterNot { it.accountId == entity.accountId && it.pluginId == entity.pluginId } + entity
        }

        override suspend fun delete(
            accountId: String,
            pluginId: String,
        ) {
            state.value = state.value.filterNot { it.accountId == accountId && it.pluginId == pluginId }
        }

        override suspend fun deleteForAccount(accountId: String) {
            state.value = state.value.filterNot { it.accountId == accountId }
        }
    }

    private fun newVm(dao: PluginOverrideDao = FakeOverrideDao()): ManagerReporteesViewModel {
        val registry =
            PluginRegistry(
                overrideDao = dao,
                activeAccount = FakeActiveAccount(account),
                presets = EmptyPersonaPresetProvider,
                debugForce = InMemoryPluginDebugForceSource(),
            )
        return ManagerReporteesViewModel(registry)
    }

    @Test
    fun `gate off keeps the view disabled`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            assertFalse(vm.state.value.enabled)
        }

    @Test
    fun `gate on enables the view and seeds four reportees`() =
        runTest {
            val dao = FakeOverrideDao(listOf(PluginOverrideEntity(account, "trackMileageManagerView", "true")))
            val vm = newVm(dao)
            advanceUntilIdle()
            assertTrue(vm.state.value.enabled)
            assertEquals(4, vm.state.value.summaries.size)
        }

    @Test
    fun `tripsFor returns the stable seeded list`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            val trips = vm.tripsFor("EMP-2101")
            assertTrue(trips.isNotEmpty())
            assertEquals(vm.tripsFor("EMP-2101"), trips)
        }
}
