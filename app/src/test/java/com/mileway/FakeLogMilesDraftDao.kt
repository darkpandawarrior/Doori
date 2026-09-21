package com.mileway

import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.model.db.LogMilesDraftEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [LogMilesDraftDao] (P5.1) — `LogMilesViewModel.observeDrafts()` now
 * `collectLatest`s `getAllDrafts()` in `init`, and a bare `mockk(relaxed = true)` returns a
 * null-backed `Flow` that crashes that collector (memory: screenshot Koin needs deterministic
 * fakes, same reason `FakeVoucherDao` exists for `VoucherHistoryViewModel`).
 */
class FakeLogMilesDraftDao : LogMilesDraftDao {
    private val drafts = MutableStateFlow<Map<String, LogMilesDraftEntity>>(emptyMap())

    override fun getAllDrafts(): Flow<List<LogMilesDraftEntity>> = drafts.map { it.values.sortedByDescending { d -> d.updatedAt } }

    override suspend fun getDraftById(draftId: String): LogMilesDraftEntity? = drafts.value[draftId]

    override suspend fun upsertDraft(draft: LogMilesDraftEntity) {
        drafts.value = drafts.value + (draft.draftId to draft)
    }

    override suspend fun deleteDraftById(draftId: String) {
        drafts.value = drafts.value - draftId
    }

    override suspend fun deleteAllDrafts() {
        drafts.value = emptyMap()
    }
}
