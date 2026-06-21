package com.miletracker

import com.miletracker.core.data.model.db.CurrentTrackData
import com.miletracker.feature.tracking.service.BootRestoreAction
import com.miletracker.feature.tracking.service.BootRestorePolicy
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Decision matrix for resuming an interrupted tracking session after reboot/app update.
 * Pure Kotlin — no device needed.
 */
class BootRestorePolicyTest {

    private fun session(
        token: String = "trip-123",
        isTracking: Boolean = true,
        startTime: Long = 1_700_000_000_000L
    ) = CurrentTrackData(token = token, isTracking = isTracking, startTime = startTime)

    @Test
    fun `live session with both permissions resumes the service`() {
        assertEquals(
            BootRestoreAction.RESUME_SERVICE,
            BootRestorePolicy.decide(session(), hasFineLocation = true, hasBackgroundLocation = true)
        )
    }

    @Test
    fun `missing fine location clears the stale session`() {
        assertEquals(
            BootRestoreAction.CLEAR_STALE_SESSION,
            BootRestorePolicy.decide(session(), hasFineLocation = false, hasBackgroundLocation = true)
        )
    }

    @Test
    fun `missing background location clears the stale session`() {
        // A location FGS started from a boot receiver is a background start: without
        // ACCESS_BACKGROUND_LOCATION it can't track, so the session must not auto-resume.
        assertEquals(
            BootRestoreAction.CLEAR_STALE_SESSION,
            BootRestorePolicy.decide(session(), hasFineLocation = true, hasBackgroundLocation = false)
        )
    }

    @Test
    fun `session not tracking does nothing`() {
        assertEquals(
            BootRestoreAction.NONE,
            BootRestorePolicy.decide(session(isTracking = false), hasFineLocation = true, hasBackgroundLocation = true)
        )
    }

    @Test
    fun `tracking flag with empty token is corrupt and gets cleared`() {
        assertEquals(
            BootRestoreAction.CLEAR_STALE_SESSION,
            BootRestorePolicy.decide(session(token = ""), hasFineLocation = true, hasBackgroundLocation = true)
        )
    }

    @Test
    fun `tracking flag with no start time is corrupt and gets cleared`() {
        assertEquals(
            BootRestoreAction.CLEAR_STALE_SESSION,
            BootRestorePolicy.decide(session(startTime = 0L), hasFineLocation = true, hasBackgroundLocation = true)
        )
    }

    @Test
    fun `empty session does nothing regardless of permissions`() {
        assertEquals(
            BootRestoreAction.NONE,
            BootRestorePolicy.decide(CurrentTrackData.empty(), hasFineLocation = false, hasBackgroundLocation = false)
        )
    }
}
