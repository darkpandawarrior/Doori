package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.PaymentWalletDao
import com.mileway.core.data.model.db.PaymentWalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P8.1: covers [WalletRepository] — seed-once, link/unlink flag flips, and the ₹ balance
 * formatting shown once linked.
 */
class WalletRepositoryTest {
    private fun repo(dao: FakeWalletDao = FakeWalletDao()) = WalletRepository(dao)

    @Test
    fun `seedIfEmpty seeds the two wallets once`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            val first = r.observeAll().first().size
            r.seedIfEmpty()
            val second = r.observeAll().first().size

            assertEquals(2, first)
            assertEquals(2, second)
        }

    @Test
    fun `wallets start unlinked`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            assertTrue(r.observeAll().first().all { !it.isLinked })
        }

    @Test
    fun `setLinked true then false flips the persisted flag`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            val id = r.observeAll().first().first().id

            r.setLinked(id, true)
            assertTrue(r.observeAll().first().first { it.id == id }.isLinked)

            r.setLinked(id, false)
            assertFalse(r.observeAll().first().first { it.id == id }.isLinked)
        }

    @Test
    fun `balance is formatted as rupees with two decimals and grouping`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            val paytm = r.observeAll().first().first { it.providerName == "Paytm Wallet" }
            assertEquals("₹1,240.50", paytm.balanceLabel)
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
