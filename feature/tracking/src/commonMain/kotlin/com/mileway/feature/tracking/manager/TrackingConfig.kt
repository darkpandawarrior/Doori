package com.mileway.feature.tracking.manager

import kotlinx.serialization.Serializable

/**
 * The typed server-driven plugin config for tracking — this IS the backend contract (§2.3/§3
 * Wave 4): every field a server plugin-config would carry, with a typed safe [DEFAULT]. Served
 * from [DEFAULT_TRACKING_CONFIG_JSON] (a local stand-in) until the backend phase; swapping the
 * source to a real network response later is a source-only change, the shape doesn't move.
 */
@Serializable
data class TrackingConfig(
    // GPS sampling interval bounds (milliseconds).
    val minGpsIntervalMs: Long = 2_000L,
    val maxGpsIntervalMs: Long = 15_000L,
    val defaultGpsIntervalMs: Long = 5_000L,
    // Accuracy thresholds (meters).
    val maxAccuracyThresholdM: Double = 50.0,
    val goodAccuracyThresholdM: Double = 20.0,
    // Feature flags.
    val kalmanFilterEnabled: Boolean = true,
    val imuFusionEnabled: Boolean = true,
    val gapFillEnabled: Boolean = true,
    // Submission/quality thresholds.
    val minSubmissionDistanceKm: Double = 0.5,
    val minSubmissionPoints: Int = 3,
    val minPointQualityScore: Double = 0.6,
    // Abnormal-detection knobs — nests the existing typed config rather than re-declaring it.
    val abnormalDetection: AbnormalDetectionConfig = AbnormalDetectionConfig.DEFAULT,
) {
    companion object {
        /** Safe defaults — used whenever the local-JSON source is missing/partial/unparseable. */
        val DEFAULT = TrackingConfig()
    }
}

/**
 * Bundled default config JSON — the local stand-in for a server plugin-config response (this
 * defines the wire shape). A commonMain string constant, not a resource file: no expect/actual
 * file IO needed for a handful of fields (file-based resource loading only if this
 * grows past a hand-editable constant).
 */
const val DEFAULT_TRACKING_CONFIG_JSON: String =
    """
    {
        "minGpsIntervalMs": 2000,
        "maxGpsIntervalMs": 15000,
        "defaultGpsIntervalMs": 5000,
        "maxAccuracyThresholdM": 50.0,
        "goodAccuracyThresholdM": 20.0,
        "kalmanFilterEnabled": true,
        "imuFusionEnabled": true,
        "gapFillEnabled": true,
        "minSubmissionDistanceKm": 0.5,
        "minSubmissionPoints": 3,
        "minPointQualityScore": 0.6,
        "abnormalDetection": {}
    }
    """
