package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.plugin.EmptyPersonaPresetProvider
import com.mileway.core.data.plugin.InMemoryPluginDebugForceSource
import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.data.plugin.PersonaSummary
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
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
import kotlin.test.assertTrue

/**
 * PLAN_V24 P0.3: covers [PluginManagerViewModel] over a real [PluginRegistry] with in-memory
 * fakes — the master-page interactions that actually mutate the registry: toggle→override,
 * apply-persona, reset, and the 7-tap experimental unlock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PluginManagerViewModelTest {
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

    private class FakeOverrideDao : PluginOverrideDao {
        val rows = MutableStateFlow<List<PluginOverrideEntity>>(emptyList())

        override fun observeForAccount(accountId: String): Flow<List<PluginOverrideEntity>> = rows.map { list -> list.filter { it.accountId == accountId } }

        override suspend fun upsert(entity: PluginOverrideEntity) {
            rows.value = rows.value.filterNot { it.accountId == entity.accountId && it.pluginId == entity.pluginId } + entity
        }

        override suspend fun delete(
            accountId: String,
            pluginId: String,
        ) {
            rows.value = rows.value.filterNot { it.accountId == accountId && it.pluginId == pluginId }
        }

        override suspend fun deleteForAccount(accountId: String) {
            rows.value = rows.value.filterNot { it.accountId == accountId }
        }
    }

    private class OnePersonaProvider : PersonaPresetProvider {
        override fun presetOverrides(accountId: String?): Flow<Map<String, String>> = MutableStateFlow(emptyMap())

        override fun availablePersonas(): List<PersonaSummary> =
            listOf(PersonaSummary("guest", "persona_minimal_guest_name", "persona_minimal_guest_desc", mapOf("cards" to "false")))
    }

    private fun newVm(
        dao: PluginOverrideDao = FakeOverrideDao(),
        presets: PersonaPresetProvider = EmptyPersonaPresetProvider,
    ): PluginManagerViewModel {
        val registry =
            PluginRegistry(
                overrideDao = dao,
                activeAccount = FakeActiveAccount(account),
                presets = presets,
                debugForce = InMemoryPluginDebugForceSource(),
            )
        return PluginManagerViewModel(registry, presets)
    }

    @Test
    fun `loads the full catalog on init`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            assertEquals(30, vm.state.value.plugins.size, "13 CORE + 4 AUTH + 7 ONBOARDING + 4 PROFILE + 1 TRACKING + 1 VERIFICATION")
            assertTrue(vm.state.value.plugins.all { it.source.name == "DEFAULT" })
        }

    @Test
    fun `toggle writes a USER override reflected in state`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()

            vm.onAction(PluginManagerAction.SetToggle("cards", false))
            advanceUntilIdle()

            val cards = vm.state.value.plugins.single { it.descriptor.id == "cards" }
            assertEquals(PluginValue.Bool(false), cards.value)
            assertEquals("USER", cards.source.name)
        }

    @Test
    fun `apply persona with clear applies the persona mix`() =
        runTest {
            val presets = OnePersonaProvider()
            val vm = newVm(presets = presets)
            advanceUntilIdle()

            vm.onAction(PluginManagerAction.ApplyPersona(presets.availablePersonas().single(), clearFirst = true))
            advanceUntilIdle()

            val cards = vm.state.value.plugins.single { it.descriptor.id == "cards" }
            assertEquals(PluginValue.Bool(false), cards.value)
        }

    @Test
    fun `reset clears user overrides back to default`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            vm.onAction(PluginManagerAction.SetToggle("cards", false))
            advanceUntilIdle()

            vm.onAction(PluginManagerAction.ResetToPreset)
            advanceUntilIdle()

            val cards = vm.state.value.plugins.single { it.descriptor.id == "cards" }
            assertEquals(PluginValue.Bool(true), cards.value, "back to descriptor default (on)")
            assertEquals("DEFAULT", cards.source.name)
        }

    @Test
    fun `seven version taps unlock the experimental section`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            assertEquals(false, vm.state.value.experimentalUnlocked)

            repeat(7) { vm.onAction(PluginManagerAction.VersionRowTap) }
            advanceUntilIdle()

            assertTrue(vm.state.value.experimentalUnlocked)
        }
}
