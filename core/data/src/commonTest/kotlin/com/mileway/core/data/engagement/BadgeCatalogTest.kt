package com.mileway.core.data.engagement

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** PLAN_V24 P12.1 — the pure milestone rule ([computeEarnedBadges] / [longestDayStreak]). */
class BadgeCatalogTest {
    private fun trip(
        km: Double,
        day: Long,
    ) = BadgeTrip(distanceKm = km, dayEpoch = day)

    @Test
    fun `no trips earns nothing`() {
        assertTrue(computeEarnedBadges(emptyList()).isEmpty())
    }

    @Test
    fun `a single trip earns first trip only`() {
        val earned = computeEarnedBadges(listOf(trip(3.0, 100)))
        assertEquals(setOf(BadgeId.FIRST_TRIP), earned)
    }

    @Test
    fun `ten trips earns the ten-trip badge`() {
        val earned = computeEarnedBadges((0 until 10).map { trip(1.0, it.toLong()) })
        assertTrue(BadgeId.TEN_TRIPS in earned)
        assertTrue(BadgeId.FIRST_TRIP in earned)
    }

    @Test
    fun `nine trips does not earn the ten-trip badge`() {
        val earned = computeEarnedBadges((0 until 9).map { trip(1.0, it.toLong()) })
        assertFalse(BadgeId.TEN_TRIPS in earned)
    }

    @Test
    fun `summed distance at or above 100 km earns the distance badge`() {
        val earned = computeEarnedBadges(listOf(trip(60.0, 1), trip(40.0, 2)))
        assertTrue(BadgeId.HUNDRED_KM in earned)
    }

    @Test
    fun `negative distances are ignored for the distance badge`() {
        val earned = computeEarnedBadges(listOf(trip(80.0, 1), trip(-50.0, 2), trip(19.0, 3)))
        assertFalse(BadgeId.HUNDRED_KM in earned)
    }

    @Test
    fun `seven consecutive days earns the streak badge`() {
        val earned = computeEarnedBadges((0 until 7).map { trip(1.0, 200L + it) })
        assertTrue(BadgeId.WEEK_STREAK in earned)
    }

    @Test
    fun `a broken run of days does not earn the streak badge`() {
        val days = listOf(200L, 201L, 202L, 204L, 205L, 206L, 207L)
        val earned = computeEarnedBadges(days.map { trip(1.0, it) })
        assertFalse(BadgeId.WEEK_STREAK in earned)
    }

    @Test
    fun `duplicate days on the same day do not inflate the streak`() {
        val days = listOf(1L, 1L, 1L, 2L, 2L, 3L)
        assertEquals(3, longestDayStreak(days))
    }
}
