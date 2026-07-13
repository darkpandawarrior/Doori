package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.feature.profile.data.NotifType
import com.mileway.feature.profile.data.NotificationData
import com.mileway.feature.profile.data.NotificationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V22 P6.5: Room-backed store for `NotificationCentreScreen`'s feed — seeded once from
 * [NotificationData.all] (the previously-unused parallel demo dataset) instead of the screen's own
 * `remember { mutableStateOf(NOTIFICATIONS) }` seed, which reset on navigation away and left the
 * topbar's "174 unread" subtitle permanently hardcoded regardless of actual state.
 */
class NotificationRepository(private val dao: NotificationDao, private val clock: Clock = Clock.System) {
    /** Live, most-recent-first list of Notification Centre entries. */
    fun observeAll(): Flow<List<NotificationRecord>> = dao.observeAll().map { rows -> rows.map { it.toRecord() } }

    /** Live count of unread entries — derived, never a hardcoded string. */
    fun observeUnreadCount(): Flow<Int> = observeAll().map { list -> list.count { it.isUnread } }

    /** Seeds [NotificationData.all] on first run only; a no-op on every subsequent launch. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(
            NotificationData.all.mapIndexed { index, record ->
                record.toEntity(createdAtMs = now - index)
            },
        )
    }

    /** Marks a single entry read/unread. */
    suspend fun setUnread(
        id: String,
        isUnread: Boolean,
    ) = dao.setUnread(id, isUnread)

    /** Marks every entry read (`DepthAwareTopBar`'s "Mark all read" action). */
    suspend fun markAllRead() = dao.markAllRead()

    private fun NotificationEntity.toRecord(): NotificationRecord =
        NotificationRecord(
            id = id,
            title = title,
            body = body,
            relativeTime = relativeTime,
            isUnread = isUnread,
            type = NotifType.valueOf(type),
            deeplink = deeplink,
        )

    private fun NotificationRecord.toEntity(createdAtMs: Long): NotificationEntity =
        NotificationEntity(
            id = id,
            title = title,
            body = body,
            relativeTime = relativeTime,
            isUnread = isUnread,
            type = type.name,
            createdAtMs = createdAtMs,
            deeplink = deeplink,
        )
}
