package com.mileway.core.data.model.display

enum class TrackingState { LIVE_TRACKING, PAUSED, COMPLETED, READY }

/**
 * C.2b: transient system-health flags the foreground tracking service surfaces live (alongside the
 * quality score) so the UI quality chip and the notification can react without a DB round-trip.
 */
data class TrackingSystemFlags(
    val gpsDisabled: Boolean = false,
    val permissionMissing: Boolean = false,
    val batteryOptimized: Boolean = false,
    val powerSaverOn: Boolean = false,
    val mockLocationDetected: Boolean = false,
) {
    /** True when any health issue is active — drives the warning chip / elevated notification. */
    val hasIssue: Boolean
        get() = gpsDisabled || permissionMissing || batteryOptimized || powerSaverOn || mockLocationDetected
}
