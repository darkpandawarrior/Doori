package com.mileway.core.data.settings

import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.plugin.EmptyPersonaPresetProvider
import com.mileway.core.data.plugin.InMemoryPluginDebugForceSource
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P10.3 guard: the registry-backed abnormal-detection source must project the plugin
 * DEFAULT layer to exactly the shipped `AbnormalDetectionConfig` baseline (numbers duplicated here
 * because core:data can't depend on feature/tracking). If a descriptor default drifts, live
 * tracking math silently changes — this test fails first.
 */
class RegistryAbnormalDetectionSourceTest {
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

    private fun registry(dao: PluginOverrideDao = FakeOverrideDao()) =
        PluginRegistry(
            overrideDao = dao,
            activeAccount = FakeActiveAccount("ACC-001"),
            presets = EmptyPersonaPresetProvider,
            debugForce = InMemoryPluginDebugForceSource(),
        )

    @Test
    fun `no overrides projects the shipped baseline thresholds`() =
        runTest {
            val source = RegistryAbnormalDetectionSource(registry())
            val o = source.overrides.first()

            assertEquals(2.5, o.walkingMaxMps)
            assertEquals(7.0, o.cyclingMaxMps)
            assertEquals(1.2, o.stationarySpeedMps)
            assertEquals(1.5, o.movementHistoryMps)
            assertEquals(2.0, o.walkingJitterM)
            assertEquals(3.0, o.cyclingJitterM)
            assertEquals(5.0, o.drivingJitterM)
            assertEquals(1.2, o.stationaryJitterM)
            assertEquals(5, o.speedHistorySize)
            assertEquals(5_000.0, o.spikeHardGateM)
            assertEquals(10_000.0, o.gapMaxDistanceM)
            assertEquals(30L, o.gapMinSec)
            assertEquals(300L, o.gap5mSec)
            assertEquals(3_600L, o.gap1hSec)
            assertEquals(21_600L, o.gap6hSec)
            assertEquals(150.0, o.gapTier5mMps)
            assertEquals(100.0, o.gapTier1hMps)
            assertEquals(60.0, o.gapTier6hMps)
        }

    @Test
    fun `a user override on one knob is projected, the rest stay baseline`() =
        runTest {
            val dao = FakeOverrideDao()
            dao.upsert(PluginOverrideEntity("ACC-001", RegistryAbnormalDetectionSource.KEY_DRIVING_JITTER_M, "8.0"))
            val source = RegistryAbnormalDetectionSource(registry(dao))

            val o = source.overrides.first()
            assertEquals(8.0, o.drivingJitterM)
            assertEquals(2.0, o.walkingJitterM)
        }
}
