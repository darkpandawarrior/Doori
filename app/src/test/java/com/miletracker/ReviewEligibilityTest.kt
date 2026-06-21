package com.miletracker

import com.miletracker.core.platform.ReviewEligibility
import com.miletracker.core.platform.ReviewGateConfig
import com.miletracker.core.platform.ReviewState
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** RV.1: engagement-gating rules for the in-app review prompt. */
class ReviewEligibilityTest {
    private val day = 24L * 60L * 60L * 1000L
    private val now = 100L * day
    private val config = ReviewGateConfig(minAccountAgeDays = 3, minInteractions = 5, cooldownDays = 30)

    @Test
    fun `eligible when account old enough, enough interactions, never prompted`() {
        val state = ReviewState(firstOpenMillis = now - 10 * day, interactionCount = 5, lastPromptMillis = 0L)
        assertTrue(ReviewEligibility.isEligible(state, now, config))
    }

    @Test
    fun `not eligible when first open is unset`() {
        val state = ReviewState(firstOpenMillis = 0L, interactionCount = 99)
        assertFalse(ReviewEligibility.isEligible(state, now, config))
    }

    @Test
    fun `not eligible when account too young`() {
        val state = ReviewState(firstOpenMillis = now - 2 * day, interactionCount = 50)
        assertFalse(ReviewEligibility.isEligible(state, now, config))
    }

    @Test
    fun `not eligible when too few interactions`() {
        val state = ReviewState(firstOpenMillis = now - 10 * day, interactionCount = 4)
        assertFalse(ReviewEligibility.isEligible(state, now, config))
    }

    @Test
    fun `not eligible within cooldown after a prompt`() {
        val state =
            ReviewState(
                firstOpenMillis = now - 60 * day,
                interactionCount = 50,
                lastPromptMillis = now - 10 * day,
            )
        assertFalse(ReviewEligibility.isEligible(state, now, config))
    }

    @Test
    fun `eligible again after cooldown elapses`() {
        val state =
            ReviewState(
                firstOpenMillis = now - 60 * day,
                interactionCount = 50,
                lastPromptMillis = now - 31 * day,
            )
        assertTrue(ReviewEligibility.isEligible(state, now, config))
    }

    @Test
    fun `boundary - exactly min age and min interactions is eligible`() {
        val state = ReviewState(firstOpenMillis = now - 3 * day, interactionCount = 5, lastPromptMillis = 0L)
        assertTrue(ReviewEligibility.isEligible(state, now, config))
    }
}
