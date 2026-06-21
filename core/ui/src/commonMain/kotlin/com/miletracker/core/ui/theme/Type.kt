package com.miletracker.core.ui.theme

import androidx.compose.material3.Typography
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
 * Design Language v2 — "mono for data".
 *
 * Numeric readouts (distance, speed, duration, amounts, codes/IDs) use a monospaced family so
 * digits are tabular: figures share a fixed advance width, columns line up and a live-updating
 * value (an odometer, a timer) doesn't jitter as digits change. Apply [MilewayMono] as a base
 * and `.dataStyle(...)` to derive a sized variant from any Material style.
 *
 * JetBrains Mono is not bundled as a font resource in this offline demo, so we lean on the
 * platform monospace family (the same choice the colour-wheel hex field already makes) — it is
 * multiplatform-safe and gives the tabular alignment the design language asks for.
 */
val MilewayMono: TextStyle =
    TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    )

/**
 * Returns [base] re-cast in the monospaced "data" family while keeping its size, line height
 * and weight. Use for any numeric value the user reads as data:
 * `Text(value, style = MaterialTheme.typography.titleMedium.dataStyle())`.
 */
fun TextStyle.dataStyle(): TextStyle =
    copy(fontFamily = FontFamily.Monospace, letterSpacing = 0.sp)
