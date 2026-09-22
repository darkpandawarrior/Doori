package com.mileway.core.data.dao

import com.mileway.core.data.model.db.DelegationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P6.3: exercises [DelegationDao]'s contract against an in-memory fake, mirroring
 * [MockAccountDaoTest]'s pattern (`exportSchema=false` blocks `MigrationTestHelper` on the JVM —
 * see memory `miletracker-backlog-audit-v18` — so the schema/migration side is covered separately
 * by an instrumented test calling `MIGRATION_12_13.migrate()` directly).
 */
class DelegationDaoTest {
    private fun delegation(
        id: String,
        isActive: Boolean = true,
        createdAt: Long = 0L,
    ) = DelegationEntity(
        id = id,
        delegateName = "Delegate $id",
        scope = "Mileage & Expense",
        expiresAtMillis = 1_800_000_000_000L,
        isActive = isActive,
        createdAt = createdAt,
    )

    @Test
    fun `upsert adds a row observable via observeAll`() =
        runTest {
            val dao = FakeDelegationDao()

            dao.upsert(delegation("D1", createdAt = 1L))

            assertEquals(listOf("D1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `observeAll orders rows by createdAt ascending`() =
        runTest {
            val dao = FakeDelegationDao()
            dao.upsert(delegation("D2", createdAt = 2L))
            dao.upsert(delegation("D1", createdAt = 1L))

            assertEquals(listOf("D1", "D2"), dao.snapshot().map { it.id })
        }

    @Test
    fun `delete removes exactly one row`() =
        runTest {
            val dao = FakeDelegationDao()
            dao.upsert(delegation("D1", createdAt = 1L))
            dao.upsert(delegation("D2", createdAt = 2L))

            dao.delete("D1")

            assertEquals(listOf("D2"), dao.snapshot().map { it.id })
        }

    @Test
    fun `setActive toggles only the targeted row`() =
        runTest {
            val dao = FakeDelegationDao()
            dao.upsert(delegation("D1", isActive = true, createdAt = 1L))
            dao.upsert(delegation("D2", isActive = true, createdAt = 2L))

            dao.setActive("D1", false)

            val rows = dao.snapshot().associateBy { it.id }
            assertTrue(rows.getValue("D2").isActive)
            assertEquals(false, rows.getValue("D1").isActive)
        }
}

/** In-memory fake mirroring the semantics Room enforces for [DelegationDao]. */
private class FakeDelegationDao : DelegationDao {
    private val rows = LinkedHashMap<String, DelegationEntity>()

    private val allRows = MutableStateFlow<List<DelegationEntity>>(emptyList())

    private fun flush() {
        allRows.value = rows.values.sortedBy { it.createdAt }
    }

    fun snapshot(): List<DelegationEntity> = allRows.value

    override fun observeAll(): Flow<List<DelegationEntity>> = allRows.asStateFlow()

    override suspend fun upsert(entity: DelegationEntity) {
        rows[entity.id] = entity
        flush()
    }

    override suspend fun delete(id: String) {
        rows.remove(id)
        flush()
    }

    override suspend fun setActive(
        id: String,
        isActive: Boolean,
    ) {
        rows[id]?.let { rows[id] = it.copy(isActive = isActive) }
        flush()
    }
}
