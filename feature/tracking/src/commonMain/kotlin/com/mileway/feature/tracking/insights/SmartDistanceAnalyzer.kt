package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.LocationData

/**
 * Pure-Kotlin engine for "Smart Distance" reconciliation (parity §2.4).
 *
 * Takes a track's existing distance buckets (already computed by [DistanceQualityAnalyzer] /
 * the tracking pipeline — this engine never recomputes distance math) plus the raw points, and
 * proposes a menu of per-category reduction options the user can selectively apply. Nothing here
 * mutates state: [analyze] only suggests, [applyReductions] only sums what the caller selected.
 *
 * reduction amounts are the existing bucket values (mock/abnormal/spike already measure
 * exactly these problems); walking/pause-drift are estimated from point-level signals since no
 * dedicated bucket exists for them. Confidence is a simple point-coverage ratio, not a model.
 */
object SmartDistanceAnalyzer {
    /** Auto-trigger threshold: |GPS − odometer| / odometer. */
    const val AUTO_TRIGGER_DISCREPANCY_RATIO = 0.20

    /** Walking speed ceiling (m/s) — below this, movement is presumed foot-traffic, not driving. */
    private const val WALKING_SPEED_MS = 2.0

    /** Below this speed the point is considered near-stationary (candidate for GPS drift). */
    private const val STATIONARY_SPEED_MS = 0.5

    fun analyze(
        buckets: DistanceBucketBreakdown,
        points: List<LocationData>,
        odometerDistance: Double,
    ): SmartDistanceAnalysis {
        val totalDistance = buckets.originalDistance.takeIf { it > 0 } ?: buckets.cleanedDistance
        val totalCount = points.size

        val options =
            listOf(
                walkingOption(points, totalDistance, totalCount),
                stationaryDriftOption(points, totalDistance, totalCount),
                speedOutlierOption(buckets, points, totalCount),
                batteryOptOption(buckets, points, totalCount),
                pauseDriftOption(points, totalDistance, totalCount),
            )

        val direction = discrepancyDirection(totalDistance, odometerDistance)
        val ratio = discrepancyRatio(totalDistance, odometerDistance)

        return SmartDistanceAnalysis(
            options = options,
            discrepancyDirection = direction,
            discrepancyRatio = ratio,
            autoTrigger = (ratio ?: 0.0) > AUTO_TRIGGER_DISCREPANCY_RATIO,
        )
    }

    /** GPS vs odometer, whichever is larger. Equal (within float noise) reports [DiscrepancyDirection.EQUAL]. */
    fun discrepancyDirection(
        gpsDistance: Double,
        odometerDistance: Double,
    ): DiscrepancyDirection =
        when {
            gpsDistance > odometerDistance -> DiscrepancyDirection.GPS_HIGHER
            odometerDistance > gpsDistance -> DiscrepancyDirection.ODOMETER_HIGHER
            else -> DiscrepancyDirection.EQUAL
        }

    /** `|gps - odometer| / odometer`, or `null` when odometer distance is unknown/zero (can't ratio against it). */
    fun discrepancyRatio(
        gpsDistance: Double,
        odometerDistance: Double,
    ): Double? = if (odometerDistance > 0) kotlin.math.abs(gpsDistance - odometerDistance) / odometerDistance else null

    /** Sum of the user's selected reduction options, applied to [base] and floored at zero. */
    fun applyReductions(
        base: Double,
        selected: List<ReductionOption>,
    ): Double = (base - selected.sumOf { it.suggestedReductionKm }).coerceAtLeast(0.0)

    /** Builds an audit-log entry for each option the user actually applied. */
    fun buildAuditLog(selected: List<ReductionOption>): List<AppliedReduction> =
        selected.map { AppliedReduction(category = it.category, reductionKm = it.suggestedReductionKm) }

    // -- per-category options -------------------------------------------------

    private fun walkingOption(
        points: List<LocationData>,
        totalDistance: Double,
        totalCount: Int,
    ): ReductionOption {
        val walkingCount = points.count { it.speed in 0f..WALKING_SPEED_MS.toFloat() && !it.isPaused }
        val pct = safePct(walkingCount, totalCount)
        return ReductionOption(
            category = ReductionCategory.WALKING,
            suggestedReductionKm = totalDistance * pct,
            confidence = confidenceFor(walkingCount, totalCount),
        )
    }

    private fun stationaryDriftOption(
        points: List<LocationData>,
        totalDistance: Double,
        totalCount: Int,
    ): ReductionOption {
        val driftCount = points.count { it.speed < STATIONARY_SPEED_MS.toFloat() && !it.isPaused }
        val pct = safePct(driftCount, totalCount)
        return ReductionOption(
            category = ReductionCategory.STATIONARY_DRIFT,
            suggestedReductionKm = totalDistance * pct,
            confidence = confidenceFor(driftCount, totalCount),
        )
    }

    /** Speed-outlier reduction reuses the abnormal-distance bucket directly — no re-derivation. */
    private fun speedOutlierOption(
        buckets: DistanceBucketBreakdown,
        points: List<LocationData>,
        totalCount: Int,
    ): ReductionOption {
        val abnormalCount = points.count { it.isAbnormal }
        return ReductionOption(
            category = ReductionCategory.SPEED_OUTLIER,
            suggestedReductionKm = buckets.abnormalDistance,
            confidence = confidenceFor(abnormalCount, totalCount),
        )
    }

    /** Battery-opt reduction reuses the spike-distance bucket directly when any point was affected. */
    private fun batteryOptOption(
        buckets: DistanceBucketBreakdown,
        points: List<LocationData>,
        totalCount: Int,
    ): ReductionOption {
        val affectedCount = points.count { it.wasBatteryOptimizationEnabled || it.wasPowerSaverModeEnabled }
        return ReductionOption(
            category = ReductionCategory.BATTERY_OPT,
            suggestedReductionKm = if (affectedCount > 0) buckets.spikeDistance else 0.0,
            confidence = confidenceFor(affectedCount, totalCount),
        )
    }

    private fun pauseDriftOption(
        points: List<LocationData>,
        totalDistance: Double,
        totalCount: Int,
    ): ReductionOption {
        val pauseCount = points.count { it.isPaused || it.wasManuallyPaused }
        val pct = safePct(pauseCount, totalCount)
        return ReductionOption(
            category = ReductionCategory.PAUSE_DRIFT,
            suggestedReductionKm = totalDistance * pct,
            confidence = confidenceFor(pauseCount, totalCount),
        )
    }

    private fun safePct(
        count: Int,
        total: Int,
    ): Double = if (total > 0) count / total.toDouble() else 0.0

    /** confidence is just affected-point coverage — more supporting points, higher confidence. */
    private fun confidenceFor(
        count: Int,
        total: Int,
    ): Double = if (total > 0) (count / total.toDouble()).coerceIn(0.0, 1.0) else 0.0
}
