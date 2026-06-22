package com.miletracker.feature.tracking.service

import com.miletracker.core.data.dao.SavedTrackDao
import com.miletracker.core.data.model.db.CurrentTrackData
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.data.model.db.TrackMetrics
import com.miletracker.core.data.session.CurrentTrackDataSource
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** P-C.4: SessionReconciliationPolicy outcome coverage. */
class SessionReconciliationPolicyTest {
    private fun policy(
        session: CurrentTrackData,
        track: SavedTrack? = null,
    ): SessionReconciliationPolicy {
        val source = FakeCurrentTrackSource(session)
        val dao = FakeReconcileDao(track)
        return SessionReconciliationPolicy(source, SavedTrackRepository(dao))
    }

    @Test
    fun `no session in DataStore returns NoSession`() =
        runTest {
            val result = policy(CurrentTrackData(token = "")).reconcile()
            assertIs<SessionReconciliationPolicy.Outcome.NoSession>(result)
        }

    @Test
    fun `isTracking false with token returns NoSession`() =
        runTest {
            val result = policy(CurrentTrackData(token = "tok", isTracking = false)).reconcile()
            assertIs<SessionReconciliationPolicy.Outcome.NoSession>(result)
        }

    @Test
    fun `tracking session with no DB row returns DiscardStale`() =
        runTest {
            val result = policy(session = tracking("ghost"), track = null).reconcile()
            assertIs<SessionReconciliationPolicy.Outcome.DiscardStale>(result)
        }

    @Test
    fun `tracking session with completed track returns DiscardStale`() =
        runTest {
            val result = policy(session = tracking("t1"), track = track("t1", isCompleted = true)).reconcile()
            assertIs<SessionReconciliationPolicy.Outcome.DiscardStale>(result)
        }

    @Test
    fun `tracking session with discarded track returns DiscardStale`() =
        runTest {
            val result = policy(session = tracking("t2"), track = track("t2", isDiscarded = true)).reconcile()
            assertIs<SessionReconciliationPolicy.Outcome.DiscardStale>(result)
        }

    @Test
    fun `clean ongoing session returns Resume`() =
        runTest {
            val result = policy(session = tracking("t3"), track = track("t3")).reconcile()
            val resumed = assertIs<SessionReconciliationPolicy.Outcome.Resume>(result)
            assertEquals("t3", resumed.token)
        }

    @Test
    fun `app-killed session returns NeedsDecision with reason containing app-kill`() =
        runTest {
            val result = policy(tracking("t4"), track("t4", wasAppKilled = true)).reconcile()
            val decision = assertIs<SessionReconciliationPolicy.Outcome.NeedsDecision>(result)
            assertTrue(decision.reason.contains("app-kill"))
        }

    @Test
    fun `fgs-terminated session returns NeedsDecision`() =
        runTest {
            val result = policy(tracking("t5"), track("t5", foregroundServiceTerminated = true)).reconcile()
            assertIs<SessionReconciliationPolicy.Outcome.NeedsDecision>(result)
        }

    @Test
    fun `phone-shutdown session returns NeedsDecision`() =
        runTest {
            val result = policy(tracking("t6"), track("t6", wasPhoneShutDown = true)).reconcile()
            assertIs<SessionReconciliationPolicy.Outcome.NeedsDecision>(result)
        }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun tracking(token: String) = CurrentTrackData(token = token, isTracking = true)

    private fun track(
        routeId: String,
        isCompleted: Boolean = false,
        isDiscarded: Boolean = false,
        wasAppKilled: Boolean = false,
        foregroundServiceTerminated: Boolean = false,
        wasPhoneShutDown: Boolean = false,
    ) = SavedTrack(
        routeId = routeId,
        name = "Test",
        startTime = 1_000L,
        createdAt = 1_000L,
        isCompleted = isCompleted,
        isDiscarded = isDiscarded,
        wasAppKilled = wasAppKilled,
        foregroundServiceTerminated = foregroundServiceTerminated,
        wasPhoneShutDown = wasPhoneShutDown,
    )
}

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeCurrentTrackSource(private val initial: CurrentTrackData) : CurrentTrackDataSource {
    override val currentTrackFlow: Flow<CurrentTrackData> = MutableStateFlow(initial)

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

    override suspend fun clearSession() {}

    override suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    ) {}
}

private class FakeReconcileDao(private val track: SavedTrack?) : SavedTrackDao {
    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = track

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

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
