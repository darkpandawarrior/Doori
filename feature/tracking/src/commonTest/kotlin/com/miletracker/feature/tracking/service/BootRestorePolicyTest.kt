package com.miletracker.feature.tracking.service

import com.miletracker.core.data.model.db.CurrentTrackData
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * P-F.3: covers the boot/reconciliation receiver seam. [LocationTrackingBootReceiver] is an
 * Android BroadcastReceiver (untestable on the JVM/commonTest), but its entire decision is
 * delegated to this pure matrix, so the seam is verified here. The library
 * (`dev.brewkits.kmpworkmanager`) reschedules the periodic workers via its own
 * `DefaultAlarmReceiver` (`RECEIVE_BOOT_COMPLETED`); this policy governs the *session* restore.
 */
class BootRestorePolicyTest {
    private fun active(token: String = "tok-123") =
        CurrentTrackData(
            token = token,
            isTracking = true,
            startTime = 1_000L,
        )

    @Test
    fun `active session with both permissions resumes the service`() {
        val action =
            BootRestorePolicy.decide(
                session = active(),
                hasFineLocation = true,
                hasBackgroundLocation = true,
            )
        assertEquals(BootRestoreAction.RESUME_SERVICE, action)
    }

    @Test
    fun `active session missing background location is not resumable`() {
        val action =
            BootRestorePolicy.decide(
                session = active(),
                hasFineLocation = true,
                hasBackgroundLocation = false,
            )
        assertEquals(BootRestoreAction.CLEAR_STALE_SESSION, action)
    }

    @Test
    fun `active session missing fine location is not resumable`() {
        val action =
            BootRestorePolicy.decide(
                session = active(),
                hasFineLocation = false,
                hasBackgroundLocation = true,
            )
        assertEquals(BootRestoreAction.CLEAR_STALE_SESSION, action)
    }

    @Test
    fun `tracking flag with empty token is a corrupt leftover and gets cleared`() {
        val action =
            BootRestorePolicy.decide(
                session = active(token = ""),
                hasFineLocation = true,
                hasBackgroundLocation = true,
            )
        assertEquals(BootRestoreAction.CLEAR_STALE_SESSION, action)
    }

    @Test
    fun `tracking flag with zero startTime is a corrupt leftover and gets cleared`() {
        val action =
            BootRestorePolicy.decide(
                session = CurrentTrackData(token = "tok", isTracking = true, startTime = 0L),
                hasFineLocation = true,
                hasBackgroundLocation = true,
            )
        assertEquals(BootRestoreAction.CLEAR_STALE_SESSION, action)
    }

    @Test
    fun `no active session does nothing even without permissions`() {
        val action =
            BootRestorePolicy.decide(
                session = CurrentTrackData.empty(),
                hasFineLocation = false,
                hasBackgroundLocation = false,
            )
        assertEquals(BootRestoreAction.NONE, action)
    }

    @Test
    fun `stopped session with a token does nothing`() {
        val action =
            BootRestorePolicy.decide(
                session = CurrentTrackData(token = "tok", isTracking = false, startTime = 5L),
                hasFineLocation = true,
                hasBackgroundLocation = true,
            )
        assertEquals(BootRestoreAction.NONE, action)
    }
}
