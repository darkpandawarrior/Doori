package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.dao.ConnectedAccountDao
import com.mileway.core.data.dao.PaymentWalletDao
import com.mileway.core.data.dao.PluginOverrideDao
import com.mileway.core.data.model.db.ConnectedAccountEntity
import com.mileway.core.data.model.db.PaymentWalletEntity
import com.mileway.core.data.model.db.PluginOverrideEntity
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.otp.OtpPurpose
import com.mileway.core.data.plugin.EmptyPersonaPresetProvider
import com.mileway.core.data.plugin.InMemoryPluginDebugForceSource
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.feature.profile.repository.ConnectedAccountsRepository
import com.mileway.feature.profile.repository.WalletRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLAN_V24 P8.1: the OTP gate on wallet linking — a wrong code must NOT link the wallet, only a
 * correct code does, and unlink reverts it. The OTP maths itself is covered by `LocalOtpEngineTest`;
 * this proves the ViewModel wires verify → persist correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectedAccountsViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private val otp = LocalOtpEngine()

    private fun newVm(): ConnectedAccountsViewModel {
        val registry =
            PluginRegistry(
                overrideDao = FakeOverrideDao(),
                activeAccount = FakeActiveAccount(),
                presets = EmptyPersonaPresetProvider,
                debugForce = InMemoryPluginDebugForceSource(),
            )
        return ConnectedAccountsViewModel(
            repository = ConnectedAccountsRepository(FakeConnectedDao()),
            walletRepository = WalletRepository(FakeWalletDao()),
            otpEngine = otp,
            pluginRegistry = registry,
        )
    }

    @Test
    fun `a wrong OTP does not link the wallet`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            val wallet = vm.state.value.wallets.first()

            vm.startLink(wallet)
            vm.onLinkCodeChange("000000") // deterministic wrong code (real code is a stable hash)
            advanceUntilIdle()

            assertTrue(vm.state.value.linkFlow?.wrongCode == true, "wrong code flags the flow, keeps it open")
            assertFalse(vm.state.value.wallets.first { it.id == wallet.id }.isLinked)
        }

    @Test
    fun `the correct OTP links the wallet and closes the flow`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            val wallet = vm.state.value.wallets.first()

            vm.startLink(wallet)
            vm.onLinkCodeChange(otp.codeFor(OtpPurpose.WALLET_LINK, wallet.mobile))
            advanceUntilIdle()

            assertNull(vm.state.value.linkFlow, "flow closes on success")
            assertTrue(vm.state.value.wallets.first { it.id == wallet.id }.isLinked)
        }

    @Test
    fun `unlink reverts a linked wallet`() =
        runTest {
            val vm = newVm()
            advanceUntilIdle()
            val wallet = vm.state.value.wallets.first()

            vm.startLink(wallet)
            vm.onLinkCodeChange(otp.codeFor(OtpPurpose.WALLET_LINK, wallet.mobile))
            advanceUntilIdle()
            assertTrue(vm.state.value.wallets.first { it.id == wallet.id }.isLinked)

            vm.unlink(wallet.id)
            advanceUntilIdle()
            assertFalse(vm.state.value.wallets.first { it.id == wallet.id }.isLinked)
        }
}

private class FakeActiveAccount : ActiveAccountSource {
    override val activeAccountId = MutableStateFlow<String?>("ACC-001")

    override suspend fun setActiveAccountId(accountId: String) {
        activeAccountId.value = accountId
    }
}

private class FakeOverrideDao : PluginOverrideDao {
    private val rows = MutableStateFlow<List<PluginOverrideEntity>>(emptyList())

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

private class FakeConnectedDao : ConnectedAccountDao {
    private val rows = MutableStateFlow<Map<String, ConnectedAccountEntity>>(emptyMap())

    override fun observeAll(): Flow<List<ConnectedAccountEntity>> = rows.map { it.values.sortedBy { row -> row.providerName } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsertAll(entities: List<ConnectedAccountEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun setConnected(
        id: String,
        isConnected: Boolean,
        updatedAtMs: Long,
    ) {
        val existing = rows.value[id] ?: return
        rows.value = rows.value + (id to existing.copy(isConnected = isConnected, updatedAtMs = updatedAtMs))
    }
}

private class FakeWalletDao : PaymentWalletDao {
    private val rows = MutableStateFlow<Map<String, PaymentWalletEntity>>(emptyMap())

    override fun observeAll(): Flow<List<PaymentWalletEntity>> = rows.map { it.values.sortedBy { row -> row.providerName } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsertAll(entities: List<PaymentWalletEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun setLinked(
        id: String,
        isLinked: Boolean,
        updatedAtMs: Long,
    ) {
        val existing = rows.value[id] ?: return
        rows.value = rows.value + (id to existing.copy(isLinked = isLinked, updatedAtMs = updatedAtMs))
    }
}
