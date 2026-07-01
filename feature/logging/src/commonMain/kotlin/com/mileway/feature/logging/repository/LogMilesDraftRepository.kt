package com.mileway.feature.logging.repository

import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.model.db.LogMilesDraftEntity
import kotlinx.coroutines.flow.Flow

class LogMilesDraftRepository(private val dao: LogMilesDraftDao) {
    fun allDrafts(): Flow<List<LogMilesDraftEntity>> = dao.getAllDrafts()

    suspend fun save(draft: LogMilesDraftEntity) = dao.upsertDraft(draft)

    suspend fun delete(draftId: String) = dao.deleteDraftById(draftId)

    suspend fun deleteAll() = dao.deleteAllDrafts()
}
