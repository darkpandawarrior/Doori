package com.mileway.feature.tracking.worker

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `run purges locations and flags has_local_data for eligible routes when a SavedTrackDao is wired`() =
        runTest {
            val locationDao = FakeLocationDao()
            val savedTrackDao = FakeEligibleSavedTrackDao(eligibleRouteIds = listOf("r1", "r2"))
            val task = MileageMaintenanceTask(locationDao, savedTrackDao)

            task.doWork(null, fakeEnv)

            assertEquals(listOf("r1", "r2"), locationDao.deletedByToken)
            assertEquals(listOf("r1", "r2"), savedTrackDao.purgedRouteIds)
        }
}

private class FakeEligibleSavedTrackDao(private val eligibleRouteIds: List<String>) : SavedTrackDao {
    // P10.1: stale-fake catch-up — SavedTrackDao.updateSmartDistanceFinal was added by the
    // SmartDistance commit without updating these test fakes; no-op override so this test source
    // set compiles (pre-existing breakage, incidental to P10.1).
    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    val purgedRouteIds = mutableListOf<String>()

    override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = eligibleRouteIds

    override suspend fun markLocalDataPurged(routeId: String) {
        purgedRouteIds += routeId
    }

    // ── Everything else unused ─────────────────────────────────────────────
    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {}

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

    override suspend fun deleteSavedTrack(track: SavedTrack) {}

    override suspend fun deleteSavedTrack(routeId: String) {}

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun count(): Long = 0

    override suspend fun getActiveTrack(): SavedTrack? = null

    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null

    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun getMostRecentActiveTrack(): SavedTrack? = null

    override suspend fun getLastCompletedTrack(): SavedTrack? = null

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = null

    override fun observeTrackById(routeId: String): Flow<SavedTrack?> = flowOf(null)

    override fun getRetainedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getTracksInRange(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getTracksInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun countInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Int = 0

    override suspend fun updateTrackName(
        routeId: String,
        name: String,
    ) {}

    override suspend fun updateTrackLiveData(
        routeId: String,
        distance: Double,
        duration: Long,
    ) {}

    override suspend fun markTrackDraft(
        routeId: String,
        draftSavedAt: Long,
    ): Int = 0

    override suspend fun updateSubmissionTime(
        routeId: String,
        submissionTime: Long,
    ): Int = 0

    override suspend fun finalizeTrack(
        routeId: String,
        endTime: Long,
        finalDistance: Double,
        avgSpeed: Double,
        maxSpeed: Double,
    ) {}

    override suspend fun markTrackCompleted(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
        submittedAmount: Double,
        submittedAmountCurrency: String,
        transId: String?,
    ): Int = 0

    override suspend fun markTrackEndedLocally(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
    ): Int = 0

    override suspend fun markRetained(routeIds: List<String>) {}

    override suspend fun markRetainedBefore(threshold: Long): Int = 0

    override suspend fun setRetained(
        routeId: String,
        retained: Boolean,
    ) {}

    override suspend fun deleteCorruptedTracks(): Int = 0

    override suspend fun getCorruptedTrackCount(): Int = 0

    override suspend fun deleteOlderThanExcludingRetained(threshold: Long): Int = 0

    override suspend fun getLastNRouteIdsFromRange(
        start: Long,
        end: Long,
        limit: Int,
    ): List<String> = emptyList()

    override suspend fun getAverageTrackMetrics(): TrackMetrics = TrackMetrics(0.0, 0L, 0f, 0)

    override suspend fun getPreviousSimilarTrack(routeId: String): SavedTrack? = null

    override suspend fun getSimilarTracks(routeId: String): List<SavedTrack> = emptyList()

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int = 0

    override suspend fun markOdometerNotWorking(routeId: String): Int = 0
}

// ── Fake ──────────────────────────────────────────────────────────────────────

private class FakeLocationDao : LocationDao {
    var lastDeleteOlderThanTimestamp: Long? = null
    val deletedByToken = mutableListOf<String>()

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

    override fun getAllCheckInPoints(): Flow<List<LocationData>> = flowOf(emptyList())

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

    override suspend fun deleteLocationsByToken(token: String) {
        deletedByToken += token
    }

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
