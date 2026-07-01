package com.mileway.feature.tracking.worker

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.model.db.LocationData
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** P-F.2: MileageMaintenanceTask deletes rows older than 90 days. */
class MaintenancePurgeTest {
    private val fakeEnv = WorkerEnvironment(progressListener = null, isCancelled = { false })

    @Test
    fun `run calls deleteOlderThan with cutoff approximately 90 days ago`() =
        runTest {
            val fake = FakeLocationDao()
            val task = MileageMaintenanceTask(fake)
            val before = kotlin.time.Clock.System.now().toEpochMilliseconds()
            task.doWork(null, fakeEnv)
            val after = kotlin.time.Clock.System.now().toEpochMilliseconds()

            val cutoff = fake.lastDeleteOlderThanTimestamp
            assertNotNull(cutoff)
            // cutoff should be 90 days before the call (within a 1-second window).
            val expectedBase = before - MileageMaintenanceTask.RETENTION_MS
            val expectedTop = after - MileageMaintenanceTask.RETENTION_MS
            assertTrue(cutoff >= expectedBase, "cutoff $cutoff < expected base $expectedBase")
            assertTrue(cutoff <= expectedTop, "cutoff $cutoff > expected top $expectedTop")
        }

    @Test
    fun `run with no locations does not throw`() =
        runTest {
            MileageMaintenanceTask(FakeLocationDao()).doWork(null, fakeEnv)
        }
}

// ── Fake ──────────────────────────────────────────────────────────────────────

private class FakeLocationDao : LocationDao {
    var lastDeleteOlderThanTimestamp: Long? = null

    override fun getLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override suspend fun getLocationsByTokenOnce(token: String): List<LocationData> = emptyList()

    override suspend fun getLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun countLocationsByToken(token: String): Int = 0

    override fun getAllLocations(): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByUploadStatus(uploaded: Boolean): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByActivity(activity: String): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getCheckInLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override suspend fun insertLocation(location: LocationData) {}

    override suspend fun insertLocations(locations: List<LocationData>) {}

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

    override suspend fun getLocationCount(): Int = 0

    override suspend fun getUnuploadedLocationCount(uploadedValue: Boolean): Int = 0

    override suspend fun getUnsyncedLocationsByToken(token: String): List<LocationData> = emptyList()

    override suspend fun getUnsyncedLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun markLocationsAsSynced(locationIds: List<Long>) {}

    override suspend fun deleteOlderThan(timestamp: Long): Int {
        lastDeleteOlderThanTimestamp = timestamp
        return 0
    }

    override suspend fun getFirstUnsyncedLocationByToken(token: String): LocationData? = null

    override suspend fun getLastLocationByToken(token: String): LocationData? = null
}
