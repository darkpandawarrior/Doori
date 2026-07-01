package com.mileway.feature.tracking.insights

import com.mileway.core.data.model.db.LocationData
import kotlin.math.abs

/**
 * Pure-Kotlin activity-pattern analyzer.
 *
 * Speed thresholds (m/s):
 *   STATIONARY  < 1.0
 *   WALKING     < 5.0
 *   CYCLING     < 15.0
 *   DRIVING     < 50.0
 *   HIGHWAY    >= 50.0
 *
 * Acceleration thresholds (m/s²):
 *   smooth acceleration   >  1.0
 *   harsh  acceleration   >  2.5
 *   smooth braking        < -1.0
 *   harsh  braking        < -2.5
 *   steady speed          everything else
 *
 * Acceleration is computed via central difference:
 *   acc1 = (v[i] - v[i-1]) / dt1
 *   acc2 = (v[i+1] - v[i]) / dt2
 *   avgAcc = (acc1 + acc2) / 2
 */
class ActivityAnalyzer {
    companion object {
        private const val STATIONARY_THRESHOLD_MPS = 1.0f
        private const val WALKING_THRESHOLD_MPS = 5.0f
        private const val CYCLING_THRESHOLD_MPS = 15.0f
        private const val DRIVING_THRESHOLD_MPS = 50.0f

        private const val SMOOTH_ACCEL_THRESHOLD = 1.0
        private const val HARSH_ACCEL_THRESHOLD = 2.5
        private const val SMOOTH_BRAKE_THRESHOLD = -1.0
        private const val HARSH_BRAKE_THRESHOLD = -2.5
    }

    fun analyze(points: List<LocationData>): ActivityResult {
        if (points.isEmpty()) {
            return ActivityResult(
                activityBreakdown = emptyMap(),
                transitions = emptyList(),
                speedConsistency = 1.0,
                accelerationProfile = emptyProfile(),
                dominantActivity = ActivityType.UNKNOWN,
            )
        }

        val (breakdown, transitions) = analyzeActivityPatterns(points)
        val speedConsistency = calculateSpeedConsistency(points)
        val accelProfile = analyzeAccelerationBehavior(points)
        val dominant = breakdown.maxByOrNull { it.value }?.key ?: ActivityType.UNKNOWN

        return ActivityResult(
            activityBreakdown = breakdown,
            transitions = transitions,
            speedConsistency = speedConsistency,
            accelerationProfile = accelProfile,
            dominantActivity = dominant,
        )
    }

    private fun analyzeActivityPatterns(points: List<LocationData>): Pair<Map<ActivityType, Double>, List<ActivityTransition>> {
        val activityDurations = mutableMapOf<ActivityType, Long>()
        val transitions = mutableListOf<ActivityTransition>()
        var currentActivity = ActivityType.UNKNOWN
        var lastTimestamp = points.first().date

        points.forEach { point ->
            val activity = classifyActivity(point)

            if (currentActivity == ActivityType.UNKNOWN) {
                currentActivity = activity
            }

            val duration = point.date - lastTimestamp
            if (duration > 0) {
                activityDurations[currentActivity] =
                    (activityDurations[currentActivity] ?: 0L) + duration
            }

            if (activity != currentActivity) {
                transitions +=
                    ActivityTransition(
                        fromActivity = currentActivity,
                        toActivity = activity,
                        timestampMs = point.date,
                        lat = point.lat,
                        lng = point.lng,
                    )
                currentActivity = activity
            }

            lastTimestamp = point.date
        }

        val totalDuration = activityDurations.values.sum().coerceAtLeast(1L)
        val percentages =
            activityDurations.mapValues { (_, d) ->
                (d.toDouble() / totalDuration) * 100.0
            }

        return Pair(percentages, transitions)
    }

