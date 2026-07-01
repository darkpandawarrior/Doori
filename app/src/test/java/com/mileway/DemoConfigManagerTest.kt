package com.mileway

import com.mileway.core.platform.UpdateConfig
import com.mileway.core.platform.UpdateMode
import com.mileway.stub.DemoConfigManager
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that DemoConfigManager returns sensible defaults
 * and correctly implements ConfigProvider.
 */
class DemoConfigManagerTest {

    private lateinit var manager: DemoConfigManager

    @Before
    fun setUp() {
        manager = DemoConfigManager()
    }

    @Test
    fun `isMilesEnabled returns true`() = assertTrue(manager.isMilesEnabled())

    @Test
    fun `isLogMilesEnabled returns true`() = assertTrue(manager.isLogMilesEnabled())

    @Test
    fun `currency is INR`() = assertEquals("INR", manager.getCurrency())

    @Test
    fun `getTrackMilesConfig has sensible defaults`() {
        val config = manager.getTrackMilesConfig()
        assertTrue(config.isTrackMilesEnabled)
        assertTrue(config.trackMilesV2)
        assertTrue(config.draftTrackMiles)
        assertTrue(config.allowPauseTrackMiles)
        assertTrue(config.saveTrackMilesEnabled)
        assertTrue(config.isDiscardJourneyEnabled)
        assertEquals("DEMO", config.tenantCode)
        assertEquals("INR", config.currency)
    }

    @Test
    fun `getLogMilesConfig has sensible defaults`() {
        val config = manager.getLogMilesConfig()
        assertTrue(config.logMilesEnabled)
        assertTrue(config.isMilesEditable)
        assertTrue(config.draftLogMiles)
        assertEquals("INR", config.currency)
        assertEquals("DEMO", config.tenantCode)
    }

    @Test
    fun `configState emits Success immediately`() {
        val state = manager.configState.value
        assertNotNull(state)
        assertTrue(state is com.mileway.core.data.result.NetworkResult.Success)
        val data = (state as com.mileway.core.data.result.NetworkResult.Success).data
        assertTrue(data.miles)
        assertTrue(data.logMiles)
    }

    // ─── V15 PF.5 gating surface ───

    @Test
    fun `getUpdateConfig defaults are demo-sane and flexible`() {
        val config = manager.getUpdateConfig()
        assertTrue(config.enabled)
        assertEquals(UpdateMode.FLEXIBLE, config.mode)
        assertEquals(1L, config.minSupportedVersionCode)
        assertEquals(7, config.staleDays)
    }

    @Test
    fun `getFeatureFlags exposes the demo flags`() {
        val flags = manager.getFeatureFlags()
        assertTrue(flags.getValue("referralEnabled"))
        assertTrue(flags.getValue("inAppReviewEnabled"))
        assertTrue(flags.getValue("shareEnabled"))
    }

    @Test
    fun `kill switch is off by default`() = assertFalse(manager.isKillSwitchOn())

    @Test
    fun `constructor overrides drive the gating surface`() {
        val forced =
            DemoConfigManager(
                updateConfig = UpdateConfig(enabled = true, mode = UpdateMode.FORCED, minSupportedVersionCode = 99L),
                featureFlags = mapOf("referralEnabled" to false),
                killSwitch = true,
            )
        assertEquals(UpdateMode.FORCED, forced.getUpdateConfig().mode)
        assertEquals(99L, forced.getUpdateConfig().minSupportedVersionCode)
        assertFalse(forced.getFeatureFlags().getValue("referralEnabled"))
        assertTrue(forced.isKillSwitchOn())
    }
}
