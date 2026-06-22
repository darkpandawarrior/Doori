package com.miletracker.feature.tracking.service

import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.display.TrackingState
import com.miletracker.core.data.model.display.TrackingSystemFlags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackingNotificationMapperTest {
    // ── TRIP_COMPLETE ─────────────────────────────────────────────────────────

    @Test
    fun `TRIP_COMPLETE branch when state is COMPLETED`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(state = TrackingState.COMPLETED, distanceMeters = 3500.0),
            )
        assertEquals(TrackingNotificationType.TRIP_COMPLETE, content.type)
        assertFalse(content.ongoing, "TRIP_COMPLETE must be dismissible")
        assertEquals(TrackingNotificationMapper.TRACK_DEEP_LINK, content.deepLink)
        assertTrue(content.actions.isEmpty(), "terminal notification has no action buttons")
    }

    // ── PERMISSION_MISSING ────────────────────────────────────────────────────

    @Test
    fun `PERMISSION_MISSING branch when permissionMissing flag is set`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(systemFlags = TrackingSystemFlags(permissionMissing = true)),
            )
        assertEquals(TrackingNotificationType.PERMISSION_MISSING, content.type)
        assertTrue(content.ongoing)
        assertEquals(listOf(TrackingNotificationAction.STOP), content.actions)
    }

    @Test
    fun `PERMISSION_MISSING branch when lastEvent is PERMISSION_DENIED`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(lastEvent = EventType.PERMISSION_DENIED),
            )
        assertEquals(TrackingNotificationType.PERMISSION_MISSING, content.type)
    }

    // ── POLICY_VIOLATION ──────────────────────────────────────────────────────

    @Test
    fun `POLICY_VIOLATION branch when lastEvent is PERMISSION_VIOLATED`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(lastEvent = EventType.PERMISSION_VIOLATED),
            )
        assertEquals(TrackingNotificationType.POLICY_VIOLATION, content.type)
        assertTrue(content.ongoing)
        assertEquals(listOf(TrackingNotificationAction.STOP), content.actions)
    }

    // ── GPS_DISABLED ──────────────────────────────────────────────────────────

    @Test
    fun `GPS_DISABLED branch when gpsDisabled flag is set`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(systemFlags = TrackingSystemFlags(gpsDisabled = true)),
            )
        assertEquals(TrackingNotificationType.GPS_DISABLED, content.type)
        assertTrue(content.ongoing)
        assertEquals(
            listOf(TrackingNotificationAction.FIX_GPS, TrackingNotificationAction.STOP),
            content.actions,
        )
    }

    @Test
    fun `GPS_DISABLED branch when isGpsAvailable is false`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(isGpsAvailable = false),
            )
        assertEquals(TrackingNotificationType.GPS_DISABLED, content.type)
    }

    @Test
    fun `GPS_DISABLED branch when lastEvent is GPS_LOST`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(lastEvent = EventType.GPS_LOST),
            )
        assertEquals(TrackingNotificationType.GPS_DISABLED, content.type)
    }

    // ── PAUSED ────────────────────────────────────────────────────────────────

    @Test
    fun `PAUSED branch when state is PAUSED`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(state = TrackingState.PAUSED),
            )
        assertEquals(TrackingNotificationType.PAUSED, content.type)
        assertTrue(content.ongoing)
        assertEquals(
            listOf(TrackingNotificationAction.RESUME, TrackingNotificationAction.STOP),
            content.actions,
        )
    }

    // ── ACTIVE ────────────────────────────────────────────────────────────────

    @Test
    fun `ACTIVE branch for a normal tracking snapshot`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(state = TrackingState.LIVE_TRACKING, distanceMeters = 1234.0, speedMps = 8.3),
            )
        assertEquals(TrackingNotificationType.ACTIVE, content.type)
        assertTrue(content.ongoing)
        assertEquals(
            listOf(TrackingNotificationAction.PAUSE, TrackingNotificationAction.STOP),
            content.actions,
        )
    }

    // ── Auto-discard standalone factory ──────────────────────────────────────

    @Test
    fun `autoDiscard returns AUTO_DISCARD dismissible content with deep link`() {
        val content = TrackingNotificationMapper.autoDiscard()
        assertEquals(TrackingNotificationType.AUTO_DISCARD, content.type)
        assertFalse(content.ongoing)
        assertEquals(TrackingNotificationMapper.TRACK_DEEP_LINK, content.deepLink)
    }

    // ── Priority ordering ─────────────────────────────────────────────────────

    @Test
    fun `COMPLETED takes priority over GPS_LOST`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(state = TrackingState.COMPLETED, lastEvent = EventType.GPS_LOST),
            )
        assertEquals(TrackingNotificationType.TRIP_COMPLETE, content.type)
    }

    @Test
    fun `PERMISSION_MISSING takes priority over PAUSED`() {
        val content =
            TrackingNotificationMapper.fromSnapshot(
                snapshot(
                    state = TrackingState.PAUSED,
                    systemFlags = TrackingSystemFlags(permissionMissing = true),
                ),
            )
        assertEquals(TrackingNotificationType.PERMISSION_MISSING, content.type)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun snapshot(
        state: TrackingState = TrackingState.LIVE_TRACKING,
        distanceMeters: Double = 0.0,
        speedMps: Double = 0.0,
        lastEvent: EventType? = null,
        isGpsAvailable: Boolean = true,
        systemFlags: TrackingSystemFlags = TrackingSystemFlags(),
    ) = TrackingSnapshot(
        state = state,
        distanceMeters = distanceMeters,
        speedMps = speedMps,
        lastEvent = lastEvent,
        isGpsAvailable = isGpsAvailable,
        systemFlags = systemFlags,
    )
}
