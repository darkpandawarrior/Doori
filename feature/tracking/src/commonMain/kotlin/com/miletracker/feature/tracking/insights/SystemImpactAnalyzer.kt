package com.miletracker.feature.tracking.insights

import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.model.db.SavedTrack
import kotlin.math.min

/**
 * Pure-Kotlin system-impact analyzer.
 *
 * Impact rules:
 *   - Battery optimisation     → impactPct = min(100, batteryOptTime / duration * 100)
 *   - Power saver              → impactPct = min(100, powerSaverTime / duration * 100)
 *   - App killed               → fixed 25 %
 *   - Phone restart            → fixed 20 %
 *   - Mock location            → mockDistance / distance * 100 (fallback 15 %)
 *   - Poor GPS accuracy (>50m) → capped at 30 %; only surfaced when >10 % of points
 *   - No-network points        → half of offline%, capped at 15 %; surfaced when >5 %
 *
 * HardwareEvents are used purely to detect app-kill / restart events not captured on
 * the track row itself (defensive augmentation, track row flags take precedence).
 */
class SystemImpactAnalyzer {
    fun analyze(
        track: SavedTrack,
        points: List<LocationData>,
        events: List<HardwareEvent> = emptyList(),
    ): SystemImpactResult {
        val impacts = mutableListOf<SystemImpact>()

        // Battery optimisation
        if (track.wasBatteryOptimizationEnabled) {
            val batteryOptTime = track.totalBatteryOptimizationOnTime.coerceAtLeast(0L)
            val impactPct = min(100.0, safeRatio(batteryOptTime.toDouble(), track.duration.toDouble()) * 100.0)
            impacts +=
                SystemImpact(
                    type = SystemImpactType.BATTERY_OPTIMIZATION,
                    estimatedImpactPct = impactPct,
                    durationMs = batteryOptTime,
                    description = "Battery optimisation reduced location accuracy and frequency",
                )
        }

        // Power saver
        if (track.wasPowerSaverEnabled) {
            val powerSaverTime = track.totalPowerSaverOnTime.coerceAtLeast(0L)
            val impactPct = min(100.0, safeRatio(powerSaverTime.toDouble(), track.duration.toDouble()) * 100.0)
            impacts +=
                SystemImpact(
                    type = SystemImpactType.POWER_SAVER,
                    estimatedImpactPct = impactPct,
                    durationMs = powerSaverTime,
                    description = "Power saver mode limited location updates and accuracy",
                )
        }

        // App killed (fixed 25 %)
        if (track.wasAppKilled) {
            impacts +=
                SystemImpact(
                    type = SystemImpactType.APP_KILLED,
                    estimatedImpactPct = 25.0,
                    durationMs = 0L,
                    description = "App was terminated during tracking, causing potential data loss",
                )
        }

        // Phone shutdown / restart (fixed 20 %)
        if (track.wasPhoneShutDown) {
            impacts +=
                SystemImpact(
                    type = SystemImpactType.PHONE_RESTART,
                    estimatedImpactPct = 20.0,
                    durationMs = 0L,
                    description = "Device was restarted during tracking, interrupting data collection",
                )
        }

        // Mock locations
        if (track.wasMockLocationUsed) {
            val mockImpactPct =
                if (track.mockDistance > 0 && track.distance > 0) {
                    min(100.0, (track.mockDistance / track.distance) * 100.0)
                } else {
                    15.0
                }
            impacts +=
                SystemImpact(
                    type = SystemImpactType.MOCK_LOCATION,
                    estimatedImpactPct = mockImpactPct,
                    durationMs = 0L,
                    description = "Mock locations detected, affecting data reliability",
                )
        }

        // GPS accuracy issues (>50 m accuracy = poor)
        if (points.isNotEmpty()) {
            val poorAccCount = points.count { it.accuracy > 50f }
            val poorAccPct = (poorAccCount.toDouble() / points.size) * 100.0
            if (poorAccPct > 10.0) {
                impacts +=
                    SystemImpact(
                        type = SystemImpactType.POOR_GPS_ACCURACY,
                        estimatedImpactPct = min(30.0, poorAccPct),
                        durationMs = 0L,
                        description = "Poor GPS accuracy affected ${poorAccPct.toInt()}% of location points",
                    )
            }

            // Network issues
            val offlineCount = points.count { it.wasCapturedWhenNoNetwork }
            val offlinePct = (offlineCount.toDouble() / points.size) * 100.0
            if (offlinePct > 5.0) {
                impacts +=
                    SystemImpact(
                        type = SystemImpactType.NETWORK_ISSUES,
                        estimatedImpactPct = min(15.0, offlinePct / 2),
                        durationMs = 0L,
                        description = "Network connectivity issues during ${offlinePct.toInt()}% of the journey",
                    )
            }
        }

        val battery = analyzeBatteryImpact(track, points)
        return SystemImpactResult(impacts = impacts, batteryImpact = battery)
    }

    private fun analyzeBatteryImpact(
        track: SavedTrack,
        points: List<LocationData>,
    ): BatteryImpact? {
        if (points.size < 2) return null
        val startBattery = points.first().batteryPercentage
        val endBattery = points.last().batteryPercentage
        val consumption = startBattery - endBattery
        if (consumption <= 0) return null

        val durationHours = track.duration / (1000.0 * 60.0 * 60.0)
        val ratePerHour = if (durationHours > 0) consumption / durationHours else 0.0
        val remainingMins = if (ratePerHour > 0) ((endBattery / ratePerHour) * 60).toLong() else 0L

        val recommendation =
            when {
                ratePerHour > 15.0 -> "Battery drain is high. Consider disabling unnecessary features."
                track.wasBatteryOptimizationEnabled -> "Disable battery optimisation for more accurate tracking."
                remainingMins < 60 -> "Battery level is low for extended tracking. Consider charging."
                else -> null
            }

        return BatteryImpact(
            consumptionPct = consumption,
            consumptionRatePerHour = ratePerHour,
            estimatedRemainingMins = remainingMins,
            recommendation = recommendation,
        )
    }

    private fun safeRatio(
        numerator: Double,
        denominator: Double,
    ): Double = if (denominator > 0) numerator / denominator else 0.0
}
