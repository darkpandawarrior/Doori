package com.miletracker

import com.miletracker.core.platform.AppReviewManager
import com.miletracker.core.platform.InMemoryReviewStateStore
import com.miletracker.core.platform.ReviewGateConfig
import com.miletracker.core.platform.ReviewState
import com.miletracker.core.platform.ReviewTracker
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** RV.4 — engagement-driven review tracker. */
class ReviewTrackerTest {
    private val day = 24L * 60L * 60L * 1000L
    private val config = ReviewGateConfig(minAccountAgeDays = 3, minInteractions = 2, cooldownDays = 30)

    private class CountingReviewManager : AppReviewManager {
        var prompts = 0
        override suspend fun promptForReview() {
            prompts++
        }
    }

    @Test
    fun `recordFirstOpenIfNeeded sets first open once`() =
        runTest {
            val store = InMemoryReviewStateStore()
            var clock = 1000L
            val tracker = ReviewTracker(store, config) { clock }
            tracker.recordFirstOpenIfNeeded()
            val first = store.load().firstOpenMillis
            clock = 5000L
            tracker.recordFirstOpenIfNeeded()
            assertEquals(first, store.load().firstOpenMillis)
        }

    @Test
    fun `recordInteraction increments the counter`() =
        runTest {
            val store = InMemoryReviewStateStore()
            val tracker = ReviewTracker(store, config) { 0L }
            tracker.recordInteraction()
            tracker.recordInteraction()
            assertEquals(2, store.load().interactionCount)
        }

    @Test
    fun `tryPrompt does nothing when not eligible`() =
        runTest {
            val store = InMemoryReviewStateStore(ReviewState(firstOpenMillis = 100 * day, interactionCount = 0))
            val tracker = ReviewTracker(store, config) { 100 * day }
            val manager = CountingReviewManager()
            assertFalse(tracker.tryPrompt(manager))
            assertEquals(0, manager.prompts)
        }

    @Test
    fun `tryPrompt prompts and stamps last prompt when eligible`() =
        runTest {
            val now = 100 * day
            val store =
                InMemoryReviewStateStore(ReviewState(firstOpenMillis = now - 10 * day, interactionCount = 2))
            val tracker = ReviewTracker(store, config) { now }
            val manager = CountingReviewManager()
            assertTrue(tracker.tryPrompt(manager))
            assertEquals(1, manager.prompts)
            assertEquals(now, store.load().lastPromptMillis)
        }

    @Test
    fun `tryPrompt respects cooldown after a prompt`() =
        runTest {
            var now = 100 * day
            val store =
                InMemoryReviewStateStore(ReviewState(firstOpenMillis = now - 10 * day, interactionCount = 5))
            val tracker = ReviewTracker(store, config) { now }
            val manager = CountingReviewManager()
            assertTrue(tracker.tryPrompt(manager))
            now += 5 * day
            assertFalse(tracker.tryPrompt(manager))
            assertEquals(1, manager.prompts)
        }
}
