package com.mileway

import com.mileway.core.data.dao.SessionDao
import com.mileway.core.data.model.db.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [SessionDao] (P6.4) — lets JVM/Robolectric tests construct
 * `ActiveSessionsRepository`/`ActiveSessionsViewModel` without a Room instance, mirroring
 * [FakeDelegationDao]'s shape.
 */
class FakeSessionDao : SessionDao {
    private val rows = MutableStateFlow<Map<String, SessionEntity>>(emptyMap())

    override fun observeAll(): Flow<List<SessionEntity>> = rows.map { it.values.sortedByDescending { row -> row.lastActiveMillis } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsertAll(sessions: List<SessionEntity>) {
        rows.value = rows.value + sessions.associateBy { it.id }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun deleteAllExceptCurrent() {
        rows.value = rows.value.filterValues { it.isCurrent }
    }
}
