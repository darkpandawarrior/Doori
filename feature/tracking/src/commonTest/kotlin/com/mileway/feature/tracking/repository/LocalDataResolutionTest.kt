package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Wave-4 §2.3 has_local_data: purged → flag set → [SavedTrackRepository.resolveLocalData]
 * signals the server-fallback stub instead of silently returning nothing.
 */
class LocalDataResolutionTest {
    private fun track(
        routeId: String,
        hasLocalData: Boolean = true,
    ) = SavedTrack(
        routeId = routeId,
        name = "trip",
        isCompleted = true,
        startLatitude = 0.0,
        startLongitude = 0.0,
        endLatitude = 0.0,
        endLongitude = 0.0,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        startTime = 0L,
        endTime = 0L,
        distance = 1.0,
        duration = 1L,
        hasLocalData = hasLocalData,
    )

    @Test
    fun `an unpurged route resolves to Local`() =
        runTest {
            val dao = FakeLocalDataSavedTrackDao()
            dao.tracks["r1"] = track("r1", hasLocalData = true)
            val repo = SavedTrackRepository(dao)

            assertEquals(LocalDataResolution.Local("r1"), repo.resolveLocalData("r1"))
        }

    @Test
    fun `a route not found resolves to NotFound`() =
        runTest {
            val repo = SavedTrackRepository(FakeLocalDataSavedTrackDao())

            assertIs<LocalDataResolution.NotFound>(repo.resolveLocalData("missing"))
        }

    @Test
    fun `after the maintenance purge sets has_local_data false, resolution signals server fallback`() =
        runTest {
            val dao = FakeLocalDataSavedTrackDao()
            dao.tracks["r1"] = track("r1", hasLocalData = true)
            val repo = SavedTrackRepository(dao)

            dao.markLocalDataPurged("r1")

            assertEquals(LocalDataResolution.WouldFetchFromServer("r1"), repo.resolveLocalData("r1"))
        }
}

private class FakeLocalDataSavedTrackDao : SavedTrackDao {
    // P10.1: stale-fake catch-up — SavedTrackDao.updateSmartDistanceFinal was added by the
    // SmartDistance commit without updating these test fakes; no-op override so this test source
    // set compiles (pre-existing breakage, incidental to P10.1).
    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    val tracks = mutableMapOf<String, SavedTrack>()

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = tracks[routeId]

    override suspend fun markLocalDataPurged(routeId: String) {
        tracks[routeId]?.let { tracks[routeId] = it.copy(hasLocalData = false) }
    }

    override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = tracks.values.filter { it.hasLocalData }.map { it.routeId }

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
