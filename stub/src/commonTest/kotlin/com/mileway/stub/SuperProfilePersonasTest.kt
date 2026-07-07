package com.mileway.stub

import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.plugin.InMemoryPluginDebugForceSource
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.ActiveAccountSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V24 P0.2 — the persona presets are only useful if they actually move the registry, so this
 * exercises the real [PluginRegistry] wired with [StubPersonaPresetProvider]: each account resolves
 * its persona's mix, and the guest fallback applies. Core-module ids are the ones with live
 * descriptors today, so assertions target those.
 */
class SuperProfilePersonasTest {
    private class FakeActiveAccount(id: String?) : ActiveAccountSource {
        override val activeAccountId = MutableStateFlow(id)

        override suspend fun setActiveAccountId(accountId: String) {
            activeAccountId.value = accountId
        }
    }

    private class EmptyOverrideDao : PluginOverrideDao {
        override fun observeForAccount(accountId: String): Flow<List<PluginOverrideEntity>> = MutableStateFlow(emptyList())

        override suspend fun upsert(entity: PluginOverrideEntity) = Unit

        override suspend fun delete(
            accountId: String,
            pluginId: String,
        ) = Unit

        override suspend fun deleteForAccount(accountId: String) = Unit
    }

    private fun registryFor(accountId: String?) =
        PluginRegistry(
            overrideDao = EmptyOverrideDao(),
            activeAccount = FakeActiveAccount(accountId),
            presets = StubPersonaPresetProvider(),
            debugForce = InMemoryPluginDebugForceSource(),
        )

    @Test
    fun `gig driver hides expenses but keeps tracking`() =
        runTest {
            val reg = registryFor("ACC-003")
            assertEquals(false, reg.observe("expenses").first(), "Gig Driver preset disables expenses")
            assertEquals(true, reg.observe("tracking").first(), "tracking stays on for every persona")
        }

    @Test
    fun `corporate keeps expenses on via default`() =
        runTest {
            val reg = registryFor("ACC-001")
            assertEquals(true, reg.observe("expenses").first(), "Corporate does not disable expenses")
        }

    @Test
    fun `consumer hides approvals`() =
        runTest {
            val reg = registryFor("ACC-002")
            assertEquals(false, reg.observe("approvals").first(), "Super-App Consumer preset disables approvals")
        }

    @Test
    fun `guest (null account) shows only tracking and logging`() =
        runTest {
            val reg = registryFor(null)
            assertEquals(true, reg.observe("tracking").first())
            assertEquals(true, reg.observe("logging").first())
            assertEquals(false, reg.observe("cards").first(), "Minimal Guest disables cards")
            assertEquals(false, reg.observe("agent").first(), "Minimal Guest disables agent")
        }

    @Test
    fun `every persona binds and overrides flatten to raw strings`() =
        runTest {
            assertEquals(4, SuperProfilePersonas.all.size)
            val gig = SuperProfilePersonas.forAccount("ACC-003")
            assertTrue(gig.overrides()["verificationCentreEnabled"] == "true")
            assertEquals(SuperProfilePersonas.MinimalGuest, SuperProfilePersonas.forAccount("ACC-UNKNOWN"))
        }
}
