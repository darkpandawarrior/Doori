package com.mileway

import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.model.db.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [NotificationDao] (P6.5) — lets JVM/Robolectric tests construct
 * `NotificationRepository`/`NotificationViewModel` without a Room instance, mirroring
 * [FakeDelegationDao]'s shape.
 */
class FakeNotificationDao : NotificationDao {
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
        val existing = rows.value[id] ?: return
        rows.value = rows.value + (id to existing.copy(isUnread = isUnread))
    }

    override suspend fun markAllRead() {
        rows.value = rows.value.mapValues { (_, entity) -> entity.copy(isUnread = false) }
    }
}
