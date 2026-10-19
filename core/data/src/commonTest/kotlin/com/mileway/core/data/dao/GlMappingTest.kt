package com.mileway.core.data.dao

import com.mileway.core.data.model.db.GlMappingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * L25 contract for how the GL export uses [GlMappingDao]: upsert by category replaces the row, and
 * get and observeAll agree. The in-memory fake mirrors `@Insert(REPLACE)` on the `category` key;
 * the SQL itself is covered by the frozen migration tests, and this lane adds no migration.
 */
class GlMappingTest {
    private class InMemoryGlMappingDao : GlMappingDao {
        private val rows = MutableStateFlow<Map<String, GlMappingEntity>>(emptyMap())

        override fun observeAll(): Flow<List<GlMappingEntity>> = rows.map { it.values.toList() }

        override suspend fun get(category: String): GlMappingEntity? = rows.value[category]

        override suspend fun upsert(entity: GlMappingEntity) {
            rows.value = rows.value + (entity.category to entity)
        }
    }

    @Test
    fun `upsert by category replaces the previous code and keeps one row`() =
        runTest {
            val dao = InMemoryGlMappingDao()
            dao.upsert(GlMappingEntity("Meals", "6200", null, 1L))
            dao.upsert(GlMappingEntity("Meals", "6210", "CC-1", 2L))
            assertEquals(GlMappingEntity("Meals", "6210", "CC-1", 2L), dao.get("Meals"))
            assertEquals(1, dao.observeAll().first().size)
            assertNull(dao.get("Travel"))
        }
}
