package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface HardwareEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: HardwareEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<HardwareEvent>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<HardwareEvent>)

    @Query("SELECT * FROM hardware_events WHERE token = :token ORDER BY time ASC")
    suspend fun getEventsByToken(token: String): List<HardwareEvent>

    @Query("SELECT * FROM hardware_events WHERE token = :token ORDER BY time ASC")
    fun observeEventsByToken(token: String): Flow<List<HardwareEvent>>

    @Query("SELECT * FROM hardware_events WHERE token = :token AND eventType IN (:types) ORDER BY time ASC")
    suspend fun getEventsByTokenAndTypes(
        token: String,
        types: List<EventType>,
    ): List<HardwareEvent>

    @Query("SELECT * FROM hardware_events WHERE token = :token AND audience IN (:audiences) ORDER BY time ASC")
    suspend fun getEventsByTokenAndAudience(
        token: String,
        audiences: List<EventAudience>,
    ): List<HardwareEvent>

    @Query("SELECT * FROM hardware_events WHERE token = :token AND lat IS NOT NULL AND lng IS NOT NULL ORDER BY time ASC")
    suspend fun getEventsWithLocationByToken(token: String): List<HardwareEvent>

    @Query("SELECT * FROM hardware_events WHERE token = :token AND time BETWEEN :startTime AND :endTime ORDER BY time ASC")
    suspend fun getEventsByTokenAndTimeRange(
        token: String,
        startTime: Long,
        endTime: Long,
    ): List<HardwareEvent>

    @Query("SELECT COUNT(*) FROM hardware_events WHERE token = :token")
    suspend fun getEventCountByToken(token: String): Int

    @Query("SELECT COUNT(*) FROM hardware_events WHERE token = :token AND eventType = :eventType")
    suspend fun getEventCountByTokenAndType(
        token: String,
        eventType: EventType,
    ): Int

    @Query("DELETE FROM hardware_events WHERE time < :cutoffTime")
    suspend fun deleteEventsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM hardware_events WHERE token = :token")
    suspend fun deleteEventsByToken(token: String): Int

    @Query("DELETE FROM hardware_events WHERE token = :token")
    suspend fun deleteByToken(token: String)

    @Query("SELECT * FROM hardware_events WHERE uploaded = 0 ORDER BY time ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int = 100): List<HardwareEvent>

    @Query("SELECT * FROM hardware_events WHERE token = :token AND uploaded = 0 ORDER BY time ASC")
    suspend fun getUnsyncedEventsByToken(token: String): List<HardwareEvent>

    @Query("UPDATE hardware_events SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markEventsAsUploaded(ids: List<Long>): Int

    @Query("UPDATE hardware_events SET uploaded = 1 WHERE id IN (:eventIds)")
    suspend fun markEventsAsSynced(eventIds: List<Long>)

    @Query("SELECT * FROM hardware_events ORDER BY time DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 50): List<HardwareEvent>

    @Query("SELECT DISTINCT eventType FROM hardware_events WHERE token = :token")
    suspend fun getDistinctEventTypesByToken(token: String): List<EventType>
}
