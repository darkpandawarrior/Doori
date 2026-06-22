package com.miletracker

import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.display.TrackingState
import com.miletracker.core.data.model.display.TrackingSystemFlags
import com.miletracker.feature.tracking.service.TrackingNotificationMapper
import com.miletracker.feature.tracking.service.TrackingNotificationType
import com.miletracker.feature.tracking.service.TrackingSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C.2d: the pure snapshot → notification mapper produces the right one of the seven types, in priority
 * order (completion → blocking issues → paused → live), with the TRIP_COMPLETE deep link.
 */
class TrackingNotificationMapperTest {
    private fun map(s: TrackingSnapshot) = TrackingNotificationMapper.fromSnapshot(s)

    @Test
    fun `live tracking maps to ACTIVE, ongoing, no deep link`() {
        val c = map(TrackingSnapshot(state = TrackingState.LIVE_TRACKING))
        assertEquals(TrackingNotificationType.ACTIVE, c.type)
        assertTrue(c.ongoing)
        assertNull(c.deepLink)
    }

    @Test
    fun `paused maps to PAUSED`() {
        assertEquals(TrackingNotificationType.PAUSED, map(TrackingSnapshot(state = TrackingState.PAUSED)).type)
    }

    @Test
    fun `completed maps to TRIP_COMPLETE, dismissible, with the track deep link`() {
        val c = map(TrackingSnapshot(state = TrackingState.COMPLETED, distanceMeters = 4_200.0))
        assertEquals(TrackingNotificationType.TRIP_COMPLETE, c.type)
        assertFalse(c.ongoing)
        assertEquals(TrackingNotificationMapper.TRACK_DEEP_LINK, c.deepLink)
    }

    @Test
    fun `permission and policy and gps issues each map to their type`() {
        assertEquals(
            TrackingNotificationType.PERMISSION_MISSING,
            map(TrackingSnapshot(systemFlags = TrackingSystemFlags(permissionMissing = true))).type,
        )
        assertEquals(
            TrackingNotificationType.POLICY_VIOLATION,
            map(TrackingSnapshot(lastEvent = EventType.PERMISSION_VIOLATED)).type,
        )
        assertEquals(
            TrackingNotificationType.GPS_DISABLED,
            map(TrackingSnapshot(isGpsAvailable = false)).type,
        )
    }

    @Test
    fun `completion takes priority over an active system flag`() {
        val c = map(
            TrackingSnapshot(
                state = TrackingState.COMPLETED,
                systemFlags = TrackingSystemFlags(permissionMissing = true),
            ),
        )
        assertEquals(TrackingNotificationType.TRIP_COMPLETE, c.type)
    }

    @Test
    fun `autoDiscard is a dismissible AUTO_DISCARD notice`() {
        val c = TrackingNotificationMapper.autoDiscard()
        assertEquals(TrackingNotificationType.AUTO_DISCARD, c.type)
        assertFalse(c.ongoing)
    }
}
