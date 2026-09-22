package com.mileway.core.data.dao

import com.mileway.core.data.model.db.SupportTicketEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * P6.8: exercises [SupportTicketDao]'s contract against an in-memory fake, mirroring
 * [ConnectedAccountDaoTest]'s pattern (`exportSchema=false` blocks `MigrationTestHelper` on the
 * JVM — see memory `miletracker-backlog-audit-v18` — so the schema/migration side is covered
 * separately by an instrumented test calling `MIGRATION_16_17.migrate()` directly).
 */
class SupportTicketDaoTest {
    private fun ticket(
        id: String,
        subject: String,
        createdAtMs: Long = 0L,
        status: String = "OPEN",
    ) = SupportTicketEntity(
        id = id,
        subject = subject,
        body = "Body for $subject",
        createdAtMs = createdAtMs,
        status = status,
    )

    @Test
    fun `upsert adds a ticket observable via observeAll`() =
        runTest {
            val dao = FakeSupportTicketDao()

            dao.upsert(ticket("T1", "Can't submit expense"))

            assertEquals(listOf("T1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `observeAll orders rows by createdAtMs descending (most recent first)`() =
        runTest {
            val dao = FakeSupportTicketDao()
            dao.upsert(ticket("T1", "Older ticket", createdAtMs = 1_000L))
            dao.upsert(ticket("T2", "Newer ticket", createdAtMs = 2_000L))

            assertEquals(listOf("T2", "T1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `upsert with the same id replaces the existing row`() =
        runTest {
            val dao = FakeSupportTicketDao()
            dao.upsert(ticket("T1", "Original subject", status = "OPEN"))

            dao.upsert(ticket("T1", "Original subject", status = "RESOLVED"))

            assertEquals(1, dao.snapshot().size)
            assertEquals("RESOLVED", dao.snapshot().single().status)
        }
}

/** In-memory fake mirroring the semantics Room enforces for [SupportTicketDao]. */
private class FakeSupportTicketDao : SupportTicketDao {
    private val rows = LinkedHashMap<String, SupportTicketEntity>()

    private val allRows = MutableStateFlow<List<SupportTicketEntity>>(emptyList())

    private fun flush() {
        allRows.value = rows.values.sortedByDescending { it.createdAtMs }
    }

    fun snapshot(): List<SupportTicketEntity> = allRows.value

    override fun observeAll(): Flow<List<SupportTicketEntity>> = allRows.asStateFlow()

    override suspend fun upsert(entity: SupportTicketEntity) {
        rows[entity.id] = entity
        flush()
    }
}
