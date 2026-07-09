package com.mileway.feature.tracking.watch

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.core.data.model.display.InMemorySnapshotPublisher
import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P1.2: fake-backed coverage for [WatchFacade] — the thin watchos-safe seam over
 * [InMemorySnapshotPublisher], [SavedTrackRepository] and [TrackingController].
 */
class WatchFacadeTest {
    @Test
    fun `observeSnapshot emits the current published snapshot`() =
        runTest {
            val publisher = InMemorySnapshotPublisher()
            val facade = WatchFacade(publisher, SavedTrackRepository(FakeWatchDao()), RecordingTrackingController())

            assertEquals(SurfaceSnapshot(), facade.observeSnapshot().first())
        }

    @Test
    fun `observeSnapshot emits again when a new snapshot is published (trip change)`() =
        runTest {
            val publisher = InMemorySnapshotPublisher()
            val facade = WatchFacade(publisher, SavedTrackRepository(FakeWatchDao()), RecordingTrackingController())

            val updated = SurfaceSnapshot(todayDistanceKm = 12.5, todayTrips = 1)
            publisher.publish(updated)

            assertEquals(updated, facade.observeSnapshot().first())
        }

    @Test
    fun `recentTrips maps completed tracks newest-first, capped at limit`() =
        runTest {
            val dao =
                FakeWatchDao(
                    listOf(
                        completedTrack(routeId = "a", name = "Oldest", distanceM = 1_000.0, endTime = 100L),
                        completedTrack(routeId = "b", name = "Newest", distanceM = 2_000.0, endTime = 300L),
                        completedTrack(routeId = "c", name = "Middle", distanceM = 3_000.0, endTime = 200L),
                    ),
                )
            val facade = WatchFacade(InMemorySnapshotPublisher(), SavedTrackRepository(dao), RecordingTrackingController())

            val trips = facade.recentTrips(limit = 2).first()

            assertEquals(listOf("b", "c"), trips.map { it.id })
            assertEquals(2.0, trips.first().km)
        }

    @Test
    fun `startTracking proxies to the controller with a generated token`() =
        runTest {
            val controller = RecordingTrackingController()
            val facade = WatchFacade(InMemorySnapshotPublisher(), SavedTrackRepository(FakeWatchDao()), controller)

            facade.startTracking()

            assertEquals(1, controller.startedTokens.size)
            assertTrue(controller.startedTokens.single().isNotBlank())
        }

    @Test
    fun `stopTracking proxies to the controller using the active track's routeId`() =
        runTest {
            val active = activeTrack(routeId = "active-route")
            val dao = FakeWatchDao(listOf(active))
            val controller = RecordingTrackingController()
            val facade = WatchFacade(InMemorySnapshotPublisher(), SavedTrackRepository(dao), controller)

            facade.stopTracking()

            assertEquals(listOf("active-route"), controller.stoppedTokens)
        }

    @Test
    fun `stopTracking is a no-op when there is no active track`() =
        runTest {
            val controller = RecordingTrackingController()
            val facade = WatchFacade(InMemorySnapshotPublisher(), SavedTrackRepository(FakeWatchDao()), controller)

            facade.stopTracking()

            assertTrue(controller.stoppedTokens.isEmpty())
        }

    @Test
    fun `pauseTracking proxies to the controller using the active track's routeId`() =
        runTest {
            val active = activeTrack(routeId = "active-route")
            val dao = FakeWatchDao(listOf(active))
            val controller = RecordingTrackingController()
            val facade = WatchFacade(InMemorySnapshotPublisher(), SavedTrackRepository(dao), controller)

            facade.pauseTracking()

            assertEquals(listOf("active-route"), controller.pausedTokens)
        }

    @Test
    fun `discardTracking proxies to the controller's stop using the active track's routeId`() =
        runTest {
            val active = activeTrack(routeId = "active-route")
            val dao = FakeWatchDao(listOf(active))
            val controller = RecordingTrackingController()
            val facade = WatchFacade(InMemorySnapshotPublisher(), SavedTrackRepository(dao), controller)

            facade.discardTracking()

            assertEquals(listOf("active-route"), controller.stoppedTokens)
        }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun completedTrack(
        routeId: String,
        name: String,
        distanceM: Double,
        endTime: Long,
    ) = SavedTrack(
        routeId = routeId,
        name = name,
        isCompleted = true,
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = endTime - 1_000L, endTime = endTime,
        distance = distanceM, duration = 1_000L,
        createdAt = endTime,
    )

    private fun activeTrack(routeId: String) =
        SavedTrack(
            routeId = routeId,
            name = "Active",
            startLatitude = 0.0, startLongitude = 0.0,
            endLatitude = 0.0, endLongitude = 0.0,
            pausedLatitude = 0.0, pausedLongitude = 0.0,
            startTime = 1_000L, endTime = -1L,
            distance = 0.0, duration = 0L,
            createdAt = 1_000L,
        )
}

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class RecordingTrackingController : TrackingController {
    val startedTokens = mutableListOf<String>()
    val pausedTokens = mutableListOf<String>()
    val stoppedTokens = mutableListOf<String>()

    override fun start(token: String) {
        startedTokens += token
    }

    override fun pause(token: String) {
        pausedTokens += token
    }

    override fun resume(token: String) {}

    override fun stop(token: String) {
        stoppedTokens += token
    }
}

private class FakeWatchDao(seed: List<SavedTrack> = emptyList()) : SavedTrackDao {
    // P10.1: stale-fake catch-up — SavedTrackDao.updateSmartDistanceFinal was added by the
    // SmartDistance commit without updating these test fakes; no-op override so this test source
    // set compiles (pre-existing breakage, incidental to P10.1).
    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    private val tracks = seed.toMutableList()

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = tracks.firstOrNull { it.routeId == routeId }

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {
        tracks += savedTrack
    }

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

    override suspend fun deleteSavedTrack(track: SavedTrack) {}

    override suspend fun deleteSavedTrack(routeId: String) {}

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = flowOf(tracks.toList())

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = MutableStateFlow(tracks.filter { it.isCompleted })

    override suspend fun count(): Long = tracks.size.toLong()

    override suspend fun getActiveTrack(): SavedTrack? = tracks.firstOrNull { it.endTime < 0L }

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
