package com.mileway.feature.tracking.service

import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.siddharth.kmp.offlineoutbox.DraftStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Journey telemetry: [HardwareEventSyncer] — outbox-enqueue, permanent-vs-retryable handling and
 * the isDraining re-entrancy guard, mirroring [LocationDataSyncerTest] for the hardware-event path.
 */
class HardwareEventSyncerTest {
    private fun event(id: Long) =
        HardwareEvent(id = id, token = "t", eventType = EventType.TRACKING_STARTED, event = "Tracking Started", audience = EventAudience.USER)

    @Test
    fun `a drain with no unsynced events is a no-op`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao(emptyList())
            val outbox = FakeHardwareEventBatchOutbox()
            val syncer = HardwareEventSyncer(dao, outbox)

            syncer.drain("t")

            assertTrue(outbox.enqueued.isEmpty())
        }

    @Test
    fun `a successful drain enqueues one batch and marks the events synced`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao((1..3L).map { event(it) })
            val outbox = FakeHardwareEventBatchOutbox()
            val syncer = HardwareEventSyncer(dao, outbox)

            syncer.drain("t")

            assertEquals(listOf(1L, 2L, 3L), outbox.enqueued.single().eventIds)
            assertEquals(listOf(1L, 2L, 3L), dao.markedSynced)
            assertTrue(dao.unsynced.isEmpty())
        }

    @Test
    fun `a permanent failure drops the batch instead of retrying it`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao((1..3L).map { event(it) })
            val outbox = FakeHardwareEventBatchOutbox()
            val syncer = HardwareEventSyncer(dao, outbox, send = { SendOutcome.PERMANENT_FAILURE })

            syncer.drain("t")

            assertEquals(DraftStatus.FAILED, outbox.enqueued.single().let { outbox.statusFor(it) })
            assertTrue(dao.markedSynced.isNotEmpty(), "a permanently-failed batch is dropped for good, so it must still be marked synced")
        }

    @Test
    fun `a retryable failure leaves the events unsynced for the next drain`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao((1..2L).map { event(it) })
            val outbox = FakeHardwareEventBatchOutbox()
            val syncer = HardwareEventSyncer(dao, outbox, send = { SendOutcome.RETRYABLE_FAILURE })

            syncer.drain("t")

            assertTrue(dao.markedSynced.isEmpty())
            assertEquals(2, dao.unsynced.size)
        }

    @Test
    fun `an exception from send resets the in-flight flag instead of getting stuck`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao((1..2L).map { event(it) })
            val outbox = FakeHardwareEventBatchOutbox()
            var shouldThrow = true
            val syncer =
                HardwareEventSyncer(
                    dao,
                    outbox,
                    send = { if (shouldThrow) throw RuntimeException("simulated crash") else SendOutcome.SUCCESS },
                )

            runCatching { syncer.drain("t") }
            shouldThrow = false
            syncer.drain("t")

            assertTrue(dao.markedSynced.isNotEmpty(), "the second drain must actually run, not be blocked by a stuck in-flight flag")
        }

    @Test
    fun `drain for a different token does not touch this token's unsynced events`() =
        runTest {
            val dao = FakeUnsyncedHardwareEventDao(listOf(event(1)))
            val outbox = FakeHardwareEventBatchOutbox()
            val syncer = HardwareEventSyncer(dao, outbox)

            syncer.drain("other-token")

            assertTrue(outbox.enqueued.isEmpty())
            assertTrue(dao.markedSynced.isEmpty())
        }
}
