package com.mileway.core.ui.theme

import androidx.compose.ui.graphics.Color

// Status colours (semantic, theme-independent)
val StatusGreen = Color(0xFF2E7D32)
val StatusAmber = Color(0xFFF57F17)
val StatusRed = Color(0xFFB71C1C)
val StatusBlue = Color(0xFF1565C0)

// Track map colours
val TrackPolyline = Color(0xFF1565C0)
val TrackStart = Color(0xFF2E7D32)
val TrackEnd = Color(0xFFBA1A1A)
val TrackPause = Color(0xFFF57F17)

/**
 * Parses a `#RRGGBB` or `#AARRGGBB` hex string into a [Color].
 * Returns null for blank or malformed input. Multiplatform-safe (no android.graphics).
 */
fun parseHexColor(hex: String): Color? {
    val trimmed = hex.trim().removePrefix("#")
    if (trimmed.length != 6 && trimmed.length != 8) return null
    val value = trimmed.toLongOrNull(16) ?: return null
    return if (trimmed.length == 6) {
        Color(0xFF000000L or value)
    } else {
        Color(value)
    }
}
