package com.mileway.core.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────────────────────
// Theme-blind escape hatches. These are the reason a design direction only ever half-applied:
// a fixed hex cannot follow the base direction, so any screen reading one renders identically
// under all ten themes. Replace with the Layer-2 role (see LAYERS.md) — `MilewayRoles.*` inside a
// composable, or hoist the colour into the composable that draws it.
// ─────────────────────────────────────────────────────────────────────────────────────────────

@Deprecated("Theme-blind. Use MilewayRoles.approved (see theme/LAYERS.md).")
val StatusGreen = Color(0xFF2E7D32)

@Deprecated("Theme-blind. Use MilewayRoles.pending (see theme/LAYERS.md).")
val StatusAmber = Color(0xFFF57F17)

@Deprecated("Theme-blind. Use MilewayRoles.rejected, or MilewayRoles.destructive for an action.")
val StatusRed = Color(0xFFB71C1C)

@Deprecated("Theme-blind. Use MilewayRoles.informational, or MilewayRoles.distance for route data.")
val StatusBlue = Color(0xFF1565C0)

@Deprecated("Theme-blind. Use MilewayRoles.distance (see theme/LAYERS.md).")
val TrackPolyline = Color(0xFF1565C0)

@Deprecated("Theme-blind. Use MilewayRoles.approved (see theme/LAYERS.md).")
val TrackStart = Color(0xFF2E7D32)

@Deprecated("Theme-blind. Use MilewayRoles.destructive (see theme/LAYERS.md).")
val TrackEnd = Color(0xFFBA1A1A)

@Deprecated("Theme-blind. Use MilewayRoles.pending (see theme/LAYERS.md).")
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
