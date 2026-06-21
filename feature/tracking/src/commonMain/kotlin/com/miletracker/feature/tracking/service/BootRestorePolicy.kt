package com.miletracker.feature.tracking.service

import com.miletracker.core.data.model.db.CurrentTrackData

/** What the boot receiver should do with the persisted session it found. */
enum class BootRestoreAction {
    /** Session is live and we can track, restart the foreground service. */
    RESUME_SERVICE,

    /** Session claims to be tracking but can't be resumed, mark it stopped. */
    CLEAR_STALE_SESSION,

    /** No tracking session, nothing to do. */
    NONE,
}

/**
 * Decides whether an interrupted tracking session can be auto-resumed after a reboot or
 * app update. Pure Kotlin so the decision matrix is unit-testable without Android.
 *
 * Both location permissions are required: fine location for the fixes themselves, and
 * background location because a location foreground service started from the background
 * (a BOOT_COMPLETED receiver is a background context) is subject to Android 11+
 * while-in-use restrictions, without ACCESS_BACKGROUND_LOCATION the start is either
 * disallowed or the service receives no fixes.
 */
object BootRestorePolicy {
    fun decide(
        session: CurrentTrackData,
        hasFineLocation: Boolean,
        hasBackgroundLocation: Boolean,
    ): BootRestoreAction {
        val resumable = session.isTracking && session.token.isNotEmpty() && session.startTime > 0L
        if (!resumable) {
            // isTracking with no token/startTime is a corrupt leftover, clean it up.
            return if (session.isTracking) {
                BootRestoreAction.CLEAR_STALE_SESSION
            } else {
                BootRestoreAction.NONE
            }
        }
        if (!hasFineLocation || !hasBackgroundLocation) return BootRestoreAction.CLEAR_STALE_SESSION
        return BootRestoreAction.RESUME_SERVICE
    }
}
