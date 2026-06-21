package com.miletracker

import com.miletracker.core.ui.platform.isUnderMaintenance
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** CF.5 — kill-switch / min-version maintenance predicate. */
class MaintenanceGateTest {
    @Test
    fun `kill switch forces maintenance`() {
        assertTrue(isUnderMaintenance(killSwitchOn = true, currentVersionCode = 10, minSupportedVersionCode = 1))
    }

    @Test
    fun `below minimum supported version forces maintenance`() {
        assertTrue(isUnderMaintenance(killSwitchOn = false, currentVersionCode = 1, minSupportedVersionCode = 5))
    }

    @Test
    fun `at or above minimum with switch off is normal`() {
        assertFalse(isUnderMaintenance(killSwitchOn = false, currentVersionCode = 5, minSupportedVersionCode = 5))
        assertFalse(isUnderMaintenance(killSwitchOn = false, currentVersionCode = 9, minSupportedVersionCode = 5))
    }
}
