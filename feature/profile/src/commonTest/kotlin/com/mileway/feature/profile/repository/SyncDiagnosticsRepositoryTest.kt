package com.mileway.feature.profile.repository

import com.mileway.feature.profile.model.SyncConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private class FixedClock(private val epochMs: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMs)
}

/**
 * PLAN_V22 P6.7: [SyncDiagnosticsRepository] backs Settings' `SyncDiagnosticsCard` — an in-memory
 * counter (no network call; Mileway has no backend to sync against yet) that [forceSync] bumps to
 * simulate a real local upload job.
 */
class SyncDiagnosticsRepositoryTest {
    @Test
    fun `initial metrics have never synced and are not mid-sync`() {
        val repository = SyncDiagnosticsRepository()

        val initial = repository.metrics.value

        assertEquals(null, initial.lastSyncTimeMs)
        assertTrue(!initial.isSyncing)
    }

    @Test
    fun `forceSync bumps the synced counters and records a real last-sync time`() =
        runTest {
            val fixedNowMs = 1_700_000_000_000L
            val repository = SyncDiagnosticsRepository(clock = FixedClock(fixedNowMs))
            val before = repository.metrics.value

            repository.forceSync()

            val after = repository.metrics.value
            assertTrue(after.locationsSynced > before.locationsSynced)
            assertTrue(after.eventsSynced > before.eventsSynced)
            assertEquals(fixedNowMs, after.lastSyncTimeMs)
            assertTrue(!after.isSyncing)
        }

    @Test
    fun `forceSync drains exactly the enabled staging buckets`() =
        runTest {
            val repository = SyncDiagnosticsRepository(clock = FixedClock(1_700_000_000_000L))
            val before = repository.metrics.value

            // Only locations enabled — events + debug pending must stay staged, not synced.
            repository.forceSync(SyncConfig(locationEnabled = true, eventsEnabled = false, debugEventsEnabled = false))

            val after = repository.metrics.value
            assertEquals(before.locationsSynced + before.pendingLocations, after.locationsSynced)
            assertEquals(before.eventsSynced, after.eventsSynced)
        }

    @Test
    fun `forceSync with everything disabled is a no-op`() =
        runTest {
            val repository = SyncDiagnosticsRepository(clock = FixedClock(1_700_000_000_000L))
            val before = repository.metrics.value

            repository.forceSync(SyncConfig(locationEnabled = false, eventsEnabled = false, debugEventsEnabled = false))

            assertEquals(before, repository.metrics.value)
        }

    @Test
    fun `forceSync sets the next auto-sync due time from the interval`() =
        runTest {
            val nowMs = 1_700_000_000_000L
            val repository = SyncDiagnosticsRepository(clock = FixedClock(nowMs))

            repository.forceSync(SyncConfig(intervalMinutes = 10))

            assertEquals(nowMs + 10 * 60_000L, repository.metrics.value.nextSyncDueMs)
        }
}
