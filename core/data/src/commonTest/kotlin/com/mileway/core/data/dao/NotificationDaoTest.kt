package com.mileway.core.data.dao

import com.mileway.core.data.model.db.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P6.5: exercises [NotificationDao]'s contract against an in-memory fake, mirroring
 * [DelegationDaoTest]'s pattern (`exportSchema=false` blocks `MigrationTestHelper` on the JVM —
 * see memory `miletracker-backlog-audit-v18` — so the schema/migration side is covered separately
 * by an instrumented test calling `MIGRATION_14_15.migrate()` directly).
 */
class NotificationDaoTest {
    private fun notification(
        id: String,
        isUnread: Boolean = true,
        createdAtMs: Long = 0L,
    ) = NotificationEntity(
        id = id,
        title = "Title $id",
        body = "Body $id",
        relativeTime = "2 min ago",
        isUnread = isUnread,
        type = "SYSTEM",
        createdAtMs = createdAtMs,
    )

    @Test
    fun `upsertAll adds rows observable via observeAll`() =
        runTest {
            val dao = FakeNotificationDao()

            dao.upsertAll(listOf(notification("N1", createdAtMs = 1L)))

            assertEquals(listOf("N1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `observeAll orders rows by createdAtMs descending`() =
        runTest {
            val dao = FakeNotificationDao()
            dao.upsertAll(listOf(notification("N1", createdAtMs = 1L), notification("N2", createdAtMs = 2L)))

            assertEquals(listOf("N2", "N1"), dao.snapshot().map { it.id })
        }

    @Test
    fun `count reflects the number of persisted rows`() =
        runTest {
            val dao = FakeNotificationDao()
            assertEquals(0, dao.count())

            dao.upsertAll(listOf(notification("N1", createdAtMs = 1L), notification("N2", createdAtMs = 2L)))

            assertEquals(2, dao.count())
        }

    @Test
    fun `setUnread toggles only the targeted row`() =
        runTest {
            val dao = FakeNotificationDao()
            dao.upsertAll(
                listOf(
                    notification("N1", isUnread = true, createdAtMs = 1L),
                    notification("N2", isUnread = true, createdAtMs = 2L),
                ),
            )

            dao.setUnread("N1", false)

            val rows = dao.snapshot().associateBy { it.id }
            assertTrue(rows.getValue("N2").isUnread)
            assertEquals(false, rows.getValue("N1").isUnread)
        }

    @Test
    fun `markAllRead clears every row's unread flag`() =
        runTest {
            val dao = FakeNotificationDao()
            dao.upsertAll(
                listOf(
                    notification("N1", isUnread = true, createdAtMs = 1L),
                    notification("N2", isUnread = true, createdAtMs = 2L),
                ),
            )

            dao.markAllRead()

            assertTrue(dao.snapshot().none { it.isUnread })
        }
}

/** In-memory fake mirroring the semantics Room enforces for [NotificationDao]. */
private class FakeNotificationDao : NotificationDao {
    private val rows = LinkedHashMap<String, NotificationEntity>()

    private val allRows = MutableStateFlow<List<NotificationEntity>>(emptyList())

    private fun flush() {
        allRows.value = rows.values.sortedByDescending { it.createdAtMs }
    }

    fun snapshot(): List<NotificationEntity> = allRows.value

    override fun observeAll(): Flow<List<NotificationEntity>> = allRows.asStateFlow()

    override suspend fun count(): Int = rows.size

    override suspend fun upsertAll(entities: List<NotificationEntity>) {
        entities.forEach { rows[it.id] = it }
        flush()
    }

    override suspend fun setUnread(
        id: String,
        isUnread: Boolean,
    ) {
        rows[id]?.let { rows[id] = it.copy(isUnread = isUnread) }
        flush()
    }

    override suspend fun markAllRead() {
        rows.keys.toList().forEach { id -> rows[id] = rows.getValue(id).copy(isUnread = false) }
        flush()
    }
}
