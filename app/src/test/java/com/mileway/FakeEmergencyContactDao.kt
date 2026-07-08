package com.mileway

import com.mileway.core.data.dao.EmergencyContactDao
import com.mileway.core.data.model.db.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [EmergencyContactDao] (PLAN_V24 P3.5) — lets JVM/Robolectric tests construct
 * `EmergencyContactsRepository`/`EmergencyContactsViewModel`/`SosViewModel` without a Room
 * instance. A relaxed mockk would return a null Flow the ViewModels' `init` collector
 * dereferences, so a real fake is required.
 */
class FakeEmergencyContactDao : EmergencyContactDao {
    private val rows = MutableStateFlow<Map<String, EmergencyContactEntity>>(emptyMap())

    override fun observeAll(): Flow<List<EmergencyContactEntity>> =
        rows.map { it.values.sortedBy { row -> row.createdAtMs } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsert(entity: EmergencyContactEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}
