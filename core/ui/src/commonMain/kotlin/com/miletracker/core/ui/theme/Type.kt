package com.miletracker.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MileTrackerTypography =
    Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
    )

/**
 * Design Language v2: the monospace family used for **all numeric / data readouts** —
 * distance, speed, duration, amounts, codes. [FontFamily.Monospace] resolves to the platform
 * mono face (no bundled font asset / dependency) and gives the tabular, "matrix" data feel:
 * fixed-width glyphs so digits don't jitter as values tick during live tracking.
 *
 * Use [MilewayType.dataLarge] / [dataMedium] / [dataSmall] for hero counters and inline stats,
 * or [MilewayType.mono] to monospace any existing [TextStyle] (e.g. a Material role) in place.
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
