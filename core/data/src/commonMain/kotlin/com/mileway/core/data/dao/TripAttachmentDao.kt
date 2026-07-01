package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.AttachmentType
import com.mileway.core.data.model.db.TripAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: TripAttachmentEntity): Long

    @Query("SELECT * FROM trip_attachments WHERE track_token = :trackToken ORDER BY created_at ASC")
    fun observeForTrack(trackToken: String): Flow<List<TripAttachmentEntity>>

    @Query("SELECT * FROM trip_attachments WHERE track_token = :trackToken ORDER BY created_at ASC")
    suspend fun getForTrack(trackToken: String): List<TripAttachmentEntity>

    @Query("SELECT * FROM trip_attachments WHERE track_token = :trackToken AND type = :type ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestOfType(
        trackToken: String,
        type: AttachmentType,
    ): TripAttachmentEntity?

    @Query("SELECT * FROM trip_attachments WHERE track_token = :trackToken AND type = :type ORDER BY created_at ASC")
    fun observeByType(
        trackToken: String,
        type: AttachmentType,
    ): Flow<List<TripAttachmentEntity>>

    @Query("DELETE FROM trip_attachments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM trip_attachments WHERE track_token = :trackToken")
    suspend fun deleteForTrack(trackToken: String)

    @Query("SELECT COUNT(*) FROM trip_attachments WHERE track_token = :trackToken")
    suspend fun countForTrack(trackToken: String): Int
}
