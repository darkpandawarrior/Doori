package com.miletracker.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Design Language v2 — semantic colour tokens.
 *
 * One source of truth for every "what does this colour *mean*" decision across the app:
 * status states (success / warning / danger / info), the neutral/premium accents, and the
 * subtle green *glow* reserved for active/primary affordances.
 *
 * These replace the scattered `Color(0xFF…)` literals, [DesignTokens.StatusColors] and the
 * per-feature `TrackingTheme` palette. They are **theme-aware**: each token resolves to a
 * lightness-appropriate value for the current Material scheme so it stays WCAG-AA on both the
 * deep-dark default surfaces and the light Daybreak surface. Always reach for these via the
 * [MilewayColors] accessor inside a composable — never hard-code a hex value in a screen again.
 *
 * Geometry, type and edge-to-edge behaviour are theme-independent and live elsewhere
 * (DesignTokens / Type).
 */
object MilewayColors {
    // ── Dark-surface tuned tokens (deep-dark default — AA on #0B0F0D) ───────────────
    private val DarkSuccess = Color(0xFF3DDC84) // accent green, doubles as "success"
    private val DarkWarning = Color(0xFFF2C14E)
    private val DarkDanger = Color(0xFFF2545B)
    private val DarkInfo = Color(0xFF5BA8F5)
    private val DarkNeutral = Color(0xFF9AA5A0)
    private val DarkPremium = Color(0xFFC08AF2)

    // ── Light-surface tuned tokens (Daybreak — AA on a near-white surface) ──────────
    private val LightSuccess = Color(0xFF1C8F52)
    private val LightWarning = Color(0xFFB8860B)
    private val LightDanger = Color(0xFFD32F2F)
    private val LightInfo = Color(0xFF1C6FD6)
    private val LightNeutral = Color(0xFF5F6B66)
    private val LightPremium = Color(0xFF7B3FB0)

    /** Brand accent — the single green the app leans on for primary affordances. */
    val accent: Color = Color(0xFF3DDC84)

    private val isLight: Boolean
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surface.relativeLuminance() >
            MaterialTheme.colorScheme.onSurface.relativeLuminance()

    /** Positive / approved / completed / reimbursable. */
    val success: Color
        @Composable @ReadOnlyComposable get() = if (isLight) LightSuccess else DarkSuccess

    /** Pending / low-balance / caution. */
    val warning: Color
        @Composable @ReadOnlyComposable get() = if (isLight) LightWarning else DarkWarning

    /** Rejected / critical / overdue / destructive. */
    val danger: Color
        @Composable @ReadOnlyComposable get() = if (isLight) LightDanger else DarkDanger

    /** Informational / processing / submitted. */
    val info: Color
        @Composable @ReadOnlyComposable get() = if (isLight) LightInfo else DarkInfo

    /** Draft / inactive / muted metadata. */
    val neutral: Color
        @Composable @ReadOnlyComposable get() = if (isLight) LightNeutral else DarkNeutral

    /** Premium / highlight (e.g. corporate cards, badges). */
    val premium: Color
        @Composable @ReadOnlyComposable get() = if (isLight) LightPremium else DarkPremium

    /**
     * Subtle green halo for active/primary surfaces. Reserved — only the *currently active*
     * control (recording, selected primary CTA) should glow, never decorative chrome.
     * Returns the accent at a low alpha so callers can drop it straight into a shadow/border.
     */
    val glow: Color
        @Composable @ReadOnlyComposable get() = accent.copy(alpha = if (isLight) 0.20f else 0.32f)
}

/** WCAG relative luminance (sRGB), multiplatform-safe (no android.graphics). */
private fun Color.relativeLuminance(): Float {
    fun lin(c: Float): Float =
        if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * lin(red) + 0.7152f * lin(green) + 0.0722f * lin(blue)
}
