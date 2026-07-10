package com.mileway.core.platform

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PLAN_V24 P12.3: the native-sheet review gate over [ReviewTracker.shouldPrompt] / [markPrompted]
 * with the plan's 7-day account-age config, using an in-memory store and a fixed clock.
 */
class ReviewTrackerTest {
    private val day = 24L * 60L * 60L * 1000L
    private val config = ReviewGateConfig(minAccountAgeDays = 7, minInteractions = 5, cooldownDays = 30)

    private fun trackerAt(
        nowMillis: Long,
        initial: ReviewState,
    ) = ReviewTracker(store = InMemoryReviewStateStore(initial), config = config, now = { nowMillis })

    @Test
    fun `not eligible before 7-day account age even with enough interactions`() =
        runTest {
            val now = 6 * day
            val tracker = trackerAt(now, ReviewState(firstOpenMillis = 1, interactionCount = 5))
            assertFalse(tracker.shouldPrompt())
        }

    @Test
    fun `eligible after 7 days with enough interactions`() =
        runTest {
            val now = 8 * day
            val tracker = trackerAt(now, ReviewState(firstOpenMillis = 1, interactionCount = 5))
            assertTrue(tracker.shouldPrompt())
        }

    @Test
    fun `not eligible without enough interactions`() =
        runTest {
            val now = 30 * day
            val tracker = trackerAt(now, ReviewState(firstOpenMillis = 1, interactionCount = 4))
            assertFalse(tracker.shouldPrompt())
        }

    @Test
    fun `markPrompted stamps the last-prompt time and starts the cooldown`() =
        runTest {
            val now = 10 * day
            val store = InMemoryReviewStateStore(ReviewState(firstOpenMillis = 1, interactionCount = 5))
            val tracker = ReviewTracker(store = store, config = config, now = { now })
            assertTrue(tracker.shouldPrompt())

            tracker.markPrompted()
            assertEquals(now, store.load().lastPromptMillis)
            // Within the 30-day cooldown it is no longer eligible.
            assertFalse(tracker.shouldPrompt())
        }
}
