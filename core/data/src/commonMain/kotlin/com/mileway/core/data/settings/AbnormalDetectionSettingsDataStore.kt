package com.mileway.core.data.settings

import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed source for the abnormal-detection thresholds a debug/customization screen can
 * override today (and a server config could override later — see AbnormalDetectionConfig).
 * `null` in [overrides] means "not set", so callers fall back to the default threshold.
 */
interface AbnormalDetectionSettingsSource {
    val overrides: Flow<AbnormalDetectionOverrides>
}

/**
 * Every field is nullable — `null` means "use the default", non-null means "override this one
 * threshold". Kept separate from `AbnormalDetectionConfig` (feature/tracking) since core/data
 * doesn't depend on the feature module; TrackingConfigManager maps this onto the full config.
 *
 * PLAN_V24 P10.3: extended from the original 5 gap/spike overrides to the full set of tunable
 * `LocationProcessor`/`AbnormalDetectionConfig` thresholds. The keys are backed by the Plugin
 * Registry (see [RegistryAbnormalDetectionSource]) so they persist per-account and surface on the
 * Master Plugin page — "one store, two surfaces". Every field an `AbnormalDetectionConfig` field
 * actually consumed by the live pipeline; reference keys with no Mileway consumer are skipped (PROGRESS).
 */
data class AbnormalDetectionOverrides(
    // Speed-classification bands (m/s).
    val walkingMaxMps: Double? = null,
    val cyclingMaxMps: Double? = null,
    val stationarySpeedMps: Double? = null,
    val movementHistoryMps: Double? = null,
    // Per-band min-displacement jitter gates (m).
    val walkingJitterM: Double? = null,
    val cyclingJitterM: Double? = null,
    val drivingJitterM: Double? = null,
    val stationaryJitterM: Double? = null,
    // Movement-history window (count).
    val speedHistorySize: Int? = null,
    // Spike / abnormal gates.
    val spikeHardGateM: Double? = null,
    val gapMaxDistanceM: Double? = null,
    // Gap-recovery tiers: window seconds + relaxed speed caps (m/s).
    val gapMinSec: Long? = null,
    val gap5mSec: Long? = null,
    val gap1hSec: Long? = null,
    val gap6hSec: Long? = null,
    val gapTier5mMps: Double? = null,
    val gapTier1hMps: Double? = null,
    val gapTier6hMps: Double? = null,
)

/**
 * PLAN_V24 P10.3: the abnormal-detection thresholds are VALUE plugins in the Plugin Registry, so
 * this source projects the registry's resolved values onto [AbnormalDetectionOverrides] for
 * `TrackingConfigManager`. Because every plugin's DEFAULT layer equals `AbnormalDetectionConfig`'s
 * baseline, an unset knob resolves to exactly the shipped threshold — live tracking math is
 * unchanged until a knob is moved on the Master Plugin page.
 *
 * commonMain + `PluginRegistry` only → KMP-clean (no android/java), shared by Android and iOS.
 */
class RegistryAbnormalDetectionSource(
    private val registry: PluginRegistry,
) : AbnormalDetectionSettingsSource {
    override val overrides: Flow<AbnormalDetectionOverrides> =
        registry.observeResolved()
            .map { resolved ->
                val byId = resolved.associateBy { it.descriptor.id }

                fun dbl(id: String): Double? = (byId[id]?.value as? PluginValue.DoubleVal)?.value

                fun int(id: String): Int? = (byId[id]?.value as? PluginValue.IntVal)?.value

                AbnormalDetectionOverrides(
                    walkingMaxMps = dbl(KEY_WALKING_MAX_MPS),
                    cyclingMaxMps = dbl(KEY_CYCLING_MAX_MPS),
                    stationarySpeedMps = dbl(KEY_STATIONARY_SPEED_MPS),
                    movementHistoryMps = dbl(KEY_MOVEMENT_HISTORY_MPS),
                    walkingJitterM = dbl(KEY_WALKING_JITTER_M),
                    cyclingJitterM = dbl(KEY_CYCLING_JITTER_M),
                    drivingJitterM = dbl(KEY_DRIVING_JITTER_M),
                    stationaryJitterM = dbl(KEY_STATIONARY_JITTER_M),
                    speedHistorySize = int(KEY_SPEED_HISTORY_SIZE),
                    spikeHardGateM = dbl(KEY_SPIKE_HARD_GATE_M),
                    gapMaxDistanceM = dbl(KEY_GAP_MAX_DISTANCE_M),
                    gapMinSec = int(KEY_GAP_MIN_S)?.toLong(),
                    gap5mSec = int(KEY_GAP_5M_S)?.toLong(),
                    gap1hSec = int(KEY_GAP_1H_S)?.toLong(),
                    gap6hSec = int(KEY_GAP_6H_S)?.toLong(),
                    gapTier5mMps = dbl(KEY_GAP_TIER_5M_MPS),
                    gapTier1hMps = dbl(KEY_GAP_TIER_1H_MPS),
                    gapTier6hMps = dbl(KEY_GAP_TIER_6H_MPS),
                )
            }
            .distinctUntilChanged()

    companion object {
        // Plugin ids — must match the descriptors registered in PluginCatalog.abnormalTuningPlugins.
        const val KEY_WALKING_MAX_MPS = "track_walking_max_mps"
        const val KEY_CYCLING_MAX_MPS = "track_cycling_max_mps"
        const val KEY_STATIONARY_SPEED_MPS = "track_stationary_speed_mps"
        const val KEY_MOVEMENT_HISTORY_MPS = "track_movement_history_mps"
        const val KEY_WALKING_JITTER_M = "track_walking_jitter_m"
        const val KEY_CYCLING_JITTER_M = "track_cycling_jitter_m"
        const val KEY_DRIVING_JITTER_M = "track_driving_jitter_m"
        const val KEY_STATIONARY_JITTER_M = "track_stationary_jitter_m"
        const val KEY_SPEED_HISTORY_SIZE = "track_speed_history_size"
        const val KEY_SPIKE_HARD_GATE_M = "track_spike_hard_gate_m"
        const val KEY_GAP_MAX_DISTANCE_M = "track_gap_max_distance_m"
        const val KEY_GAP_MIN_S = "track_gap_min_s"
        const val KEY_GAP_5M_S = "track_gap_5m_s"
        const val KEY_GAP_1H_S = "track_gap_1h_s"
        const val KEY_GAP_6H_S = "track_gap_6h_s"
        const val KEY_GAP_TIER_5M_MPS = "track_gap_tier_5m_mps"
        const val KEY_GAP_TIER_1H_MPS = "track_gap_tier_1h_mps"
        const val KEY_GAP_TIER_6H_MPS = "track_gap_tier_6h_mps"
    }
}
