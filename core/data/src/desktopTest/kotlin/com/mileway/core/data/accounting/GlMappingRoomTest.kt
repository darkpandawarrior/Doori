package com.mileway.core.data.accounting

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mileway.core.data.database.MilewayDatabase
import com.mileway.core.data.model.db.GlMappingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GlMappingRoomTest {
    @Test
    fun realMappingTableRoundTripsAndUpsertReplacesOnlyItsCategory() =
        runTest {
            val database =
                Room
                    .inMemoryDatabaseBuilder<MilewayDatabase>()
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.Default)
                    .build()
            try {
                val dao = database.glMappingDao()
                assertNull(dao.get("FOOD"))
                val food = GlMappingEntity("FOOD", "6100", "CC-1", 123)
                val mileage = GlMappingEntity("MILEAGE", "6200", null, 124)
                dao.upsert(food)
                dao.upsert(mileage)
                assertEquals(food, dao.get("FOOD"))
                assertEquals(setOf(food, mileage), dao.observeAll().first().toSet())
                val replacement = food.copy(glAccountCode = "6300", costCenter = "CC-2", updatedAtMs = 456)
                dao.upsert(replacement)
                assertEquals(replacement, dao.get("FOOD"))
                assertEquals(mileage, dao.get("MILEAGE"))
                assertEquals(setOf(replacement, mileage), dao.observeAll().first().toSet())
            } finally {
                database.close()
            }
        }
}
