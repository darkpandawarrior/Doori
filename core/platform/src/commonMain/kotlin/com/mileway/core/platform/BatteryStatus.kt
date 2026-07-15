package com.mileway.core.platform

/**
 * A single battery reading. [levelPercent] is `null` when the platform can't report a level
 * (desktop has no battery; a real device query can also fail) — callers must treat that as
 * "unknown", never as "empty" (see [com.mileway.feature.tracking.manager.PreflightChecks]).
 */
data class BatteryStatus(
    val levelPercent: Int?,
    val isCharging: Boolean,
)

/**
 * Reads the device's current battery level/charging state (PLAN_V33 C6 tracking-start preflight).
 * Context-backed on Android (`BatteryManager`/the sticky `ACTION_BATTERY_CHANGED` broadcast), so —
 * like [Haptics]/[MotionSensorProvider] — this is an interface bound per platform through
 * `platformModule()` rather than a bare `expect`/`actual` fun (a bare fun can't reach the Android
 * Context cleanly; see [Haptics]'s doc for the same rationale). [currentDeviceManufacturer] above
 * gets away with a bare fun only because `Build.MANUFACTURER` needs no Context.
 */
interface BatteryStatusReader {
    fun current(): BatteryStatus
}
