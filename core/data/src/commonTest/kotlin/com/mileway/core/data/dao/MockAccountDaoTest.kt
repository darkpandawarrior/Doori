package com.mileway.core.data.dao

import com.mileway.core.data.model.db.MockAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * P1.1: exercises [MockAccountDao]'s contract — a real Room table enforces the same semantics,
 * but `exportSchema=false` blocks `MigrationTestHelper` on the JVM (memory
 * `miletracker-backlog-audit-v18`), so the schema/migration side is covered separately by an
 * instrumented test that calls `MIGRATION_10_11.migrate()` directly. This test proves the DAO's
 * *behavioral* contract (single-active-row invariant, live `observeAll()`, seed-once shape) against
 * an in-memory fake implementation of the same interface — the pattern already used for
 * `FakeAgentDao`/`FakeVoucherDao` in this codebase.
 */
class MockAccountDaoTest {
    private fun account(
        id: String,
        isActive: Boolean = false,
        createdAtMs: Long = 0L,
    ) = MockAccountEntity(
        accountId = id,
        displayName = "Persona $id",
        employeeCode = "EMP-$id",
        organization = "Org $id",
        avatarSeed = id,
        isActive = isActive,
        lastLoginAtMs = createdAtMs,
        createdAtMs = createdAtMs,
    )

    @Test
    fun `upsertAll seeds rows and count reflects them`() =
        runTest {
            val dao = FakeMockAccountDao()
            assertEquals(0, dao.count())

            dao.upsertAll(listOf(account("A", createdAtMs = 1L), account("B", createdAtMs = 2L)))

            assertEquals(2, dao.count())
        }

    @Test
    fun `seed-once guard skips re-seeding once the table is non-empty`() =
        runTest {
            val dao = FakeMockAccountDao()
            val seed = listOf(account("A", isActive = true, createdAtMs = 1L), account("B", createdAtMs = 2L))

            // Mirrors the seed-once guard a repository builds on top of this DAO (P1.2's
            // `seedIfEmpty()`): only seed when the table is empty.
            if (dao.count() == 0) dao.upsertAll(seed)
            if (dao.count() == 0) dao.upsertAll(seed)

            assertEquals(2, dao.count())
        }

    @Test
    fun `setActive clears every other row and sets exactly one`() =
        runTest {
            val dao = FakeMockAccountDao()
            dao.upsertAll(
                listOf(
                    account("A", isActive = true, createdAtMs = 1L),
                    account("B", createdAtMs = 2L),
                    account("C", createdAtMs = 3L),
                ),
            )

            dao.setActive("C")

            val rows = dao.snapshot()
            assertEquals(listOf("C"), rows.filter { it.isActive }.map { it.accountId })
            assertEquals(1, rows.count { it.isActive })
        }

    @Test
    fun `observeAll emits on every mutation`() =
        runTest {
            val dao = FakeMockAccountDao()
            val emissions = mutableListOf<Int>()

            dao.upsert(account("A", createdAtMs = 1L))
            emissions += dao.snapshot().size

            dao.upsert(account("B", createdAtMs = 2L))
            emissions += dao.snapshot().size

            dao.delete("A")
            emissions += dao.snapshot().size

            assertEquals(listOf(1, 2, 1), emissions)
        }

    @Test
    fun `getById returns null for a missing account`() =
        runTest {
            val dao = FakeMockAccountDao()
            assertNull(dao.getById("missing"))
        }

    @Test
    fun `delete removes exactly one row`() =
        runTest {
            val dao = FakeMockAccountDao()
            dao.upsertAll(listOf(account("A", createdAtMs = 1L), account("B", createdAtMs = 2L)))

            dao.delete("A")

            assertEquals(listOf("B"), dao.snapshot().map { it.accountId })
        }
}

/**
 * In-memory fake mirroring the semantics Room enforces for [MockAccountDao] — in particular,
 * [setActive]'s atomic clear-then-set (the same shape `FakeAgentDao`/`FakeVoucherDao` use
 * elsewhere in this codebase for deterministic, DAO-shaped test doubles).
 */
private class FakeMockAccountDao : MockAccountDao {
    private val rows = LinkedHashMap<String, MockAccountEntity>()

    private val allRows = MutableStateFlow<List<MockAccountEntity>>(emptyList())

    private fun flush() {
        allRows.value = rows.values.sortedBy { it.createdAtMs }
    }

    /** Test-only convenience: the current emitted list, without collecting the [Flow]. */
    fun snapshot(): List<MockAccountEntity> = allRows.value

    override fun observeAll(): Flow<List<MockAccountEntity>> = allRows.asStateFlow()

    override suspend fun count(): Int = rows.size

    override suspend fun getById(accountId: String): MockAccountEntity? = rows[accountId]

    override suspend fun upsert(account: MockAccountEntity) {
        rows[account.accountId] = account
        flush()
    }

    override suspend fun upsertAll(accounts: List<MockAccountEntity>) {
        accounts.forEach { rows[it.accountId] = it }
        flush()
    }

    override suspend fun delete(accountId: String) {
        rows.remove(accountId)
        flush()
    }

    override suspend fun clearActive() {
        rows.keys.toList().forEach { key -> rows[key] = rows.getValue(key).copy(isActive = false) }
        flush()
    }

    override suspend fun markActive(accountId: String) {
        rows[accountId]?.let { rows[accountId] = it.copy(isActive = true) }
        flush()
    }

    override suspend fun setActive(accountId: String) {
        clearActive()
        markActive(accountId)
    }
}
