package com.mileway.feature.whatsnew.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PLAN_V36 P7 (spec §8): [WhatsNewEngagementRecorder] enqueues the right type with an entry-id-bearing payload. */
class WhatsNewEngagementRecorderTest {
    @Test
    fun `record enqueues the given type with a payload carrying the entry id`() =
        runTest {
            val outbox = FakeOpOutbox()
            val recorder = WhatsNewEngagementRecorder(outbox)

            recorder.record(WhatsNewEngagementRecorder.TYPE_OPENED, "v36-whatsnew")

            val op = outbox.enqueued.single()
            assertEquals(WhatsNewEngagementRecorder.TYPE_OPENED, op.type)
            assertTrue("\"entryId\":\"v36-whatsnew\"" in op.payload)
        }

    @Test
    fun `every engagement type is enqueued independently`() =
        runTest {
            val outbox = FakeOpOutbox()
            val recorder = WhatsNewEngagementRecorder(outbox)

            recorder.record(WhatsNewEngagementRecorder.TYPE_SHARED, "e1")
            recorder.record(WhatsNewEngagementRecorder.TYPE_CONTACT, "e1")
            recorder.record(WhatsNewEngagementRecorder.TYPE_BANNER_OPEN, "e2")

            assertEquals(
                listOf(WhatsNewEngagementRecorder.TYPE_SHARED, WhatsNewEngagementRecorder.TYPE_CONTACT, WhatsNewEngagementRecorder.TYPE_BANNER_OPEN),
                outbox.enqueued.map { it.type },
            )
        }
}
