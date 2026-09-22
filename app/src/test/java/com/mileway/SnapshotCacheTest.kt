package com.mileway

import com.mileway.core.data.watch.WatchSyncPayload
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * P6.1: a separate reader must see the payload a writer persisted — the exact contract a widget
 * (cold process, no live [com.mileway.core.data.model.display.SnapshotPublisher] subscriber) needs
 * from [com.mileway.core.data.watch.SnapshotCache]. [FakeSnapshotCache] stands in for the
 * DataStore/`NSUserDefaults`-backed actual the same way [FakeActiveAccountSource] does for
 * `ActiveAccountStore` (see that class's doc comment) — the real cross-process round trip is a
 * platform concern, not a JVM-testable one.
 */
class SnapshotCacheTest {
    @Test
    fun `nothing cached yet returns null`() =
        runTest {
            val cache = FakeSnapshotCache()
            assertNull(cache.read())
        }

    @Test
    fun `a separate reader sees the payload a writer persisted`() =
        runTest {
            val cache = FakeSnapshotCache()
            val payload =
                WatchSyncPayload(
                    todayKm = 8.0,
                    weekKm = 18.0,
                    tripCount = 3,
                    isTracking = false,
                    isPaused = false,
                    weekGoalProgress = 0.18f,
                    lastTripLabel = "Evening commute",
                    updatedAtMs = 1_700_000_000_000L,
                )

            cache.write(payload)

            // A second handle to the same store stands in for a widget's own cold-process read.
            val reader: com.mileway.core.data.watch.SnapshotCache = cache
            assertEquals(payload, reader.read())
        }

    @Test
    fun `a later write overwrites the previously cached payload`() =
        runTest {
            val cache = FakeSnapshotCache(seed = WatchSyncPayload(todayKm = 1.0))
            val updated = WatchSyncPayload(todayKm = 9.5, tripCount = 1)

            cache.write(updated)

            assertEquals(updated, cache.read())
        }
}
