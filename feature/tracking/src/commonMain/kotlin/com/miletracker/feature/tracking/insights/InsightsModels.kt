package com.miletracker.feature.tracking.insights

// ---------------------------------------------------------------------------
// Result data classes returned by the pure-Kotlin analyzers.
// No Android imports, safe for JVM unit tests.
// ---------------------------------------------------------------------------

/**
 * Overall journey quality result from [JourneyQualityAnalyzer].
 */
data class QualityResult(
    /** 0–100 composite quality score. */
    val qualityScore: Int,
    /** 0.0–1.0 fraction of expected GPS points that were actually captured. */
    val dataCompleteness: Double,
    /** 0–100 reliability score driven by distance-bucket divergence and system events. */
    val reliabilityScore: Int,
    /** Individual factors that contributed to the score (label → deduction). */
    val scoreFactors: List<ScoreFactor>,
)

data class ScoreFactor(val label: String, val deduction: Int)

// ---------------------------------------------------------------------------

/** Activity classification type, speed-based thresholds. */
enum class ActivityType {
    STATIONARY,
    WALKING,
    CYCLING,
    DRIVING,
    HIGHWAY,
    PAUSED,
    UNKNOWN,
}

/** Single activity→activity transition event. */
data class ActivityTransition(
    val fromActivity: ActivityType,
    val toActivity: ActivityType,
    val timestampMs: Long,
    val lat: Double,
    val lng: Double,
)

enum class AccelerationType { SMOOTH_ACCELERATION, HARSH_ACCELERATION, SMOOTH_BRAKING, HARSH_BRAKING, STEADY }

data class AccelerationEvent(
    val type: AccelerationType,
    val magnitudeMs2: Double,
    val timestampMs: Long,
    val lat: Double,
    val lng: Double,
)

data class AccelerationProfile(
    val smoothAccelerationPct: Double,
    val harshAccelerationPct: Double,
    val smoothBrakingPct: Double,
    val harshBrakingPct: Double,
    val steadySpeedPct: Double,
    val harshEvents: List<AccelerationEvent>,
) {
    /** Dominant label for display purposes. */
    val dominantLabel: String get() =
        when {
            harshAccelerationPct + harshBrakingPct > 20.0 -> "HARSH"
            steadySpeedPct > 60.0 -> "SMOOTH"
            else -> "MODERATE"
        }
}

/**
 * Full activity analysis result from [ActivityAnalyzer].
 */
data class ActivityResult(
    /** Activity type → percentage of total journey time (values sum ≈ 100). */
    val activityBreakdown: Map<ActivityType, Double>,
    val transitions: List<ActivityTransition>,
    val speedConsistency: Double,
    val accelerationProfile: AccelerationProfile,
    /** Dominant activity type (highest time share). */
    val dominantActivity: ActivityType,
)

// ---------------------------------------------------------------------------

/** A single system-level impact on journey quality. */
enum class SystemImpactType {
    BATTERY_OPTIMIZATION,
    POWER_SAVER,
    APP_KILLED,
    PHONE_RESTART,
    MOCK_LOCATION,
    POOR_GPS_ACCURACY,
    NETWORK_ISSUES,
}

data class SystemImpact(
    val type: SystemImpactType,
    /** Estimated percentage of the journey affected (0–100). */
    val estimatedImpactPct: Double,
    val durationMs: Long,
    val description: String,
)

data class BatteryImpact(
    val consumptionPct: Double,
    val consumptionRatePerHour: Double,
    val estimatedRemainingMins: Long,
    val recommendation: String?,
)

data class SystemImpactResult(
    val impacts: List<SystemImpact>,
    val batteryImpact: BatteryImpact?,
)

// ---------------------------------------------------------------------------

/**
 * Distance-quality result from [DistanceQualityAnalyzer].
 */
data class DistanceQualityResult(
    /** 0–100 composite quality score for the raw distance data. */
    val score: Int,
    val assessment: String,
    val cleanedDistanceRatio: Double,
    val isReliableForBusiness: Boolean,
    val mockPct: Double,
    val abnormalPct: Double,
)

// ---------------------------------------------------------------------------

/**
 * Aggregate output from [RouteAnalyzer], the full set of insights for one track.
 */
data class RouteAnalysisResult(
    val quality: QualityResult,
    val activity: ActivityResult,
    val systemImpact: SystemImpactResult,
    val distanceQuality: DistanceQualityResult,
    val summary: String,
    val category: String,
    val anomalies: List<String>,
)
