package com.mileway

import app.cash.turbine.test
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.feature.agent.engine.AssistantChunk
import com.mileway.feature.agent.engine.OfflineAssistantEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Clock

class OfflineAssistantEngineTest {

    private val now = Clock.System.now().toEpochMilliseconds()
    private val mockDao = mockk<SavedTrackDao>(relaxed = true)
    private val engine = OfflineAssistantEngine(mockDao)

    private fun track(routeId: String, distanceKm: Double, endOffsetMs: Long = 1_000L) = SavedTrack(
        routeId = routeId,
        name = "Trip $routeId",
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = now - endOffsetMs - 3_600_000L,
        endTime = now - endOffsetMs,
        distance = distanceKm,
        duration = 3_600_000L,
        isCompleted = true,
    )

    private suspend fun collectDone(userMsg: String): String {
        var doneText = ""
        engine.respond("conv-1", userMsg, 0).test {
            while (true) {
                val item = awaitItem()
                if (item is AssistantChunk.Done) { doneText = item.fullText; break }
            }
            cancelAndIgnoreRemainingEvents()
        }
        return doneText
    }

    @Test
    fun `mileage week reply reflects actual seeded trips`() = runTest {
        every { mockDao.getCompletedTracks() } returns flowOf(listOf(track("r1", 50.0), track("r2", 30.0)))
        val reply = collectDone("how many km this week")
        assertTrue(reply.contains("80 km"), "Expected 80 km, got: $reply")
        assertTrue(reply.contains("2 trip"), "Expected 2 trips, got: $reply")
    }

    @Test
    fun `mileage week with no trips returns informative message`() = runTest {
        every { mockDao.getCompletedTracks() } returns flowOf(emptyList())
        val reply = collectDone("how many km this week")
        assertTrue(reply.contains("haven't tracked"), "Expected empty message, got: $reply")
    }

    @Test
    fun `card balance reply returns static card data`() = runTest {
        val reply = collectDone("what is my card balance")
        assertTrue(reply.contains("₹48,000"), "Expected card balance, got: $reply")
    }

    @Test
    fun `title suggestion set on first message`() = runTest {
        engine.respond("conv-1", "What is the mileage rate?", 0).test {
            var titleSuggestion: String? = null
            while (true) {
                val item = awaitItem()
                if (item is AssistantChunk.Done) { titleSuggestion = item.titleSuggestion; break }
            }
            cancelAndIgnoreRemainingEvents()
            assertTrue(titleSuggestion != null)
        }
    }

    @Test
    fun `title suggestion null on follow-up messages`() = runTest {
        engine.respond("conv-1", "What is the mileage rate?", 3).test {
            var titleSuggestion: String? = "non-null"
            while (true) {
                val item = awaitItem()
                if (item is AssistantChunk.Done) { titleSuggestion = item.titleSuggestion; break }
            }
            cancelAndIgnoreRemainingEvents()
            assertTrue(titleSuggestion == null)
        }
    }

    @Test
    fun `first chunk is always a Thinking chunk`() = runTest {
        var first: AssistantChunk? = null
        engine.respond("conv-1", "hello", 0).test {
            first = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(first is AssistantChunk.Thinking, "Expected Thinking, got: $first")
    }
}
