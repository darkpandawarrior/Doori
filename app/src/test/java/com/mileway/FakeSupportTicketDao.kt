package com.mileway

import com.mileway.core.data.dao.SupportTicketDao
import com.mileway.core.data.model.db.SupportTicketEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [SupportTicketDao] (P6.8) — lets JVM/Robolectric tests construct
 * `SupportTicketRepository`/`SupportTicketViewModel` without a Room instance, mirroring
 * [FakeDelegationDao]'s shape.
 */
class FakeSupportTicketDao : SupportTicketDao {
    private val rows = MutableStateFlow<Map<String, SupportTicketEntity>>(emptyMap())

    override fun observeAll(): Flow<List<SupportTicketEntity>> = rows.map { it.values.sortedByDescending { row -> row.createdAtMs } }

    override suspend fun upsert(entity: SupportTicketEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
