package com.mileway.core.data.session

import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.MockAccountEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeCurrentTrackDataSource(initial: CurrentTrackData = CurrentTrackData.empty()) : CurrentTrackDataSource {
    val flow = MutableStateFlow(initial)
    var clearCalls = 0
        private set

    override val currentTrackFlow: Flow<CurrentTrackData> = flow

    override suspend fun saveSession(data: CurrentTrackData) {
        flow.value = data
    }

    override suspend fun updateDistance(
        token: String,
        distanceMeters: Double,
        speed: Double,
        avgSpeed: Double,
    ) = Unit

    override suspend fun updateLocationCount(
        token: String,
        total: Long,
        unsynced: Long,
    ) = Unit

    override suspend fun markPaused(
        token: String,
        lat: Double,
        lng: Double,
    ) = Unit

    override suspend fun markResumed(token: String) = Unit

    override suspend fun markStopped(
        token: String,
        endLat: Double,
        endLng: Double,
    ) = Unit

    override suspend fun clearSession() {
        clearCalls++
        flow.value = CurrentTrackData.empty()
    }

    override suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    ) = Unit
}

private class FakeSavedTrackDao : SavedTrackDao {
    val tracks = mutableMapOf<String, SavedTrack>()

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {
        tracks[savedTrack.routeId] = savedTrack
    }

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int {
        tracks[savedTrack.routeId] = savedTrack
        return 1
    }

    override suspend fun deleteSavedTrack(track: SavedTrack) {
        tracks.remove(track.routeId)
    }

    override suspend fun deleteSavedTrack(routeId: String) {
        tracks.remove(routeId)
    }

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = flowOf(tracks.values.toList())

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun count(): Long = tracks.size.toLong()

    override suspend fun getActiveTrack(): SavedTrack? = null

    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? =
        tracks.values.firstOrNull { it.startedByEmployeeCode == employeeCode && !it.isCompleted && !it.isDiscarded && !it.isDraft }

    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun getMostRecentActiveTrack(): SavedTrack? = null

    override suspend fun getLastCompletedTrack(): SavedTrack? = null

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = tracks[routeId]

    override fun observeTrackById(routeId: String): Flow<SavedTrack?> = flowOf(tracks[routeId])

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
    ) {
        tracks[routeId]?.let { tracks[routeId] = it.copy(name = name) }
    }

    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    override suspend fun updateTrackLiveData(
        routeId: String,
        distance: Double,
        duration: Long,
    ) = Unit

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
    ) = Unit

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

    override suspend fun markRetained(routeIds: List<String>) = Unit

    override suspend fun markRetainedBefore(threshold: Long): Int = 0

    override suspend fun setRetained(
        routeId: String,
        retained: Boolean,
    ) = Unit

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

    override suspend fun markLocalDataPurged(routeId: String) = Unit

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int = 0

    override suspend fun markOdometerNotWorking(routeId: String): Int = 0
}

private class FakeMockAccountDao : MockAccountDao {
    val accounts = mutableMapOf<String, MockAccountEntity>()

    override fun observeAll(): Flow<List<MockAccountEntity>> = flowOf(accounts.values.toList())

    override suspend fun count(): Int = accounts.size

    override suspend fun getById(accountId: String): MockAccountEntity? = accounts[accountId]

    override suspend fun upsert(account: MockAccountEntity) {
        accounts[account.accountId] = account
    }

    override suspend fun upsertAll(accounts: List<MockAccountEntity>) {
        accounts.forEach { this.accounts[it.accountId] = it }
    }

    override suspend fun delete(accountId: String) {
        accounts.remove(accountId)
    }

    override suspend fun clearActive() {
        accounts.keys.toList().forEach { key -> accounts[key] = accounts.getValue(key).copy(isActive = false) }
    }

    override suspend fun markActive(accountId: String) {
        accounts[accountId]?.let { accounts[accountId] = it.copy(isActive = true) }
    }
}

private fun account(
    accountId: String,
    employeeCode: String,
) = MockAccountEntity(
    accountId = accountId,
    displayName = "Persona $accountId",
    employeeCode = employeeCode,
    organization = "Org",
    avatarSeed = accountId,
    isActive = false,
    lastLoginAtMs = 0L,
    createdAtMs = 0L,
)

private fun track(
    routeId: String,
    employeeCode: String,
    isCompleted: Boolean = false,
) = SavedTrack(
    routeId = routeId,
    name = "Journey $routeId",
    isCompleted = isCompleted,
    startedByEmployeeCode = employeeCode,
    startLatitude = 0.0, startLongitude = 0.0,
    endLatitude = 0.0, endLongitude = 0.0,
    pausedLatitude = 0.0, pausedLongitude = 0.0,
    startTime = 0L, endTime = if (isCompleted) 1L else -1L,
    distance = 100.0, duration = 1_000L,
)

