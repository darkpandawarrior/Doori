package com.mileway.core.platform

// ponytail: a JVM dashboard target has no battery to read — always unknown, which
// PreflightChecks.evaluateStartPreflight treats as Ok (never block on a reading that isn't there).
class DesktopBatteryStatusReader : BatteryStatusReader {
    override fun current(): BatteryStatus = BatteryStatus(levelPercent = null, isCharging = false)
}
