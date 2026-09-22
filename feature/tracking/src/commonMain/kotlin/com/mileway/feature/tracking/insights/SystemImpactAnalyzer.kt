package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.util.MillisPerHour
import kotlin.math.min

// The impact rules stated in the KDoc below, as constants.
private const val MaxImpactPct = 100.0
private const val PctScale = 100.0

private const val AppKilledImpactPct = 25.0
private const val PhoneRestartImpactPct = 20.0

/** Used when a mock location was flagged but no mock distance was attributed to it. */
private const val MockLocationFallbackImpactPct = 15.0

/** A fix wider than this many metres is "poor"; poor fixes are only surfaced past a share of them. */
private const val PoorAccuracyMetres = 50f
private const val PoorAccuracyReportThresholdPct = 10.0
private const val PoorAccuracyMaxImpactPct = 30.0

/** Offline capture costs half its share, capped, and is only surfaced past a share of points. */
private const val OfflineReportThresholdPct = 5.0
private const val OfflineImpactDivisor = 2
private const val OfflineMaxImpactPct = 15.0

/** Drain above this many percent per hour is worth a recommendation. */
private const val HighDrainPctPerHour = 15.0
private const val LowBatteryRemainingMins = 60
private const val MinutesPerHour = 60

/** Battery impact needs a first and a last reading to subtract. */
private const val MinPointsForBatteryReading = 2

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
            val impactPct = min(MaxImpactPct, safeRatio(batteryOptTime.toDouble(), track.duration.toDouble()) * PctScale)
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
            val impactPct = min(MaxImpactPct, safeRatio(powerSaverTime.toDouble(), track.duration.toDouble()) * PctScale)
            impacts +=
                SystemImpact(
                    type = SystemImpactType.POWER_SAVER,
                    estimatedImpactPct = impactPct,
                    durationMs = powerSaverTime,
                    description = "Power saver mode limited location updates and accuracy",
                )
        }

        // App killed (fixed 25 %). The track row is authoritative; the event log is the fallback for
        // the case the kill happened before the flag could be written — which is what the KDoc has
        // always promised and what `events` was passed in for.
        if (track.wasAppKilled || events.any { it.eventType == EventType.APP_KILLED }) {
            impacts +=
                SystemImpact(
                    type = SystemImpactType.APP_KILLED,
                    estimatedImpactPct = AppKilledImpactPct,
                    durationMs = 0L,
                    description = "App was terminated during tracking, causing potential data loss",
                )
        }

        // Phone shutdown / restart (fixed 20 %), same track-row-then-event-log precedence.
        if (track.wasPhoneShutDown || events.any { it.eventType == EventType.PHONE_RESTART }) {
            impacts +=
                SystemImpact(
                    type = SystemImpactType.PHONE_RESTART,
                    estimatedImpactPct = PhoneRestartImpactPct,
                    durationMs = 0L,
                    description = "Device was restarted during tracking, interrupting data collection",
                )
        }

        // Mock locations
        if (track.wasMockLocationUsed) {
            val mockImpactPct =
                if (track.mockDistance > 0 && track.distance > 0) {
                    min(MaxImpactPct, (track.mockDistance / track.distance) * PctScale)
                } else {
                    MockLocationFallbackImpactPct
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
            val poorAccCount = points.count { it.accuracy > PoorAccuracyMetres }
            val poorAccPct = (poorAccCount.toDouble() / points.size) * PctScale
            if (poorAccPct > PoorAccuracyReportThresholdPct) {
                impacts +=
                    SystemImpact(
                        type = SystemImpactType.POOR_GPS_ACCURACY,
                        estimatedImpactPct = min(PoorAccuracyMaxImpactPct, poorAccPct),
                        durationMs = 0L,
                        description = "Poor GPS accuracy affected ${poorAccPct.toInt()}% of location points",
                    )
            }

            // Network issues
            val offlineCount = points.count { it.wasCapturedWhenNoNetwork }
            val offlinePct = (offlineCount.toDouble() / points.size) * PctScale
            if (offlinePct > OfflineReportThresholdPct) {
                impacts +=
                    SystemImpact(
                        type = SystemImpactType.NETWORK_ISSUES,
                        estimatedImpactPct = min(OfflineMaxImpactPct, offlinePct / OfflineImpactDivisor),
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
        if (points.size < MinPointsForBatteryReading) return null
        val startBattery = points.first().batteryPercentage
        val endBattery = points.last().batteryPercentage
        val consumption = startBattery - endBattery
        if (consumption <= 0) return null

        val durationHours = track.duration / MillisPerHour.toDouble()
        val ratePerHour = if (durationHours > 0) consumption / durationHours else 0.0
        val remainingMins = if (ratePerHour > 0) ((endBattery / ratePerHour) * MinutesPerHour).toLong() else 0L

        val recommendation =
            when {
                ratePerHour > HighDrainPctPerHour -> "Battery drain is high. Consider disabling unnecessary features."
                track.wasBatteryOptimizationEnabled -> "Disable battery optimisation for more accurate tracking."
                remainingMins < LowBatteryRemainingMins -> "Battery level is low for extended tracking. Consider charging."
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
