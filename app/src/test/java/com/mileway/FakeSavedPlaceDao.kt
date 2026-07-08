package com.mileway

import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.data.model.db.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [SavedPlaceDao] (PLAN_V24 P3.4) — lets JVM/Robolectric tests construct
 * `SavedPlacesRepository`/`SavedPlacesViewModel` without a Room instance, mirroring
 * [FakeDelegationDao]'s shape. A relaxed mockk would return a null Flow that the ViewModel's
 * `init` collector dereferences, so a real fake is required here.
 */
class FakeSavedPlaceDao : SavedPlaceDao {
    private val rows = MutableStateFlow<Map<String, SavedPlaceEntity>>(emptyMap())

    override fun observeAll(): Flow<List<SavedPlaceEntity>> =
        rows.map { it.values.sortedByDescending { row -> row.updatedAtMs } }

    override suspend fun upsert(entity: SavedPlaceEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value - id
    }
}
