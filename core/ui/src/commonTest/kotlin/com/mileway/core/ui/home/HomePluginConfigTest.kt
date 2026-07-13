package com.mileway.core.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomePluginConfigTest {
    @Test
    fun `defaults are all-true, so a fresh install matches today's rendering`() {
        val config = HomePluginConfig()
        assertTrue(config.showTrackMiles)
        assertTrue(config.showMyCards)
        assertTrue(config.showCheckIn)
        assertTrue(config.showMarketingStrip)
        // V29 P29.H.2: off by default — "Ask Advance" stays on the classic form until flipped.
        assertEquals(false, config.useQrForAdvance)
    }

    @Test
    fun `useQrForAdvance toggles independently of the section flags`() {
        val controller = HomePluginConfigController(prefs = null)

        controller.update { it.copy(useQrForAdvance = true) }

        assertEquals(HomePluginConfig(useQrForAdvance = true), controller.config.value)
    }

    @Test
    fun `controller with no DataStore starts at defaults`() {
        val controller = HomePluginConfigController(prefs = null)
        assertEquals(HomePluginConfig(), controller.config.value)
    }

    @Test
    fun `update transforms in-memory state without a DataStore`() {
        val controller = HomePluginConfigController(prefs = null)

        controller.update { it.copy(showTrackMiles = false) }
        assertEquals(HomePluginConfig(showTrackMiles = false), controller.config.value)

        controller.update { it.copy(showMyCards = false, showCheckIn = false) }
        assertEquals(
            HomePluginConfig(showTrackMiles = false, showMyCards = false, showCheckIn = false),
            controller.config.value,
        )
    }
}
