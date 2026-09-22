package com.mileway.core.data.dao

import com.mileway.core.data.model.db.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P6.4: exercises [SessionDao]'s contract against an in-memory fake, mirroring
 * [DelegationDaoTest]'s pattern (`exportSchema=false` blocks `MigrationTestHelper` on the JVM —
 * see memory `miletracker-backlog-audit-v18` — so the schema/migration side is covered separately
 * by an instrumented test calling `MIGRATION_13_14.migrate()` directly).
 */
class SessionDaoTest {
    private fun session(
        id: String,
        isCurrent: Boolean = false,
        lastActiveMillis: Long = 0L,
    ) = SessionEntity(
        id = id,
        deviceName = "Device $id",
        platform = "Android 15",
        lastActiveMillis = lastActiveMillis,
        isCurrent = isCurrent,
    )

    @Test
    fun `upsertAll adds rows observable via observeAll`() =
        runTest {
            val dao = FakeSessionDao()

            dao.upsertAll(listOf(session("S1", lastActiveMillis = 1L)))

            assertEquals(listOf("S1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `observeAll orders rows by lastActiveMillis descending`() =
        runTest {
            val dao = FakeSessionDao()
            dao.upsertAll(listOf(session("S1", lastActiveMillis = 1L), session("S2", lastActiveMillis = 2L)))

            assertEquals(listOf("S2", "S1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `delete removes exactly one row`() =
        runTest {
            val dao = FakeSessionDao()
            dao.upsertAll(listOf(session("S1", lastActiveMillis = 1L), session("S2", lastActiveMillis = 2L)))

            dao.delete("S1")

            assertEquals(listOf("S2"), dao.snapshot().map { it.id })
        }

    @Test
    fun `deleteAllExceptCurrent leaves only the current-device row`() =
        runTest {
            val dao = FakeSessionDao()
            dao.upsertAll(
                listOf(
                    session("S1", isCurrent = true, lastActiveMillis = 3L),
                    session("S2", isCurrent = false, lastActiveMillis = 2L),
                    session("S3", isCurrent = false, lastActiveMillis = 1L),
                ),
            )

            dao.deleteAllExceptCurrent()

            val remaining = dao.snapshot()
            assertEquals(listOf("S1"), remaining.map { it.id })
            assertTrue(remaining.single().isCurrent)
        }

    @Test
    fun `count reflects the number of persisted rows`() =
        runTest {
            val dao = FakeSessionDao()
            assertEquals(0, dao.count())

            dao.upsertAll(listOf(session("S1", lastActiveMillis = 1L), session("S2", lastActiveMillis = 2L)))

            assertEquals(2, dao.count())
        }
}

/** In-memory fake mirroring the semantics Room enforces for [SessionDao]. */
private class FakeSessionDao : SessionDao {
    private val rows = LinkedHashMap<String, SessionEntity>()

    private val allRows = MutableStateFlow<List<SessionEntity>>(emptyList())

    private fun flush() {
        allRows.value = rows.values.sortedByDescending { it.lastActiveMillis }
    }

    fun snapshot(): List<SessionEntity> = allRows.value

    override fun observeAll(): Flow<List<SessionEntity>> = allRows.asStateFlow()

    override suspend fun count(): Int = rows.size

    override suspend fun upsertAll(sessions: List<SessionEntity>) {
        sessions.forEach { rows[it.id] = it }
        flush()
    }

    override suspend fun delete(id: String) {
        rows.remove(id)
        flush()
    }

    override suspend fun deleteAllExceptCurrent() {
        rows.values.filterNot { it.isCurrent }.forEach { rows.remove(it.id) }
        flush()
    }
}
