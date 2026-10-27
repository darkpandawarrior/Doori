package com.mileway.feature.tracking.manager

import com.mileway.core.data.model.state.LogMilesPluginConfig
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.data.settings.AbnormalDetectionOverrides
import com.mileway.core.network.config.ConfigProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object FakeConfigProvider : ConfigProvider {
    override fun getTrackMilesConfig(): TrackMilesPluginConfig = TrackMilesPluginConfig()

    override fun getLogMilesConfig(): LogMilesPluginConfig = LogMilesPluginConfig()

    override fun isMilesEnabled(): Boolean = true

    override fun isLogMilesEnabled(): Boolean = true

    override fun getCurrency(): String = "USD"
}

/**
 * Regression guard (the project guidelines: capture current behavior before refactoring) — DEFAULT must stay
 * byte-for-byte equal to the thresholds LocationProcessor used to hardcode as private consts.
 */
class AbnormalDetectionConfigTest {
    @Test
    fun `DEFAULT equals the documented pre-refactor thresholds`() {
        val d = AbnormalDetectionConfig.DEFAULT
        assertEquals(2.5, d.walkingMaxMps)
        assertEquals(7.0, d.cyclingMaxMps)
        assertEquals(2.0, d.walkingJitterM)
        assertEquals(3.0, d.cyclingJitterM)
        assertEquals(5.0, d.drivingJitterM)
        assertEquals(1.2, d.stationarySpeedMps)
        assertEquals(1.2, d.stationaryJitterM)
        assertEquals(5, d.speedHistorySize)
        assertEquals(1.5, d.movementHistoryMps)
        assertEquals(5_000.0, d.spikeHardGateM)
        assertEquals(30L, d.gapMinSec)
        assertEquals(300L, d.gap5mSec)
        assertEquals(3_600L, d.gap1hSec)
        assertEquals(21_600L, d.gap6hSec)
        assertEquals(150.0, d.gapTier5mMps)
        assertEquals(100.0, d.gapTier1hMps)
        assertEquals(60.0, d.gapTier6hMps)
        assertEquals(10_000.0, d.gapMaxDistanceM)
    }

    @Test
    fun `TrackingConfigManager with no overrides source emits DEFAULT`() =
        runTest {
            val manager = TrackingConfigManager(configProvider = FakeConfigProvider)
            assertEquals(AbnormalDetectionConfig.DEFAULT, manager.abnormalDetectionConfig.first())
        }

    @Test
    fun `hot-reload Flow reflects a pushed override and falls back to DEFAULT for unset fields`() =
        runTest {
            val overrides = MutableStateFlow(AbnormalDetectionOverrides())
            val manager =
                TrackingConfigManager(
                    configProvider = FakeConfigProvider,
                    abnormalDetectionOverrides = overrides,
                )
            assertEquals(AbnormalDetectionConfig.DEFAULT, manager.abnormalDetectionConfig.first())

            overrides.value = AbnormalDetectionOverrides(spikeHardGateM = 42.0)
            val updated = manager.abnormalDetectionConfig.first()
            assertEquals(42.0, updated.spikeHardGateM)
            // Every other field still falls back to DEFAULT.
            assertEquals(AbnormalDetectionConfig.DEFAULT.gapTier5mMps, updated.gapTier5mMps)
        }
}
