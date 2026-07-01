package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * P-C.1: verifies that SavedTrackRepository.markAppKilled delegates to the DAO and returns the
 * DAO's row-count result.  Uses an in-memory fake so no Room infrastructure is needed.
 */
class AppKilledFlagTest {
    @Test
    fun `markAppKilled delegates to dao and returns updated row count`() =
        runTest {
            val fakeDao = FakeAppKilledSavedTrackDao()
            fakeDao.routeIdsInDb += "route-123"
            val repo = SavedTrackRepository(fakeDao)

            val result = repo.markAppKilled("route-123")

            assertEquals(1, result, "Should return 1 for a found route")
            assertEquals(listOf("route-123"), fakeDao.killedRouteIds)
        }

    @Test
    fun `markAppKilled returns 0 when routeId not found`() =
        runTest {
            val fakeDao = FakeAppKilledSavedTrackDao()
            val repo = SavedTrackRepository(fakeDao)

            val result = repo.markAppKilled("does-not-exist")

            assertEquals(0, result)
            assertEquals(emptyList(), fakeDao.killedRouteIds)
        }

    @Test
    fun `markAppKilled records each call independently`() =
        runTest {
            val fakeDao = FakeAppKilledSavedTrackDao()
            fakeDao.routeIdsInDb += "r1"
            val repo = SavedTrackRepository(fakeDao)

            repo.markAppKilled("r1")
            repo.markAppKilled("r1")

            assertEquals(2, fakeDao.killedRouteIds.size)
        }
}

// ── Minimal fake (only markAppKilled matters for this test) ──────────────────

private class FakeAppKilledSavedTrackDao : SavedTrackDao {
    val routeIdsInDb = mutableListOf<String>()
    val killedRouteIds = mutableListOf<String>()

    override suspend fun markAppKilled(routeId: String): Int {
        if (routeId !in routeIdsInDb) return 0
        killedRouteIds += routeId
        return 1
    }

    // ── Everything else unused ─────────────────────────────────────────────
    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {}

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

    override suspend fun deleteSavedTrack(track: SavedTrack) {}

    override suspend fun deleteSavedTrack(routeId: String) {}

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

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

    override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = emptyList()

    override suspend fun markLocalDataPurged(routeId: String) {}

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun markPhoneShutDown(routeId: String): Int = 0
}
