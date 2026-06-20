package com.miletracker

import com.miletracker.core.data.model.db.EventType
import com.miletracker.core.data.model.display.TrackingState
import com.miletracker.feature.tracking.service.TrackingStatePublisher
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * C.2b: the in-process publisher is the channel between the foreground service (writer) and the
 * ViewModel (reader, C.3). Covers the transitions the service drives: start → live, pause, resume,
 * stop → completed, and reset. Lives in app/src/test (the repo's JVM-test home for feature:tracking
 * commonMain logic, alongside DynamicIntervalCalculatorTest) so it runs under testNoGmsDebugUnitTest.
 */
class TrackingStatePublisherTest {

    @Test
    fun `starts in READY with an empty snapshot`() {
        val publisher = TrackingStatePublisher()
        val s = publisher.trackingState.value
        assertEquals(TrackingState.READY, s.state)
        assertEquals(0.0, s.distanceMeters)
        assertNull(s.lastEvent)
    }

    @Test
    fun `update transitions state and carries telemetry`() {
        val publisher = TrackingStatePublisher()
        publisher.update {
            it.copy(state = TrackingState.LIVE_TRACKING, token = "t1", distanceMeters = 1_250.0, totalPoints = 9)
        }
        val s = publisher.trackingState.value
        assertEquals(TrackingState.LIVE_TRACKING, s.state)
        assertEquals("t1", s.token)
        assertEquals(1_250.0, s.distanceMeters)
        assertEquals(9, s.totalPoints)
    }

    @Test
    fun `pause then resume flips state and records the event`() {
        val publisher = TrackingStatePublisher()
        publisher.update { it.copy(state = TrackingState.PAUSED, lastEvent = EventType.TRACKING_PAUSED) }
        assertEquals(TrackingState.PAUSED, publisher.trackingState.value.state)
        assertEquals(EventType.TRACKING_PAUSED, publisher.trackingState.value.lastEvent)

        publisher.update { it.copy(state = TrackingState.LIVE_TRACKING, lastEvent = EventType.TRACKING_RESUMED) }
        assertEquals(TrackingState.LIVE_TRACKING, publisher.trackingState.value.state)
        assertEquals(EventType.TRACKING_RESUMED, publisher.trackingState.value.lastEvent)
    }

    @Test
    fun `stop marks COMPLETED and reset clears back to READY`() {
        val publisher = TrackingStatePublisher()
        publisher.update { it.copy(state = TrackingState.LIVE_TRACKING, distanceMeters = 5_000.0) }
        publisher.update { it.copy(state = TrackingState.COMPLETED, lastEvent = EventType.TRACKING_STOPPED) }
        assertEquals(TrackingState.COMPLETED, publisher.trackingState.value.state)

        publisher.reset()
        assertEquals(TrackingState.READY, publisher.trackingState.value.state)
        assertEquals(0.0, publisher.trackingState.value.distanceMeters)
    }
}
