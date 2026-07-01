package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mileway.core.data.model.db.LogMilesDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogMilesDraftDao {
    @Query("SELECT * FROM log_miles_drafts ORDER BY updatedAt DESC")
    fun getAllDrafts(): Flow<List<LogMilesDraftEntity>>

    @Query("SELECT * FROM log_miles_drafts WHERE draftId = :draftId LIMIT 1")
    suspend fun getDraftById(draftId: String): LogMilesDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: LogMilesDraftEntity)

    @Query("DELETE FROM log_miles_drafts WHERE draftId = :draftId")
    suspend fun deleteDraftById(draftId: String)

    @Query("DELETE FROM log_miles_drafts")
    suspend fun deleteAllDrafts()
}
