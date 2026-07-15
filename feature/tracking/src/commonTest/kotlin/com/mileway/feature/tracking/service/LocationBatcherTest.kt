package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.model.db.LocationData
import com.mileway.feature.tracking.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Wave-2: LocationBatcher cadence (10pt/30s) + the stop/pause flush data-loss guardrail. */
class LocationBatcherTest {
    private fun point(id: Long) = LocationData(id = id, activity = "", speed = 0f, lat = 0.0, lng = 0.0, token = "t", batteryPercentage = 100.0)

    @Test
    fun `flushes at exactly 10 points`() =
        runTest {
            val dao = FakeLocationDao()
            var clock = 0L
            val batcher = LocationBatcher(LocationRepository(dao), now = { clock })

            repeat(9) { batcher.add(point(it.toLong())) }
            assertTrue(dao.batches.isEmpty(), "should not flush before the 10th point")

            batcher.add(point(9))
            assertEquals(1, dao.batches.size)
            assertEquals(10, dao.batches.single().size)
        }

    @Test
    fun `flushes after 30s even with a partial batch`() =
        runTest {
            val dao = FakeLocationDao()
            var clock = 0L
            val batcher = LocationBatcher(LocationRepository(dao), now = { clock })

            batcher.add(point(1))
            batcher.add(point(2))
            assertTrue(dao.batches.isEmpty())

            clock = LocationBatcher.MAX_BATCH_AGE_MS
            batcher.add(point(3))
            assertEquals(1, dao.batches.size)
            assertEquals(3, dao.batches.single().size)
        }

    @Test
    fun `flush persists a partial buffer - the stop-flush guarantee`() =
        runTest {
            val dao = FakeLocationDao()
            val batcher = LocationBatcher(LocationRepository(dao), now = { 0L })

            batcher.add(point(1))
            batcher.add(point(2))
            batcher.add(point(3))
            assertTrue(dao.batches.isEmpty(), "buffer under 10pt/30s shouldn't auto-flush")

            batcher.flush()
            assertEquals(1, dao.batches.size)
            assertEquals(3, dao.batches.single().size)
        }

    @Test
    fun `no points lost across a start then add 7 then stop sequence`() =
        runTest {
            val dao = FakeLocationDao()
            val batcher = LocationBatcher(LocationRepository(dao), now = { 0L })

            repeat(7) { batcher.add(point(it.toLong())) }
            batcher.flush() // simulates ACTION_STOP

            val totalWritten = dao.batches.sumOf { it.size }
            assertEquals(7, totalWritten)
        }

    @Test
    fun `flush is a no-op when the buffer is empty`() =
        runTest {
            val dao = FakeLocationDao()
            val batcher = LocationBatcher(LocationRepository(dao), now = { 0L })

            batcher.flush()
            assertTrue(dao.batches.isEmpty())
        }
}

private class FakeLocationDao : LocationDao {
    val batches = mutableListOf<List<LocationData>>()

    override fun getLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override suspend fun getLocationsByTokenOnce(token: String): List<LocationData> = batches.flatten()

    override suspend fun getLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun countLocationsByToken(token: String): Int = batches.sumOf { it.size }

    override fun getAllLocations(): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByUploadStatus(uploaded: Boolean): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByActivity(activity: String): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getCheckInLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getAllCheckInPoints(): Flow<List<LocationData>> = flowOf(emptyList())

    override suspend fun insertLocation(location: LocationData) {
        batches.add(listOf(location))
    }

    override suspend fun insertLocations(locations: List<LocationData>) {
        batches.add(locations)
    }

    override suspend fun updateLocation(location: LocationData) {}

    override suspend fun updateUploadStatus(
        id: Long,
        uploaded: Boolean,
    ) {}

    override suspend fun updateUploadStatusByToken(
        token: String,
        uploaded: Boolean,
    ) {}

    override suspend fun deleteLocation(location: LocationData) {}

    override suspend fun deleteLocationById(id: Long) {}

    override suspend fun deleteLocationsByToken(token: String) {}

    override suspend fun deleteUploadedLocations(uploadedValue: Boolean) {}

    override suspend fun deleteAllLocations() {}

    override suspend fun getLocationCount(): Int = batches.sumOf { it.size }

    override suspend fun getUnuploadedLocationCount(uploadedValue: Boolean): Int = 0

    override suspend fun getUnsyncedLocationsByToken(token: String): List<LocationData> = emptyList()

    override suspend fun getUnsyncedLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun getLocationsByIds(ids: List<Long>): List<LocationData> = emptyList()

    override suspend fun markLocationsAsSynced(locationIds: List<Long>) {}

    override suspend fun deleteOlderThan(timestamp: Long): Int = 0

    override suspend fun getFirstUnsyncedLocationByToken(token: String): LocationData? = null

    override suspend fun getLastLocationByToken(token: String): LocationData? = null
}
