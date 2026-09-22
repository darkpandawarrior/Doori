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

// ─────────────────────────────────────────────────────────────────────────────────────────────
// Fallback semantic tokens for the legacy seed / system-colour path.
//
// A Material `ColorScheme` carries error but has no warning/info/success, so `derivedSemanticColors`
// has to supply them. These are the design system's values for that fallback and belong here with
// the rest of the palette — inline in MilewayTheme.kt they were six hex literals that had escaped
// the design system. Not deprecated: nothing in Material derives them, so there is no role to
// forward to.
// ─────────────────────────────────────────────────────────────────────────────────────────────

internal val FallbackWarningDark = Color(0xFFF2C14E)
internal val FallbackWarningLight = Color(0xFFB8860B)
internal val FallbackInfoDark = Color(0xFF5BA8F5)
internal val FallbackInfoLight = Color(0xFF1C6FD6)
internal val FallbackSuccessDark = Color(0xFF46C46B)
internal val FallbackSuccessLight = Color(0xFF1C8F52)

// Digit counts of the two hex forms this parser accepts, and the alpha bits an `#RRGGBB`
// string does not carry.
private const val RgbHexDigits = 6
private const val ArgbHexDigits = 8
private const val HexRadix = 16
private const val OpaqueAlphaMask = 0xFF000000L

/**
 * Parses a `#RRGGBB` or `#AARRGGBB` hex string into a [Color].
 * Returns null for blank or malformed input. Multiplatform-safe (no android.graphics).
 */
fun parseHexColor(hex: String): Color? {
    val trimmed = hex.trim().removePrefix("#")
    if (trimmed.length != RgbHexDigits && trimmed.length != ArgbHexDigits) return null
    val value = trimmed.toLongOrNull(HexRadix) ?: return null
    return if (trimmed.length == RgbHexDigits) {
        Color(OpaqueAlphaMask or value)
    } else {
        Color(value)
    }
}