    private fun classifyActivity(point: LocationData): ActivityType =
        when {
            point.isPaused -> ActivityType.PAUSED
            point.speed < STATIONARY_THRESHOLD_MPS -> ActivityType.STATIONARY
            point.speed < WALKING_THRESHOLD_MPS -> ActivityType.WALKING
            point.speed < CYCLING_THRESHOLD_MPS -> ActivityType.CYCLING
            point.speed < DRIVING_THRESHOLD_MPS -> ActivityType.DRIVING
            else -> ActivityType.HIGHWAY
        }

    private fun calculateSpeedConsistency(points: List<LocationData>): Double {
        if (points.size < 3) return 1.0

        var totalVariation = 0.0
        var prevSpeed = points.first().speed

        points.drop(1).forEach { point ->
            totalVariation += abs(point.speed - prevSpeed)
            prevSpeed = point.speed
        }

        val maxSpeed = points.maxByOrNull { it.speed }?.speed?.toDouble() ?: 1.0
        val avgVariation = totalVariation / (points.size - 1)
        val normalizedVariation = avgVariation / maxSpeed.coerceAtLeast(1.0)
        return (1.0 - normalizedVariation).coerceIn(0.0, 1.0)
    }

    private fun analyzeAccelerationBehavior(points: List<LocationData>): AccelerationProfile {
        if (points.size < 3) return emptyProfile()

        var smoothAccelCount = 0
        var harshAccelCount = 0
        var smoothBrakeCount = 0
        var harshBrakeCount = 0
        var steadyCount = 0
        val harshEvents = mutableListOf<AccelerationEvent>()

        for (i in 1 until points.size - 1) {
            val prev = points[i - 1]
            val cur = points[i]
            val next = points[i + 1]

            val dt1 = (cur.date - prev.date) / 1000.0
            val dt2 = (next.date - cur.date) / 1000.0
            if (dt1 <= 0 || dt2 <= 0) continue

            val acc1 = (cur.speed - prev.speed) / dt1
            val acc2 = (next.speed - cur.speed) / dt2
            val avgAcc = (acc1 + acc2) / 2.0

            when {
                avgAcc > HARSH_ACCEL_THRESHOLD -> {
                    harshAccelCount++
                    harshEvents +=
                        AccelerationEvent(
                            type = AccelerationType.HARSH_ACCELERATION,
                            magnitudeMs2 = avgAcc,
                            timestampMs = cur.date,
                            lat = cur.lat,
                            lng = cur.lng,
                        )
                }
                avgAcc > SMOOTH_ACCEL_THRESHOLD -> smoothAccelCount++
                avgAcc < HARSH_BRAKE_THRESHOLD -> {
                    harshBrakeCount++
                    harshEvents +=
                        AccelerationEvent(
                            type = AccelerationType.HARSH_BRAKING,
                            magnitudeMs2 = avgAcc,
                            timestampMs = cur.date,
                            lat = cur.lat,
                            lng = cur.lng,
                        )
                }
                avgAcc < SMOOTH_BRAKE_THRESHOLD -> smoothBrakeCount++
                else -> steadyCount++
            }
        }

        val total =
            (
                smoothAccelCount + harshAccelCount + smoothBrakeCount +
                    harshBrakeCount + steadyCount
            ).toDouble()
        return if (total > 0) {
            AccelerationProfile(
                smoothAccelerationPct = (smoothAccelCount / total) * 100.0,
                harshAccelerationPct = (harshAccelCount / total) * 100.0,
                smoothBrakingPct = (smoothBrakeCount / total) * 100.0,
                harshBrakingPct = (harshBrakeCount / total) * 100.0,
                steadySpeedPct = (steadyCount / total) * 100.0,
                harshEvents = harshEvents,
            )
        } else {
            emptyProfile()
        }
    }

    private fun emptyProfile() =
        AccelerationProfile(
            smoothAccelerationPct = 0.0,
            harshAccelerationPct = 0.0,
            smoothBrakingPct = 0.0,
            harshBrakingPct = 0.0,
            steadySpeedPct = 100.0,
            harshEvents = emptyList(),
        )
}
