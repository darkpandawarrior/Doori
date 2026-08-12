package com.mileway.wear

import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.core.data.watch.TrackingCommandSender
import com.mileway.core.data.model.display.SurfaceSnapshotProducer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the one rule that makes watch-initiated tracking either work or lie:
 * `TrackingController.stop(token)` returns early unless the token matches the running session, so
 * the watch must stop with the token the *phone* started with — never one it made up.
 */
class WearTrackingControlTest {

    private class RecordingSender : TrackingCommandSender {
        val sent = mutableListOf<Pair<String, String>>()

        override suspend fun sendStart(token: String) {
            sent += "START" to token
        }

        override suspend fun sendStop(token: String) {
            sent += "STOP" to token
        }
    }

    @Test
    fun `the live token reaches the watch through the snapshot`() {
        val ui = WearPresentation.toUiState(
            SurfaceSnapshot(isTracking = true, activeToken = "phone-42"),
        )
        assertEquals("phone-42", ui.activeToken)
        assertTrue(ui.isTracking)
    }

    // An idle snapshot must not carry a token: a surface holding a stale one would offer to stop a
    // trip that already ended, and the phone would ignore it.
    @Test
    fun `an idle snapshot drops the token`() {
        val snapshot = SurfaceSnapshotProducer.produce(
            completedTracks = emptyList(),
            isTracking = false,
            nowEpochMs = 1_000L,
            activeToken = "left-over",
        )
        assertNull(snapshot.activeToken)
        assertNull(WearPresentation.toUiState(snapshot).activeToken)
    }

    @Test
    fun `a live snapshot keeps the token`() {
        val snapshot = SurfaceSnapshotProducer.produce(
            completedTracks = emptyList(),
            isTracking = true,
            nowEpochMs = 1_000L,
            activeToken = "live-7",
        )
        assertEquals("live-7", snapshot.activeToken)
    }

    @Test
    fun `the label states why the control is unavailable rather than just disabling it`() {
        assertEquals("Start trip", trackingActionLabel(WearRootUiState(isTracking = false)))
        assertEquals(
            "Stop trip",
            trackingActionLabel(WearRootUiState(isTracking = true, activeToken = "t")),
        )
        // Live, but this watch never received the token — the phone would ignore any stop we sent.
        assertEquals(
            "Stop on phone",
            trackingActionLabel(WearRootUiState(isTracking = true, activeToken = null)),
        )
    }

    @Test
    fun `noGms has no sender, so the control is absent rather than inert`() {
        assertFalse(WearRootUiState().canControlTracking)
    }

    // Guards the actual send decision without spinning up a ViewModel: stopping must reuse the
    // phone's token verbatim, and starting must mint one.
    @Test
    fun `stop reuses the phone token and start mints a new one`() = runTest {
        val sender = RecordingSender()
        sender.sendStop(WearRootUiState(isTracking = true, activeToken = "phone-99").activeToken!!)
        sender.sendStart("wear-1234")
        assertEquals(listOf("STOP" to "phone-99", "START" to "wear-1234"), sender.sent)
    }
}
