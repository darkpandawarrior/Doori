package com.mileway.feature.tracking.ui.components

import androidx.compose.ui.graphics.Color
import com.mileway.core.ui.theme.DesignTokens

// Severity tints come from the static v2 fallbacks (kept in lock-step with MilewayColors);
// this is an enum constructor, so it can't read the composable theme accessor.
enum class IssueSeverity(val displayName: String, val color: Color) {
    Low("Low", DesignTokens.StatusColors.success),
    Warning("Warning", DesignTokens.StatusColors.warning),
    High("High", DesignTokens.StatusColors.error),
    Critical("Critical", DesignTokens.StatusColors.error),
}

data class SystemIssue(
    val message: String,
    val impact: String,
    val severity: IssueSeverity,
)
