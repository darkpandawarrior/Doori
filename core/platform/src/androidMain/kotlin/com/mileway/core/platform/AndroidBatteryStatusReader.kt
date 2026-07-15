package com.mileway.core.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Android actual: the sticky `ACTION_BATTERY_CHANGED` broadcast, the same context-only battery
 * read [com.mileway.feature.tracking.service.LocationTrackingService] already uses for its live
 * diagnostics snapshot — `registerReceiver(null, filter)` returns the last sticky intent without
 * registering a real receiver.
 */
class AndroidBatteryStatusReader(private val context: Context) : BatteryStatusReader {
    override fun current(): BatteryStatus {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else null

        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryStatus(levelPercent = percent, isCharging = charging)
    }
}
