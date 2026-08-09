package com.mileway.widget

import com.mileway.core.data.model.display.TrackingState
import com.mileway.core.data.watch.WatchSyncPayload
import com.mileway.feature.tracking.service.TrackingNotificationMapper
import com.mileway.feature.tracking.service.TrackingSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AMBIENT.1: JVM-unit coverage for [buildWidgetUiModel] — no Glance/Koin/Robolectric host needed,
 * mirrors `WearPresentationTest`'s plain-pure-mapper style. [WidgetScreenshotTest] covers the
 * render side of [WidgetUiModel] separately.
 */
class WidgetUiModelTest {

    private val now = 1_000_000L

    @Test
    fun `no active drive maps to a null status label and is not tracking`() {
        val model = buildWidgetUiModel(WatchSyncPayload(), liveSnapshot = null, nowEpochMs = now)

        assertNull(model.statusLabel)
        assertFalse(model.isTracking)
        assertFalse(model.isStale)
    }

    @Test
    fun `live tracking snapshot drives the status label from the shared notification mapper`() {
        val snapshot = TrackingSnapshot(state = TrackingState.LIVE_TRACKING, distanceMeters = 1_000.0)
        val expected = TrackingNotificationMapper.fromSnapshot(snapshot)

        val model = buildWidgetUiModel(WatchSyncPayload(), liveSnapshot = snapshot, nowEpochMs = now)

        assertEquals(expected.title, model.statusLabel)
        assertTrue(model.isTracking)
        assertFalse(model.isStale)
    }

    @Test
    fun `live paused snapshot drives the paused status label from the shared mapper`() {
        val snapshot = TrackingSnapshot(state = TrackingState.PAUSED, distanceMeters = 1_000.0)
        val expected = TrackingNotificationMapper.fromSnapshot(snapshot)

        val model = buildWidgetUiModel(WatchSyncPayload(), liveSnapshot = snapshot, nowEpochMs = now)

        assertEquals(expected.title, model.statusLabel)
        assertTrue(model.isTracking)
    }

    @Test
    fun `a live GPS-disabled snapshot surfaces that state, not a generic tracking label`() {
        val snapshot =
            TrackingSnapshot(
                state = TrackingState.LIVE_TRACKING,
                isGpsAvailable = false,
            )

        val model = buildWidgetUiModel(WatchSyncPayload(), liveSnapshot = snapshot, nowEpochMs = now)

        assertEquals("GPS unavailable", model.statusLabel)
    }

    @Test
    fun `a live READY snapshot is idle, not fed to the mapper`() {
        val snapshot = TrackingSnapshot(state = TrackingState.READY)

        val model = buildWidgetUiModel(WatchSyncPayload(), liveSnapshot = snapshot, nowEpochMs = now)

        assertNull(model.statusLabel)
        assertFalse(model.isTracking)
    }

    @Test
    fun `without a live snapshot, a tracking payload falls back to mapper-worded copy`() {
        val payload = WatchSyncPayload(isTracking = true, updatedAtMs = now)

        val model = buildWidgetUiModel(payload, liveSnapshot = null, nowEpochMs = now)

        assertEquals("Tracking active", model.statusLabel)
        assertTrue(model.isTracking)
    }

    @Test
    fun `without a live snapshot, a paused payload falls back to mapper-worded paused copy`() {
        val payload = WatchSyncPayload(isTracking = true, isPaused = true, updatedAtMs = now)

        val model = buildWidgetUiModel(payload, liveSnapshot = null, nowEpochMs = now)

        assertEquals("Tracking paused", model.statusLabel)
    }

    @Test
    fun `a fresh tracking payload without a live snapshot is not stale`() {
        val payload = WatchSyncPayload(isTracking = true, updatedAtMs = now - 60_000L)

        val model = buildWidgetUiModel(payload, liveSnapshot = null, nowEpochMs = now)

        assertFalse(model.isStale)
    }

    @Test
    fun `a stale tracking payload without a live snapshot is flagged stale`() {
        val payload = WatchSyncPayload(isTracking = true, updatedAtMs = now - 400_000L)

        val model = buildWidgetUiModel(payload, liveSnapshot = null, nowEpochMs = now)

        assertTrue(model.isStale)
    }

    @Test
    fun `an idle payload is never stale no matter how old`() {
        val payload = WatchSyncPayload(isTracking = false, updatedAtMs = 0L)

        val model = buildWidgetUiModel(payload, liveSnapshot = null, nowEpochMs = now)

        assertFalse(model.isStale)
    }

    @Test
    fun `a live snapshot overrides an old cached payload and is never treated as stale`() {
        val payload = WatchSyncPayload(isTracking = true, updatedAtMs = now - 400_000L)
        val snapshot = TrackingSnapshot(state = TrackingState.LIVE_TRACKING)

        val model = buildWidgetUiModel(payload, liveSnapshot = snapshot, nowEpochMs = now)

        assertFalse(model.isStale)
    }
}
