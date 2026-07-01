package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mileway.core.data.model.db.SubmitDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmitDraftDao {
    @Query("SELECT * FROM submit_drafts WHERE formKey = :formKey ORDER BY createdAt DESC")
    fun observeByFormKey(formKey: String): Flow<List<SubmitDraftEntity>>

    @Upsert
    suspend fun upsert(entity: SubmitDraftEntity)

    @Query(
        "UPDATE submit_drafts SET status = :status, updatedAt = :now WHERE formKey = :formKey AND uniqueKey = :uniqueKey",
    )
    suspend fun updateStatus(
        formKey: String,
        uniqueKey: String,
        status: String,
        now: Long,
    )

    @Query(
        "UPDATE submit_drafts SET status = :status, errorMessage = :error, updatedAt = :now WHERE formKey = :formKey AND uniqueKey = :uniqueKey",
    )
    suspend fun updateStatusWithError(
        formKey: String,
        uniqueKey: String,
        status: String,
        error: String,
        now: Long,
    )

    @Query("DELETE FROM submit_drafts WHERE formKey = :formKey")
    suspend fun deleteByFormKey(formKey: String)
}
