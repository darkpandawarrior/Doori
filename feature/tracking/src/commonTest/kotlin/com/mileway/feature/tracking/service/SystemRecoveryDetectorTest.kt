package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * P-C.2: verifies that SystemRecoveryDetector writes the FG-terminated flag only when
 * [isSystemRelaunch] is true, and is a no-op otherwise.
 */
class SystemRecoveryDetectorTest {
    @Test
    fun `system relaunch with valid token marks fg terminated and returns true`() =
        runTest {
            val dao = FakeSystemRecoveryDao()
            dao.routeIdsInDb += "tok-1"
            val detector = SystemRecoveryDetector(SavedTrackRepository(dao))

            val result = detector.handleIfSystemRecovery("tok-1", isSystemRelaunch = true)

            assertTrue(result)
            assertEquals(listOf("tok-1"), dao.fgTerminatedRouteIds)
        }

    @Test
    fun `non-system relaunch does not write flag`() =
        runTest {
            val dao = FakeSystemRecoveryDao()
            dao.routeIdsInDb += "tok-2"
            val detector = SystemRecoveryDetector(SavedTrackRepository(dao))

            val result = detector.handleIfSystemRecovery("tok-2", isSystemRelaunch = false)

            assertFalse(result)
            assertEquals(emptyList(), dao.fgTerminatedRouteIds)
        }

    @Test
    fun `empty token is a no-op even when system relaunch`() =
        runTest {
            val dao = FakeSystemRecoveryDao()
            val detector = SystemRecoveryDetector(SavedTrackRepository(dao))

            val result = detector.handleIfSystemRecovery("", isSystemRelaunch = true)

            assertFalse(result)
            assertEquals(emptyList(), dao.fgTerminatedRouteIds)
        }
}

// ── Fake ──────────────────────────────────────────────────────────────────────

private class FakeSystemRecoveryDao : SavedTrackDao {
    val routeIdsInDb = mutableListOf<String>()
    val fgTerminatedRouteIds = mutableListOf<String>()

    override suspend fun markFgTerminated(routeId: String): Int {
        if (routeId !in routeIdsInDb) return 0
        fgTerminatedRouteIds += routeId
        return 1
    }

    override suspend fun markAppKilled(routeId: String): Int = 0

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

    override suspend fun markPhoneShutDown(routeId: String): Int = 0
}
