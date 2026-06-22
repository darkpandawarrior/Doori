package com.miletracker.feature.tracking.service

import com.miletracker.core.data.dao.SavedTrackDao
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.data.model.db.TrackMetrics
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * P-C.3: verifies ShutdownFlagPolicy consume-once semantics.
 */
class ShutdownFlagPolicyTest {
    @Test
    fun `consume-once - flag consumed and markPhoneShutDown called when flag is set`() =
        runTest {
            val store = FakeShutdownFlagStore(isPending = true)
            val dao = FakeShutdownDao()
            dao.routeIdsInDb += "tok-sd"
            val policy = ShutdownFlagPolicy(store, SavedTrackRepository(dao))

            val consumed = policy.consumeAndMark("tok-sd")

            assertTrue(consumed)
            assertFalse(store.isPending)
            assertEquals(listOf("tok-sd"), dao.phoneShutDownRouteIds)
        }

    @Test
    fun `flag not set returns false and does not write to db`() =
        runTest {
            val store = FakeShutdownFlagStore(isPending = false)
            val dao = FakeShutdownDao()
            val policy = ShutdownFlagPolicy(store, SavedTrackRepository(dao))

            val consumed = policy.consumeAndMark("tok-x")

            assertFalse(consumed)
            assertEquals(emptyList(), dao.phoneShutDownRouteIds)
        }

    @Test
    fun `empty token is a no-op even when flag is set`() =
        runTest {
            val store = FakeShutdownFlagStore(isPending = true)
            val dao = FakeShutdownDao()
            val policy = ShutdownFlagPolicy(store, SavedTrackRepository(dao))

            val consumed = policy.consumeAndMark("")

            assertFalse(consumed)
            // Flag must NOT be consumed — token was invalid so we don't know which row to mark.
            assertTrue(store.isPending)
        }

    @Test
    fun `second call after consume returns false when flag cleared`() =
        runTest {
            val store = FakeShutdownFlagStore(isPending = true)
            val dao = FakeShutdownDao()
            dao.routeIdsInDb += "tok-a"
            val policy = ShutdownFlagPolicy(store, SavedTrackRepository(dao))

            policy.consumeAndMark("tok-a") // first call — consumes
            val second = policy.consumeAndMark("tok-a") // second call — flag gone

            assertFalse(second)
            assertEquals(1, dao.phoneShutDownRouteIds.size)
        }
}

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeShutdownFlagStore(var isPending: Boolean) : ShutdownFlagStore {
    override fun set() {
        isPending = true
    }

    override fun consumeAndClear(): Boolean {
        val was = isPending
        isPending = false
        return was
    }
}

private class FakeShutdownDao : SavedTrackDao {
    val routeIdsInDb = mutableListOf<String>()
    val phoneShutDownRouteIds = mutableListOf<String>()

    override suspend fun markPhoneShutDown(routeId: String): Int {
        if (routeId !in routeIdsInDb) return 0
        phoneShutDownRouteIds += routeId
        return 1
    }

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

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
}
