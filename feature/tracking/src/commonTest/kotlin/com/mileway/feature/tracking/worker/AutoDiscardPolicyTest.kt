package com.mileway.feature.tracking.worker

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P-F.2: AutoDiscardTask policy unit tests. */
class AutoDiscardPolicyTest {
    private val fakeEnv = WorkerEnvironment(progressListener = null, isCancelled = { false })

    private fun task(
        enabled: Boolean,
        activeTrack: SavedTrack? = null,
    ): Triple<AutoDiscardTask, FakeDiscardLocationDao, FakeDiscardCurrentTrackSource> {
        val locationDao = FakeDiscardLocationDao()
        val currentTrackSource = FakeDiscardCurrentTrackSource()
        val t =
            AutoDiscardTask(
                isEnabled = { enabled },
                savedTrackRepository = SavedTrackRepository(FakeDiscardSavedTrackDao(activeTrack)),
                locationRepository = LocationRepository(locationDao),
                currentTrackRepository = CurrentTrackRepository(currentTrackSource),
            )
        return Triple(t, locationDao, currentTrackSource)
    }

    @Test
    fun `when disabled active track is not discarded`() =
        runTest {
            val (task, locationDao, currentTrackSource) = task(enabled = false, activeTrack = track())
            task.doWork(null, fakeEnv)
            assertEquals(emptyList(), locationDao.deletedTokens)
            assertFalse(currentTrackSource.sessionCleared)
        }

    @Test
    fun `when enabled but no active track nothing is discarded`() =
        runTest {
            val (task, locationDao, currentTrackSource) = task(enabled = true, activeTrack = null)
            task.doWork(null, fakeEnv)
            assertEquals(emptyList(), locationDao.deletedTokens)
            assertFalse(currentTrackSource.sessionCleared)
        }

    @Test
    fun `when enabled and active track exists locations deleted and session cleared`() =
        runTest {
            val active = track(routeId = "route-abc")
            val (task, locationDao, currentTrackSource) = task(enabled = true, activeTrack = active)
            task.doWork(null, fakeEnv)
            assertTrue("route-abc" in locationDao.deletedTokens)
            assertTrue(currentTrackSource.sessionCleared)
        }

    private fun track(routeId: String = "test-route") =
        SavedTrack(
            routeId = routeId,
            name = "Test Track",
            startLatitude = 0.0,
            startLongitude = 0.0,
            endLatitude = 0.0,
            endLongitude = 0.0,
            pausedLatitude = 0.0,
            pausedLongitude = 0.0,
            startTime = 1_000L,
            endTime = 0L,
            distance = 0.0,
            duration = 0L,
        )
}

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeDiscardSavedTrackDao(private val active: SavedTrack?) : SavedTrackDao {
    // P10.1: stale-fake catch-up — SavedTrackDao.updateSmartDistanceFinal was added by the
    // SmartDistance commit without updating these test fakes; no-op override so this test source
    // set compiles (pre-existing breakage, incidental to P10.1).
    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    override suspend fun getActiveTrack(): SavedTrack? = active

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = null

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {}

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

    override suspend fun deleteSavedTrack(track: SavedTrack) {}

    override suspend fun deleteSavedTrack(routeId: String) {}

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun count(): Long = 0

    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null

    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun getMostRecentActiveTrack(): SavedTrack? = null

    override suspend fun getLastCompletedTrack(): SavedTrack? = null

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

    override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = emptyList()

    override suspend fun markLocalDataPurged(routeId: String) {}

    override suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int = 0

    override suspend fun markOdometerNotWorking(routeId: String): Int = 0
}

private class FakeDiscardLocationDao : LocationDao {
    val deletedTokens = mutableListOf<String>()

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
        deletedTokens += token
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

    override suspend fun getLocationsByIds(ids: List<Long>): List<LocationData> = emptyList()

    override suspend fun markLocationsAsSynced(locationIds: List<Long>) {}

    override suspend fun deleteOlderThan(timestamp: Long): Int = 0

    override suspend fun getFirstUnsyncedLocationByToken(token: String): LocationData? = null

    override suspend fun getLastLocationByToken(token: String): LocationData? = null
}

private class FakeDiscardCurrentTrackSource : CurrentTrackDataSource {
    var sessionCleared = false

    override val currentTrackFlow: Flow<CurrentTrackData> = MutableStateFlow(CurrentTrackData(token = ""))

    override val syncSessionOverrideFlow: Flow<com.mileway.core.data.session.SyncSessionOverride?> =
        MutableStateFlow(null)

    override suspend fun setSyncSessionOverride(override: com.mileway.core.data.session.SyncSessionOverride?) {}

    override suspend fun saveSession(data: CurrentTrackData) {}

    override suspend fun updateDistance(
        token: String,
        distanceMeters: Double,
        speed: Double,
        avgSpeed: Double,
    ) {}

    override suspend fun updateLocationCount(
        token: String,
        total: Long,
        unsynced: Long,
    ) {}

    override suspend fun markPaused(
        token: String,
        lat: Double,
        lng: Double,
    ) {}

    override suspend fun markResumed(token: String) {}

    override suspend fun markStopped(
        token: String,
        endLat: Double,
        endLng: Double,
    ) {}

    override suspend fun clearSession() {
        sessionCleared = true
    }

    override suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    ) {}
}
