package com.mileway.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MilewayTypography =
    Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 1.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.8.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.8.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.5.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.5.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 1.2.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.0.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.8.sp),
    )

/**
 * Design Language v2 — "mono for data".
 *
 * Numeric readouts (distance, speed, duration, amounts, codes/IDs) use a monospaced family so
 * digits are tabular: figures share a fixed advance width, columns line up and a live-updating
 * value (an odometer, a timer) doesn't jitter as digits change.
 *
 * Two ergonomics are offered, both backed by [FontFamily.Monospace] (no bundled font asset /
 * dependency — multiplatform-safe, same choice the colour-wheel hex field makes):
 *  - [MilewayMono] + [TextStyle.dataStyle] — the lightweight "derive a mono variant from any
 *    Material role" path used across the per-screen sweep:
 *    `Text(value, style = MaterialTheme.typography.titleMedium.dataStyle())`.
 *  - [MilewayType] — fixed-size presets ([dataLarge] / [dataMedium] / [dataSmall]) plus [mono]
 *    for hero counters and inline stats that want an explicit size rather than a Material role.
 */
val MilewayMono: TextStyle =
    TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    )

/**
 * Returns this style re-cast in the monospaced "data" family while keeping its size, line height
 * and weight. Use for any numeric value the user reads as data:
 * `Text(value, style = MaterialTheme.typography.titleMedium.dataStyle())`.
 */
fun TextStyle.dataStyle(): TextStyle =
    copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.sp)

/**
 * Fixed-size mono presets for hero counters and inline stats that want an explicit size rather
 * than deriving from a Material role via [dataStyle]. Same monospace family as [MilewayMono].
 */
object MilewayType {
    val MonoFamily: FontFamily = FontFamily.Monospace

    /** Hero counters (live distance / big amounts). Tight tracking, heavy weight. */
    val dataLarge =
        TextStyle(
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            letterSpacing = (-0.5).sp,
        )

    /** Section stats (speed, duration, secondary amounts). */
    val dataMedium =
        TextStyle(
            fontFamily = MonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            letterSpacing = 0.sp,
        )

    /** Inline data chips, codes, coordinates. */
    val dataSmall =
        TextStyle(
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 0.4.sp,
        )

    /** Re-cast an arbitrary [TextStyle] (e.g. a Material role) into the data-mono family. */
    fun mono(style: TextStyle): TextStyle = style.copy(fontFamily = MonoFamily)

    /** Convenience: the current theme's `headlineMedium` recast as mono, for one-off counters. */
    val heroMono: TextStyle
        @Composable @ReadOnlyComposable
        get() = mono(MaterialTheme.typography.headlineMedium)
}

/**
 * Terminal-specific text styles for the Matrix/terminal aesthetic.
 * Use these in AI chat, section headers, status lines, and anywhere you want a
 * phosphor-terminal feel distinct from the standard Material roles.
 */
object TerminalType {
    private val TerminalFamily: FontFamily = FontFamily.Monospace

    /** Command-line prompt — user input prefix `> `, short commands. */
    val prompt = TextStyle(
        fontFamily = TerminalFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 20.sp,
    )

    /** System output — AI replies, status messages. */
    val output = TextStyle(
        fontFamily = TerminalFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp,
        lineHeight = 20.sp,
    )

    /** Section header label — e.g. `// QUICK_ACTIONS`. Wide tracking, caps friendly. */
    val sectionLabel = TextStyle(
        fontFamily = TerminalFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.4.sp,
        lineHeight = 16.sp,
    )

    /** Status line — tiny one-liner status, coordinates, version strings. */
    val statusLine = TextStyle(
        fontFamily = TerminalFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 1.0.sp,
        lineHeight = 14.sp,
    )

    /** App/module header — large monospaced display type. */
    val display = TextStyle(
        fontFamily = TerminalFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.5.sp,
    )
}

/**
 * Returns this style as a terminal monospaced variant with wider letter spacing.
 * Use for any UI element that should feel like terminal output rather than regular UI.
 */
fun TextStyle.terminalStyle(): TextStyle =
    copy(
        fontFamily = FontFamily.Monospace,
        letterSpacing = (letterSpacing.value + 0.5f).sp,
    )
