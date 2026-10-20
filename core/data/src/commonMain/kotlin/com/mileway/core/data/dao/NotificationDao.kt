package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.NotificationEntity
import kotlinx.coroutines.flow.Flow

/** PLAN_V22 P6.5: the persisted Notification Centre feed — see [NotificationEntity]. */
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE id NOT LIKE 'nudge-%'")
    suspend fun countNonNudge(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NotificationEntity>)

    @Query("UPDATE notifications SET isUnread = :isUnread WHERE id = :id")
    suspend fun setUnread(
        id: String,
        isUnread: Boolean,
    )

    @Query("UPDATE notifications SET isUnread = 0")
    suspend fun markAllRead()
}
