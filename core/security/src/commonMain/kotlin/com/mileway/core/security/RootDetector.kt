package com.mileway.core.security

/**
 * Cross-platform device-integrity check. [check] assembles a list of root/jailbreak signals; the
 * platform-specific detection is provided by [detectRootSignals] (Android root checks in androidMain,
 * iOS jailbreak heuristic in iosMain).
 */
object RootDetector {
    data class RootCheckResult(
        val isRooted: Boolean,
        val signals: List<String>,
    )

    fun check(): RootCheckResult {
        val signals = detectRootSignals()
        return RootCheckResult(isRooted = signals.isNotEmpty(), signals = signals)
    }
}

/** Platform device-compromise signals. Android: su binary / test-keys / Magisk. iOS: jailbreak paths. */
internal expect fun detectRootSignals(): List<String>
