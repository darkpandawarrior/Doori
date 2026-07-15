package com.mileway.feature.tracking.manager

import com.mileway.core.platform.BatteryStatus

/** Outcome of [PreflightChecks.evaluateStartPreflight]: whether a trip may start right now. */
sealed interface StartCheckResult {
    /** Start is refused; [reason] is shown to the user. */
    data class Blocked(val reason: String) : StartCheckResult

    /** Start proceeds, but [reason] is surfaced as an advisory. */
    data class Warn(val reason: String) : StartCheckResult

    /** No battery concern. */
    data object Ok : StartCheckResult
}

/**
 * Pure commonMain battery-preflight policy (PLAN_V33 C6) — no Android/Java imports, mirrors
 * [DeviceTierManager]'s split of "platform reads a value, this object judges it". Gates starting a
 * trip on device battery: very low battery blocks the start outright (a trip that dies mid-way is
 * worse than not starting), moderate battery just warns (more so if not charging, since it'll only
 * drain further).
 */
object PreflightChecks {
    private const val BLOCK_AT_OR_BELOW_PERCENT = 10
    private const val WARN_BELOW_PERCENT = 30
    private const val WARN_BELOW_PERCENT_WHILE_CHARGING = 50

    /**
     * Unknown level ([BatteryStatus.levelPercent] `null`, e.g. desktop or a failed device query)
     * always resolves to [StartCheckResult.Ok] — never block a trip on a reading that isn't there.
     */
    fun evaluateStartPreflight(status: BatteryStatus): StartCheckResult {
        val level = status.levelPercent ?: return StartCheckResult.Ok
        return when {
            level <= BLOCK_AT_OR_BELOW_PERCENT ->
                StartCheckResult.Blocked("Battery at $level% — charge your device before starting a trip.")

            status.isCharging && level < WARN_BELOW_PERCENT_WHILE_CHARGING ->
                StartCheckResult.Warn("Battery at $level% — tracking will continue while you're plugged in.")

            !status.isCharging && level < WARN_BELOW_PERCENT ->
                StartCheckResult.Warn("Battery at $level% — tracking may be interrupted if it runs out.")

            else -> StartCheckResult.Ok
        }
    }
}
