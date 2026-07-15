package com.mileway.feature.tracking.manager

import com.mileway.core.platform.BatteryStatus
import kotlin.test.Test
import kotlin.test.assertIs

class PreflightChecksTest {
    private fun status(
        level: Int?,
        charging: Boolean = false,
    ) = BatteryStatus(levelPercent = level, isCharging = charging)

    @Test
    fun `5 percent blocks`() {
        assertIs<StartCheckResult.Blocked>(PreflightChecks.evaluateStartPreflight(status(5)))
    }

    @Test
    fun `10 percent blocks (boundary)`() {
        assertIs<StartCheckResult.Blocked>(PreflightChecks.evaluateStartPreflight(status(10)))
    }

    @Test
    fun `25 percent not charging warns`() {
        assertIs<StartCheckResult.Warn>(PreflightChecks.evaluateStartPreflight(status(25)))
    }

    @Test
    fun `45 percent charging warns`() {
        assertIs<StartCheckResult.Warn>(PreflightChecks.evaluateStartPreflight(status(45, charging = true)))
    }

    @Test
    fun `45 percent not charging is ok`() {
        assertIs<StartCheckResult.Ok>(PreflightChecks.evaluateStartPreflight(status(45, charging = false)))
    }

    @Test
    fun `80 percent is ok`() {
        assertIs<StartCheckResult.Ok>(PreflightChecks.evaluateStartPreflight(status(80)))
    }

    @Test
    fun `unknown level never blocks`() {
        assertIs<StartCheckResult.Ok>(PreflightChecks.evaluateStartPreflight(status(null)))
    }

    @Test
    fun `11 percent (just above block threshold) warns not blocks`() {
        assertIs<StartCheckResult.Warn>(PreflightChecks.evaluateStartPreflight(status(11)))
    }

    @Test
    fun `50 percent charging is ok (boundary, warn floor while charging is below 50)`() {
        assertIs<StartCheckResult.Ok>(PreflightChecks.evaluateStartPreflight(status(50, charging = true)))
    }
}
