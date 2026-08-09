package com.mileway.wear

import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.core.data.model.display.TrackingState
import com.mileway.feature.tracking.service.TrackingNotificationMapper
import com.mileway.feature.tracking.service.TrackingSnapshot
import com.mileway.feature.tracking.watch.TripSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P2.4: JVM-unit coverage for the pure [WearPresentation] mapper (`:wear:testNoGmsDebugUnitTest`
 * per the plan's acceptance) — no Compose/Robolectric needed since it's plain data mapping.
 */
class WearPresentationTest {

    @Test
    fun `maps today and week distance straight through`() {
        val snapshot = SurfaceSnapshot(todayDistanceKm = 12.4, weekDistanceKm = 58.7)

        val uiState = WearPresentation.toUiState(snapshot)

        assertEquals(12.4, uiState.todayDistanceKm, 0.0)
        assertEquals(58.7, uiState.weekDistanceKm, 0.0)
    }

    @Test
    fun `maps isTracking through unchanged`() {
        assertEquals(true, WearPresentation.toUiState(SurfaceSnapshot(isTracking = true)).isTracking)
        assertEquals(false, WearPresentation.toUiState(SurfaceSnapshot(isTracking = false)).isTracking)
    }

    @Test
    fun `maps week goal km and derives progress from the snapshot`() {
        val snapshot = SurfaceSnapshot(weekDistanceKm = 50.0, weekGoalKm = 100.0)

        val uiState = WearPresentation.toUiState(snapshot)

        assertEquals(100.0, uiState.weekGoalKm, 0.0)
        assertEquals(0.5f, uiState.weekGoalProgress, 0.0001f)
    }

    @Test
    fun `week goal progress is clamped to 1f when distance exceeds the goal`() {
        val snapshot = SurfaceSnapshot(weekDistanceKm = 150.0, weekGoalKm = 100.0)

        val uiState = WearPresentation.toUiState(snapshot)

        assertEquals(1f, uiState.weekGoalProgress, 0.0001f)
    }

    @Test
    fun `default snapshot maps to a zeroed idle ui state`() {
        val uiState = WearPresentation.toUiState(SurfaceSnapshot())

        assertEquals(WearRootUiState(), uiState)
    }

    // ─── P2.5: toTripListItems ──────────────────────────────────────────────────────────────────

    @Test
    fun `maps trip summaries straight through to display rows`() {
        val trips =
            listOf(
                TripSummary(id = "t1", label = "Commute", km = 12.4, endMs = 1_000L),
                TripSummary(id = "t2", label = "Errand", km = 3.2, endMs = 2_000L),
            )

        val items = WearPresentation.toTripListItems(trips)

        assertEquals(2, items.size)
        assertEquals("t1", items[0].id)
        assertEquals("Commute", items[0].label)
        assertEquals(12.4, items[0].km, 0.0)
        assertEquals(1_000L, items[0].endMs)
        assertEquals("t2", items[1].id)
    }

    @Test
    fun `blank trip label falls back to a generic Trip label`() {
        val trip = TripSummary(id = "t1", label = "", km = 5.0, endMs = 1_000L)

        val items = WearPresentation.toTripListItems(listOf(trip))

        assertEquals("Trip", items.single().label)
    }

    @Test
    fun `empty trip list maps to an empty list of rows`() {
        val items = WearPresentation.toTripListItems(emptyList())

        assertTrue(items.isEmpty())
    }

    // ─── P2.6: toTodayDistanceLabel (tile/complication shared formatter) ───────────────────────────

    @Test
    fun `today distance label rounds to one decimal and appends the km unit`() {
        val snapshot = SurfaceSnapshot(todayDistanceKm = 12.449)

        val label = WearPresentation.toTodayDistanceLabel(snapshot)

        assertEquals("12.4 km", label)
    }

    @Test
    fun `today distance label for a zeroed snapshot is 0 point 0 km`() {
        val label = WearPresentation.toTodayDistanceLabel(SurfaceSnapshot())

        assertEquals("0.0 km", label)
    }

    // ─── P2.7: toWeekGoalValueLabel (RANGED_VALUE complication) ─────────────────────────────────

    @Test
    fun `week goal value label rounds week distance to one decimal without a unit suffix`() {
        val snapshot = SurfaceSnapshot(weekDistanceKm = 32.449, weekGoalKm = 100.0)

        val label = WearPresentation.toWeekGoalValueLabel(snapshot)

        assertEquals("32.4", label)
    }

    @Test
    fun `week goal value label for a zeroed snapshot is 0 point 0`() {
        val label = WearPresentation.toWeekGoalValueLabel(SurfaceSnapshot())

        assertEquals("0.0", label)
    }

    // ─── P2.8/AMBIENT.1: toOngoingActivityState (ongoing-activity live/idle + shared mapper copy) ──

    @Test
    fun `live tracking maps to an isLive ongoing state carrying the shared mapper's ACTIVE copy`() {
        val snapshot = TrackingSnapshot(state = TrackingState.LIVE_TRACKING, distanceMeters = 12_400.0)

        val ongoing = WearPresentation.toOngoingActivityState(snapshot)
        val expected = TrackingNotificationMapper.fromSnapshot(snapshot)

        assertTrue(ongoing.isLive)
        assertEquals(expected.title, ongoing.title)
        assertEquals(expected.text, ongoing.text)
    }

    @Test
    fun `paused tracking still counts as live and carries the mapper's PAUSED copy`() {
        val snapshot = TrackingSnapshot(state = TrackingState.PAUSED, distanceMeters = 5_000.0)

        val ongoing = WearPresentation.toOngoingActivityState(snapshot)
        val expected = TrackingNotificationMapper.fromSnapshot(snapshot)

        assertTrue(ongoing.isLive)
        assertEquals(expected.title, ongoing.title)
        assertEquals(expected.text, ongoing.text)
        assertTrue(ongoing.text.contains("resume"))
    }

    @Test
    fun `ready state maps to a non-live ongoing state and never calls the mapper`() {
        val ongoing = WearPresentation.toOngoingActivityState(TrackingSnapshot(state = TrackingState.READY))

        assertFalse(ongoing.isLive)
        assertEquals("", ongoing.title)
        assertEquals("", ongoing.text)
    }

    @Test
    fun `completed state maps to a non-live ongoing state, clearing the notification`() {
        val snapshot = TrackingSnapshot(state = TrackingState.COMPLETED, distanceMeters = 8_000.0)

        val ongoing = WearPresentation.toOngoingActivityState(snapshot)

        assertFalse(ongoing.isLive)
    }

    // ─── AMBIENT.1: toTileStatusLabel (tile/complication compact live-state word) ──────────────────

    @Test
    fun `idle snapshot has no tile status label`() {
        assertEquals(null, WearPresentation.toTileStatusLabel(SurfaceSnapshot()))
    }

    @Test
    fun `tracking snapshot maps to a TRACKING tile status label`() {
        val label = WearPresentation.toTileStatusLabel(SurfaceSnapshot(isTracking = true))

        assertEquals("TRACKING", label)
    }

    @Test
    fun `paused snapshot maps to a PAUSED tile status label`() {
        val label = WearPresentation.toTileStatusLabel(SurfaceSnapshot(isTracking = true, isPaused = true))

        assertEquals("PAUSED", label)
    }

    // ─── AMBIENT.1: isStale (frozen-cache detection for the tile/complication) ─────────────────────

    @Test
    fun `idle snapshot is never stale regardless of age`() {
        val snapshot = SurfaceSnapshot(isTracking = false, lastUpdatedEpochMs = 0L)

        assertFalse(WearPresentation.isStale(snapshot, nowEpochMs = 999_999_999L))
    }

    @Test
    fun `tracking snapshot within the threshold is not stale`() {
        val snapshot = SurfaceSnapshot(isTracking = true, lastUpdatedEpochMs = 1_000L)

        assertFalse(WearPresentation.isStale(snapshot, nowEpochMs = 1_000L + 60_000L, thresholdMs = 300_000L))
    }

    @Test
    fun `tracking snapshot past the threshold is stale`() {
        val snapshot = SurfaceSnapshot(isTracking = true, lastUpdatedEpochMs = 1_000L)

        assertTrue(WearPresentation.isStale(snapshot, nowEpochMs = 1_000L + 300_001L, thresholdMs = 300_000L))
    }
}