/**
 * PLAN_V22 P3.4: covers the pause/restore/no-op paths of [MockAccountSessionCoordinator
 * .onPersonaSwitch] against deterministic in-memory fakes (never a real Room/DataStore
 * instance — this is pure coordination logic over the two data sources).
 */
class MockAccountSessionCoordinatorTest {
    @Test
    fun `switching persona mid-trip pauses the outgoing trip and clears the live session`() =
        runTest {
            val accountDao = FakeMockAccountDao().apply { accounts["ACC-002"] = account("ACC-002", "EMP-002") }
            val trackDao = FakeSavedTrackDao().apply { tracks["route-1"] = track("route-1", "EMP-001") }
            val liveSession = FakeCurrentTrackDataSource(CurrentTrackData(token = "route-1", isTracking = true, startedByEmployeeCode = "EMP-001"))
            val coordinator = MockAccountSessionCoordinator(liveSession, trackDao, accountDao)

            val outcome = coordinator.onPersonaSwitch("ACC-002").getOrThrow()

            assertTrue(outcome is MockAccountSessionCoordinator.Outcome.Paused)
            assertEquals("route-1", (outcome as MockAccountSessionCoordinator.Outcome.Paused).pausedRouteId)
            assertEquals(PERSONA_SWITCH_PAUSE_NAME, trackDao.tracks.getValue("route-1").name)
            assertEquals(1, liveSession.clearCalls)
        }

    @Test
    fun `switching persona restores the incoming persona's own paused trip`() =
        runTest {
            val accountDao = FakeMockAccountDao().apply { accounts["ACC-002"] = account("ACC-002", "EMP-002") }
            val trackDao =
                FakeSavedTrackDao().apply {
                    tracks["route-1"] = track("route-1", "EMP-001")
                    tracks["route-2"] = track("route-2", "EMP-002")
                }
            val liveSession = FakeCurrentTrackDataSource(CurrentTrackData(token = "route-1", isTracking = true, startedByEmployeeCode = "EMP-001"))
            val coordinator = MockAccountSessionCoordinator(liveSession, trackDao, accountDao)

            val outcome = coordinator.onPersonaSwitch("ACC-002").getOrThrow()

            assertTrue(outcome is MockAccountSessionCoordinator.Outcome.Paused)
            assertEquals("route-2", (outcome as MockAccountSessionCoordinator.Outcome.Paused).restoredRouteId)
            val restored = liveSession.flow.first()
            assertEquals("route-2", restored.token)
            assertTrue(restored.isTracking)
            assertEquals("EMP-002", restored.startedByEmployeeCode)
        }

    @Test
    fun `switching persona with no active trip is a silent no-op`() =
        runTest {
            val accountDao = FakeMockAccountDao().apply { accounts["ACC-002"] = account("ACC-002", "EMP-002") }
            val trackDao = FakeSavedTrackDao()
            val liveSession = FakeCurrentTrackDataSource()
            val coordinator = MockAccountSessionCoordinator(liveSession, trackDao, accountDao)

            val outcome = coordinator.onPersonaSwitch("ACC-002").getOrThrow()

            assertTrue(outcome is MockAccountSessionCoordinator.Outcome.NoActiveTrip)
            assertEquals(0, liveSession.clearCalls)
        }

    @Test
    fun `switching to the same account's own trip does not pause it`() =
        runTest {
            val accountDao = FakeMockAccountDao().apply { accounts["ACC-001"] = account("ACC-001", "EMP-001") }
            val trackDao = FakeSavedTrackDao().apply { tracks["route-1"] = track("route-1", "EMP-001") }
            val liveSession = FakeCurrentTrackDataSource(CurrentTrackData(token = "route-1", isTracking = true, startedByEmployeeCode = "EMP-001"))
            val coordinator = MockAccountSessionCoordinator(liveSession, trackDao, accountDao)

            val outcome = coordinator.onPersonaSwitch("ACC-001").getOrThrow()

            assertTrue(outcome is MockAccountSessionCoordinator.Outcome.NoActiveTrip)
            assertEquals(0, liveSession.clearCalls)
            assertEquals("Journey route-1", trackDao.tracks.getValue("route-1").name)
        }

    @Test
    fun `unknown target account never pauses the outgoing trip`() =
        runTest {
            val accountDao = FakeMockAccountDao()
            val trackDao = FakeSavedTrackDao().apply { tracks["route-1"] = track("route-1", "EMP-001") }
            val liveSession = FakeCurrentTrackDataSource(CurrentTrackData(token = "route-1", isTracking = true, startedByEmployeeCode = "EMP-001"))
            val coordinator = MockAccountSessionCoordinator(liveSession, trackDao, accountDao)

            val outcome = coordinator.onPersonaSwitch("ACC-GHOST").getOrThrow()

            assertTrue(outcome is MockAccountSessionCoordinator.Outcome.NoActiveTrip)
            assertEquals(0, liveSession.clearCalls)
            assertNull(trackDao.tracks["route-1"]?.let { if (it.name == PERSONA_SWITCH_PAUSE_NAME) it else null })
        }
}
