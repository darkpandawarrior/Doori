package com.mileway.feature.agent

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.feature.agent.engine.AssistantChunk
import com.mileway.feature.agent.engine.OfflineAssistantEngine
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// ── Fake DAO ──────────────────────────────────────────────────────────────────

private class FakeSavedTrackDao(tracks: List<SavedTrack> = emptyList()) : SavedTrackDao {
    private val _flow = MutableStateFlow(tracks)

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = _flow
    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = _flow
    override suspend fun insertSavedTrack(savedTrack: SavedTrack) = Unit
    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0
    override suspend fun deleteSavedTrack(track: SavedTrack) = Unit
    override suspend fun deleteSavedTrack(routeId: String) = Unit
    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0
    override suspend fun count(): Long = _flow.value.size.toLong()
    override suspend fun getActiveTrack(): SavedTrack? = null
    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null
    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())
    override fun observeTrackById(routeId: String): Flow<SavedTrack?> = flowOf(null)
    override suspend fun getMostRecentActiveTrack(): SavedTrack? = null
    override suspend fun getLastCompletedTrack(): SavedTrack? = null
    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = null
    override fun getRetainedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())
    override fun getTracksInRange(start: Long, end: Long): Flow<List<SavedTrack>> = flowOf(emptyList())
    override fun getTracksInRangeExcludingRetained(start: Long, end: Long): Flow<List<SavedTrack>> = flowOf(emptyList())
    override suspend fun countInRangeExcludingRetained(start: Long, end: Long): Int = 0
    override suspend fun updateTrackName(routeId: String, name: String) = Unit
    override suspend fun updateTrackLiveData(routeId: String, distance: Double, duration: Long) = Unit
    override suspend fun markTrackDraft(routeId: String, draftSavedAt: Long): Int = 0
    override suspend fun updateSubmissionTime(routeId: String, submissionTime: Long): Int = 0
    override suspend fun finalizeTrack(routeId: String, endTime: Long, finalDistance: Double, avgSpeed: Double, maxSpeed: Double) = Unit
    override suspend fun markTrackCompleted(routeId: String, trackingActivity: String, currentTime: Long, newName: String, submittedAmount: Double, submittedAmountCurrency: String, transId: String?): Int = 0
    override suspend fun markTrackEndedLocally(routeId: String, trackingActivity: String, currentTime: Long, newName: String): Int = 0
    override suspend fun markRetained(routeIds: List<String>) = Unit
    override suspend fun markRetainedBefore(threshold: Long): Int = 0
    override suspend fun setRetained(routeId: String, retained: Boolean) = Unit
    override suspend fun deleteCorruptedTracks(): Int = 0
    override suspend fun getCorruptedTrackCount(): Int = 0
    override suspend fun deleteOlderThanExcludingRetained(threshold: Long): Int = 0
    override suspend fun getLastNRouteIdsFromRange(start: Long, end: Long, limit: Int): List<String> = emptyList()
    override suspend fun getAverageTrackMetrics(): TrackMetrics = TrackMetrics(0.0, 0L, 0f, 0)
    override suspend fun getPreviousSimilarTrack(routeId: String): SavedTrack? = null
    override suspend fun getSimilarTracks(routeId: String): List<SavedTrack> = emptyList()
    override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = emptyList()
    override suspend fun markLocalDataPurged(routeId: String) = Unit
    override suspend fun markAppKilled(routeId: String): Int = 0
    override suspend fun markFgTerminated(routeId: String): Int = 0
    override suspend fun markPhoneShutDown(routeId: String): Int = 0
}

private fun fakeTrack(routeId: String, distanceKm: Double, endTimeMs: Long): SavedTrack =
    SavedTrack(
        routeId = routeId,
        name = routeId,
        isCompleted = true,
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = endTimeMs - 60_000L,
        endTime = endTimeMs,
        distance = distanceKm,
        duration = 60_000L,
    )

// ── Tests ─────────────────────────────────────────────────────────────────────

class OfflineAssistantEngineTest {

    @Test
    fun `respond emits Thinking then Tokens then Done`() = runTest {
        val engine = OfflineAssistantEngine(FakeSavedTrackDao())
        val chunks = engine.respond("conv1", "hello", 0).toList()
        assertTrue(chunks.first() is AssistantChunk.Thinking, "first chunk should be Thinking")
        assertTrue(chunks.last() is AssistantChunk.Done, "last chunk should be Done")
        val tokens = chunks.filterIsInstance<AssistantChunk.Token>()
        assertTrue(tokens.isNotEmpty(), "should emit at least one Token")
    }

    @Test
    fun `Done chunk fullText matches concatenated tokens`() = runTest {
        val engine = OfflineAssistantEngine(FakeSavedTrackDao())
        val chunks = engine.respond("conv1", "hello", 0).toList()
        val concatenated = chunks.filterIsInstance<AssistantChunk.Token>().joinToString("") { it.text }
        val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
        assertEquals(done.fullText.trim(), concatenated.trim())
    }

    @Test
    fun `mileage query with no tracks returns zero-trip message`() = runTest {
        val engine = OfflineAssistantEngine(FakeSavedTrackDao(emptyList()))
        val chunks = engine.respond("conv1", "km this week", 0).toList()
        val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
        assertTrue(done.fullText.contains("haven't tracked", ignoreCase = true))
    }

    @Test
    fun `mileage query with recent track returns grounded km count`() = runTest {
        val recentEndMs = Clock.System.now().toEpochMilliseconds() - 60_000L
        val track = fakeTrack("r1", distanceKm = 42.0, endTimeMs = recentEndMs)
        val engine = OfflineAssistantEngine(FakeSavedTrackDao(listOf(track)))
        val chunks = engine.respond("conv1", "km this week", 0).toList()
        val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
        assertTrue(done.fullText.contains("42"), "reply should contain the distance '42'")
        assertTrue(done.fullText.contains("1 trip", ignoreCase = true) || done.fullText.contains("1trip", ignoreCase = true))
    }

    @Test
    fun `mileage query with old track (beyond 7 days) returns zero-trip message`() = runTest {
        val oldEnd = 1_000_000_000L
        val track = fakeTrack("r1", distanceKm = 100.0, endTimeMs = oldEnd)
        val engine = OfflineAssistantEngine(FakeSavedTrackDao(listOf(track)))
        val chunks = engine.respond("conv1", "km this week", 0).toList()
        val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
        assertTrue(done.fullText.contains("haven't tracked", ignoreCase = true))
    }

    @Test
    fun `first-turn reply includes title suggestion`() = runTest {
        val engine = OfflineAssistantEngine(FakeSavedTrackDao())
        val chunks = engine.respond("conv1", "what is policy cap", 0).toList()
        val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
        assertNotNull(done.titleSuggestion, "first-turn Done should carry a title suggestion")
    }

    @Test
    fun `subsequent-turn reply omits title suggestion`() = runTest {
        val engine = OfflineAssistantEngine(FakeSavedTrackDao())
        val chunks = engine.respond("conv1", "what is policy cap", historySize = 3).toList()
        val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
        assertEquals(null, done.titleSuggestion)
    }

    @Test
    fun `generic fallback reply is non-empty`() = runTest {
        val engine = OfflineAssistantEngine(FakeSavedTrackDao())
        val chunks = engine.respond("conv1", "xyzzy nonsense", 0).toList()
        val done = chunks.filterIsInstance<AssistantChunk.Done>().first()
        assertTrue(done.fullText.isNotBlank())
    }
}
