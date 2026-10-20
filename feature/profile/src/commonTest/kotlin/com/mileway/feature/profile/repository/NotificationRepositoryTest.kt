package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.feature.profile.data.NotifType
import com.mileway.feature.profile.data.NotificationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * V29 P29.H.4: [NotificationRepository.observeUnreadCount] is the live source Home's header badge
 * now reads from (replacing `HomeMockData.notificationCount()`) — covers the derivation against a
 * fake [NotificationDao].
 */
class NotificationRepositoryTest {
    @Test
    fun `unread count derives from the unread rows only`() =
        runTest {
            val dao = FakeNotificationDao()
            val repo = NotificationRepository(dao)

            dao.upsertAll(
                listOf(
                    entity("n1", isUnread = true),
                    entity("n2", isUnread = true),
                    entity("n3", isUnread = false),
                ),
            )

            assertEquals(2, repo.observeUnreadCount().first())
        }

    @Test
    fun `marking one entry read decrements the count`() =
        runTest {
            val dao = FakeNotificationDao()
            val repo = NotificationRepository(dao)
            dao.upsertAll(listOf(entity("n1", isUnread = true), entity("n2", isUnread = true)))

            repo.setUnread("n1", false)

            assertEquals(1, repo.observeUnreadCount().first())
        }

    @Test
    fun `markAllRead zeroes the count`() =
        runTest {
            val dao = FakeNotificationDao()
            val repo = NotificationRepository(dao)
            dao.upsertAll(listOf(entity("n1", isUnread = true), entity("n2", isUnread = true)))

            repo.markAllRead()

            assertEquals(0, repo.observeUnreadCount().first())
        }

    @Test
    fun `unknown notification type falls back to system`() =
        runTest {
            val dao = FakeNotificationDao()
            dao.upsertAll(listOf(entity("future", true).copy(type = "FUTURE_TYPE"), entity("known", true).copy(type = "APPROVAL")))
            val records = NotificationRepository(dao).observeAll().first().associateBy { it.id }
            assertEquals(NotifType.SYSTEM, records.getValue("future").type)
            assertEquals(NotifType.APPROVAL, records.getValue("known").type)
        }

    @Test
    fun `nudges before first opening do not suppress seed or lose read state`() =
        runTest {
            val dao = FakeNotificationDao()
            val clock =
                object : Clock {
                    override fun now(): Instant = Instant.fromEpochMilliseconds(10_000)
                }
            val nudge = entity("nudge-health-2026-10-05", false).copy(createdAtMs = 5_000)
            dao.upsertAll(listOf(nudge))
            val repo = NotificationRepository(dao, clock)

            repo.seedIfEmpty()
            val seeded = dao.observeAll().first()
            assertEquals(NotificationData.all.size + 1, seeded.size)
            assertEquals(nudge, seeded.first())
            assertFalse(seeded.first().isUnread)
            assertTrue(seeded.drop(1).all { it.createdAtMs < nudge.createdAtMs })

            repo.markAllRead()
            val readRows = dao.observeAll().first()
            repo.seedIfEmpty()
            assertEquals(readRows, dao.observeAll().first())
        }

    @Test
    fun `existing non nudge feed is not seeded again`() =
        runTest {
            val dao = FakeNotificationDao()
            dao.upsertAll(listOf(entity("existing", false)))
            NotificationRepository(dao).seedIfEmpty()
            assertEquals(1, dao.count())
        }

    private fun entity(
        id: String,
        isUnread: Boolean,
    ) = NotificationEntity(
        id = id,
        title = "Title $id",
        body = "Body $id",
        relativeTime = "now",
        isUnread = isUnread,
        type = "SYSTEM",
        createdAtMs = 0L,
        deeplink = "",
    )
}

private class FakeNotificationDao : NotificationDao {
    private val rows = MutableStateFlow<Map<String, NotificationEntity>>(emptyMap())

    override fun observeAll(): Flow<List<NotificationEntity>> = rows.map { it.values.sortedByDescending { row -> row.createdAtMs } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun countNonNudge(): Int = rows.value.keys.count { !it.startsWith("nudge-") }

    override suspend fun upsertAll(entities: List<NotificationEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun setUnread(
        id: String,
        isUnread: Boolean,
    ) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(isUnread = isUnread)) }
    }

    override suspend fun markAllRead() {
        rows.value = rows.value.mapValues { (_, v) -> v.copy(isUnread = false) }
    }
}
