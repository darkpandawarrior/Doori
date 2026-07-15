package com.mileway.core.platform

import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState

/**
 * iOS actual: `UIDevice.batteryLevel`/`batteryState`. Battery monitoring is opt-in on iOS — without
 * enabling it, `batteryLevel` always reads -1.0 (unknown) — so this reader turns it on at
 * construction, mirroring [IosMotionSensorProvider]'s pattern of enabling the platform feed it reads.
 */
class IosBatteryStatusReader : BatteryStatusReader {
    private val device = UIDevice.currentDevice

    init {
        device.batteryMonitoringEnabled = true
    }

    override fun current(): BatteryStatus {
        val level = device.batteryLevel
        val percent = if (level < 0f) null else (level * 100).toInt()
        val charging =
            device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateCharging ||
                device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateFull

        return BatteryStatus(levelPercent = percent, isCharging = charging)
    }
}
