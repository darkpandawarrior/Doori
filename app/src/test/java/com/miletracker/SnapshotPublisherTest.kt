package com.miletracker

import com.miletracker.core.data.model.display.InMemorySnapshotPublisher
import com.miletracker.core.data.model.display.SurfaceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L.1: the in-process snapshot publisher starts empty and exposes the latest published [SurfaceSnapshot].
 */
class SnapshotPublisherTest {
    @Test
    fun `starts empty and reflects the latest published snapshot`() {
        val publisher = InMemorySnapshotPublisher()
        assertEquals(SurfaceSnapshot(), publisher.snapshot.value)

        val snap = SurfaceSnapshot(weekDistanceKm = 42.0, weekTrips = 6, actionRequiredCount = 2, qualityScore = 80)
        publisher.publish(snap)

        assertEquals(snap, publisher.snapshot.value)
        assertEquals(2, publisher.snapshot.value.actionRequiredCount)
    }
}
