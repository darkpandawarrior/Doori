package com.mileway.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Design Language v2 colours that don't fit Material's role slots — the accent glow ramp, the
 * tuned-for-dark semantic states, and the raised-surface / border tokens that drive
 * elevation-by-lightness. Provided once by [MilewayTheme] and read 2 levels deep via
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
 *
 * Two groups of tokens live here:
 *  - **Provided** by the active curated theme (Matrix / Amoled / Ion / Daybreak) through
 *    [LocalMilewaySemanticColors]: the status states, the accent glow ramp and the surface /
 *    border tokens. Each curated spec hand-tunes these so they stay coherent per theme.
 *  - **Derived** from the live Material scheme — [neutral], [premium], [glow]. These were added
 *    by the per-screen sweep; rather than enumerate them in every theme spec, they resolve to a
 *    lightness-appropriate value off the current scheme so they stay WCAG-AA on both the deep-dark
 *    defaults and the light Daybreak surface. Always reach for colours via this accessor inside a
 *    composable — never hard-code a hex value in a screen.
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

    /**
     * True when the active scheme paints onto a light surface (Daybreak). Derived from relative
     * luminance so the muted / highlight tokens below pick an AA-correct lightness without each
     * curated theme having to carry them explicitly.
     */
    private val isLightSurface: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surface.relativeLuminance() >
            MaterialTheme.colorScheme.onSurface.relativeLuminance()

    /** Draft / inactive / muted metadata. Tuned for both deep-dark and Daybreak surfaces. */
    val neutral: Color
        @Composable @ReadOnlyComposable
        get() = if (isLightSurface) Color(0xFF5F6B66) else Color(0xFF9AA5A0)

    /** Premium / highlight (e.g. corporate cards, badges). AA on both default and light surfaces. */
    val premium: Color
        @Composable @ReadOnlyComposable
        get() = if (isLightSurface) Color(0xFF7B3FB0) else Color(0xFFC08AF2)

    /**
     * Subtle accent halo for the *currently active* primary affordance (recording, selected CTA) —
     * never decorative chrome. Returns the theme accent at a low alpha so callers can drop it
     * straight into a shadow / border / glow. Tracks the active theme's primary, so it follows the
     * curated scheme (Matrix green, Ion blue, etc.) rather than a fixed hardcoded green.
     */
    val glow: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = if (isLightSurface) 0.20f else 0.32f)
}

/** WCAG relative luminance (sRGB), multiplatform-safe (no android.graphics). */
private fun Color.relativeLuminance(): Float {
    fun lin(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * lin(red) + 0.7152f * lin(green) + 0.0722f * lin(blue)
}
