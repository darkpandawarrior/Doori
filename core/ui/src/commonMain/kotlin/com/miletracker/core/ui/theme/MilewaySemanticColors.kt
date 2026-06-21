package com.miletracker.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design Language v2 colours that don't fit Material's role slots — the accent glow ramp, the
 * tuned-for-dark semantic states, and the raised-surface / border tokens that drive
 * elevation-by-lightness. Provided once by [MileTrackerTheme] and read 2 levels deep via
 * [MilewayColors], so screens never hard-code hexes and every curated theme stays coherent.
 *
 * Architecture: this is the seam future under-themes hook into. A driving-mode or per-feature
 * accent tint can override just these tokens via a nested provider with no change to call sites.
 */
data class MilewaySemanticColors(
    val warning: Color,
    val danger: Color,
    val info: Color,
    val success: Color,
    val accentGlow: Color,
    val accentDim: Color,
    val border: Color,
    val surfaceRaised: Color,
    val surfaceHighest: Color,
    /** Dark schemes paint a subtle glow + top-edge highlight on raised surfaces; light schemes don't. */
    val useGlow: Boolean,
)

/** Fallback mirrors the Matrix scheme so previews that forget to wrap still look on-brand. */
val LocalMilewaySemanticColors: ProvidableCompositionLocal<MilewaySemanticColors> =
    staticCompositionLocalOf { MatrixSpec.semanticColors() }

/**
 * Accessor for [MilewaySemanticColors]. Mirrors the `MaterialTheme.colorScheme` ergonomics:
 *
 * ```
 * Text(color = MilewayColors.warning)
 * Surface(color = MilewayColors.surfaceRaised) { … }
 * ```
 */
object MilewayColors {
    val warning: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.warning
    val danger: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.danger
    val info: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.info
    val success: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.success
    val accentGlow: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.accentGlow
    val accentDim: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.accentDim
    val border: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.border
    val surfaceRaised: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.surfaceRaised
    val surfaceHighest: Color
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.surfaceHighest
    val useGlow: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalMilewaySemanticColors.current.useGlow

    /** The primary accent — convenience mirror of `MaterialTheme.colorScheme.primary`. */
    val accent: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary
}
