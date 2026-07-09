package com.mileway

import com.mileway.core.data.dao.DeletionRequestDao
import com.mileway.core.data.model.db.DeletionRequestEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake for [DeletionRequestDao] (PLAN_V24 P7.1) — lets JVM/Robolectric renders construct
 * `DeletionRequestRepository`/`AccountDeletionViewModel`, whose init collects `observe()`, without a
 * Room instance. A relaxed mockk returns a null-backed Flow that crashes that collector (memory:
 * screenshot Koin needs deterministic fakes, same reason FakeVoucherDao exists).
 */
class FakeDeletionRequestDao : DeletionRequestDao {
    private val row = MutableStateFlow<DeletionRequestEntity?>(null)

    override fun observe(): Flow<DeletionRequestEntity?> = row

    override suspend fun get(): DeletionRequestEntity? = row.value

    override suspend fun upsert(entity: DeletionRequestEntity) {
        row.value = entity
    }

    override suspend fun clear() {
        row.value = null
    }
}
