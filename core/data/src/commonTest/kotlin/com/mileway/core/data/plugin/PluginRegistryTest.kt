package com.mileway.core.data.plugin

import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P0.1 — the registry's resolution order is the plan's load-bearing wall, so it is
 * table-driven tested here: FORCED > USER > PRESET > DEFAULT, plus source reporting and
 * per-account isolation. Uses in-memory fakes (real DataStore/Room round-trips are exercised via
 * instrumentation, mirroring the [com.mileway.core.data.session.AccountBindingTest] convention).
 */
class PluginRegistryTest {
    private val account = "ACC-001"

    // "tracking" is a seeded CORE_MODULES TILE plugin, defaultOn = true.
    private val trackingId = "tracking"

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

    private class FakePresets(overrides: Map<String, String>) : PersonaPresetProvider {
        val state = MutableStateFlow(overrides)

        override fun presetOverrides(accountId: String?): Flow<Map<String, String>> = state
    }

    private fun registry(
        activeAccount: ActiveAccountSource = FakeActiveAccount(account),
        overrideDao: PluginOverrideDao = FakeOverrideDao(),
        presets: PersonaPresetProvider = EmptyPersonaPresetProvider,
        debugForce: PluginDebugForceSource = InMemoryPluginDebugForceSource(),
    ) = PluginRegistry(
        overrideDao = overrideDao,
        activeAccount = activeAccount,
        presets = presets,
        debugForce = debugForce,
    )

    @Test
    fun `default layer wins when nothing overrides`() =
        runTest {
            assertEquals(true, registry().observe(trackingId).first())
            assertEquals(PluginSource.DEFAULT, registry().observeSource(trackingId).first())
        }

    @Test
    fun `preset overrides default`() =
        runTest {
            val reg = registry(presets = FakePresets(mapOf(trackingId to "false")))
            assertEquals(false, reg.observe(trackingId).first())
            assertEquals(PluginSource.PRESET, reg.observeSource(trackingId).first())
        }

    @Test
    fun `user override beats preset`() =
        runTest {
            val dao = FakeOverrideDao().apply { rows.value = listOf(PluginOverrideEntity(account, trackingId, "true")) }
            val reg = registry(overrideDao = dao, presets = FakePresets(mapOf(trackingId to "false")))
            assertEquals(true, reg.observe(trackingId).first())
            assertEquals(PluginSource.USER, reg.observeSource(trackingId).first())
        }

    @Test
    fun `forced beats everything`() =
        runTest {
            val dao = FakeOverrideDao().apply { rows.value = listOf(PluginOverrideEntity(account, trackingId, "true")) }
            val force = InMemoryPluginDebugForceSource(mapOf(trackingId to "false"))
            val reg = registry(overrideDao = dao, presets = FakePresets(mapOf(trackingId to "true")), debugForce = force)
            assertEquals(false, reg.observe(trackingId).first())
            assertEquals(PluginSource.FORCED, reg.observeSource(trackingId).first())
        }

    @Test
    fun `user overrides are isolated per account`() =
        runTest {
            val dao = FakeOverrideDao().apply { rows.value = listOf(PluginOverrideEntity("ACC-OTHER", trackingId, "false")) }
            // Active account is ACC-001, so ACC-OTHER's override must not apply.
            val reg = registry(overrideDao = dao)
            assertEquals(true, reg.observe(trackingId).first())
            assertEquals(PluginSource.DEFAULT, reg.observeSource(trackingId).first())
        }

    @Test
    fun `setUserOverride then clear round-trips through the active account`() =
        runTest {
            val dao = FakeOverrideDao()
            val reg = registry(overrideDao = dao)

            reg.setUserOverride(trackingId, PluginValue.Bool(false))
            assertEquals(false, reg.observe(trackingId).first())

            reg.clearUserOverride(trackingId)
            assertEquals(true, reg.observe(trackingId).first())
        }

    @Test
    fun `unknown plugin id resolves to a safe off`() =
        runTest {
            assertEquals(false, registry().observe("does-not-exist").first())
        }
}
