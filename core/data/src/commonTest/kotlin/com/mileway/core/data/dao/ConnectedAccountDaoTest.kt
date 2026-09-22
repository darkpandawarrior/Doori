package com.mileway.core.data.dao

import com.mileway.core.data.model.db.ConnectedAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P6.6: exercises [ConnectedAccountDao]'s contract against an in-memory fake, mirroring
 * [NotificationDaoTest]'s pattern (`exportSchema=false` blocks `MigrationTestHelper` on the JVM —
 * see memory `miletracker-backlog-audit-v18` — so the schema/migration side is covered separately
 * by an instrumented test calling `MIGRATION_15_16.migrate()` directly).
 */
class ConnectedAccountDaoTest {
    private fun account(
        id: String,
        providerName: String,
        isConnected: Boolean = false,
        updatedAtMs: Long = 0L,
    ) = ConnectedAccountEntity(
        id = id,
        providerName = providerName,
        category = "Cabs",
        isConnected = isConnected,
        updatedAtMs = updatedAtMs,
    )

    @Test
    fun `upsertAll adds rows observable via observeAll`() =
        runTest {
            val dao = FakeConnectedAccountDao()

            dao.upsertAll(listOf(account("A1", "Uber for Business")))

            assertEquals(listOf("A1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `observeAll orders rows by providerName ascending`() =
        runTest {
            val dao = FakeConnectedAccountDao()
            dao.upsertAll(listOf(account("A1", "Zed Cabs"), account("A2", "Alpha Cabs")))

            assertEquals(listOf("Alpha Cabs", "Zed Cabs"), dao.snapshot().map { it.providerName })
        }

    @Test
    fun `count reflects the number of persisted rows`() =
        runTest {
            val dao = FakeConnectedAccountDao()
            assertEquals(0, dao.count())

            dao.upsertAll(listOf(account("A1", "Uber for Business"), account("A2", "Ola Corporate")))

            assertEquals(2, dao.count())
        }

    @Test
    fun `setConnected toggles only the targeted row`() =
        runTest {
            val dao = FakeConnectedAccountDao()
            dao.upsertAll(
                listOf(
                    account("A1", "Uber for Business", isConnected = false),
                    account("A2", "Ola Corporate", isConnected = false),
                ),
            )

            dao.setConnected("A1", isConnected = true, updatedAtMs = 5L)

            val rows = dao.snapshot().associateBy { it.id }
            assertTrue(rows.getValue("A1").isConnected)
            assertEquals(5L, rows.getValue("A1").updatedAtMs)
            assertEquals(false, rows.getValue("A2").isConnected)
        }
}

/** In-memory fake mirroring the semantics Room enforces for [ConnectedAccountDao]. */
private class FakeConnectedAccountDao : ConnectedAccountDao {
    private val rows = LinkedHashMap<String, ConnectedAccountEntity>()

    private val allRows = MutableStateFlow<List<ConnectedAccountEntity>>(emptyList())

    private fun flush() {
        allRows.value = rows.values.sortedBy { it.providerName }
    }

    fun snapshot(): List<ConnectedAccountEntity> = allRows.value

    override fun observeAll(): Flow<List<ConnectedAccountEntity>> = allRows.asStateFlow()

    override suspend fun count(): Int = rows.size

    override suspend fun upsertAll(entities: List<ConnectedAccountEntity>) {
        entities.forEach { rows[it.id] = it }
        flush()
    }

    override suspend fun setConnected(
        id: String,
        isConnected: Boolean,
        updatedAtMs: Long,
    ) {
        rows[id]?.let { rows[id] = it.copy(isConnected = isConnected, updatedAtMs = updatedAtMs) }
        flush()
    }
}
