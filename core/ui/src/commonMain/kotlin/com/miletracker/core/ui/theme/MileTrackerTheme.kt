package com.miletracker.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Android 12+ wallpaper-derived dynamic colour scheme, or null when unsupported
 * (older OS or non-Android target). Implemented per platform.
 */
@Composable
expect fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/**
 * App theme — Design Language v2.
 *
 * Colour resolution, in priority order:
 *
 * 1. **Curated [MilewayTheme]** (Matrix / Amoled / Ion / Daybreak) — a fully hand-tuned
 *    [ColorScheme] with AA-verified accent/container triplets. This is the default path and
 *    drives both the Material roles *and* the [MilewaySemanticColors] (glow, state colours).
 * 2. **System dynamic colours** — when [useSystemColors] is on and the platform supports it,
 *    Android 12+ wallpaper colours replace the scheme (Material You opt-in).
 * 3. **Generated seed scheme** — when [milewayTheme] is `null` (legacy / custom-seed path),
 *    MaterialKolor generates a scheme from [customSeedHex] or the [palette] preset.
 *
 * Geometry, mono-for-data type, and edge-to-edge behaviour are theme-independent.
 *
 * @param milewayTheme the curated theme to apply; `null` falls back to the legacy seed path.
 *   When non-null it also dictates light/dark (Daybreak is light), so [darkTheme] is ignored.
 */
@Composable
fun MileTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    milewayTheme: MilewayTheme? = MilewayTheme.DEFAULT,
    palette: AccentPalette = AccentPalette.DEFAULT,
    customSeedHex: String = ThemeDefaults.CUSTOM_THEME,
    useSystemColors: Boolean = ThemeDefaults.USE_SYSTEM_COLORS,
    paletteStyle: String = ThemeDefaults.PALETTE_STYLE,
    mapProvider: MapProvider = ThemeDefaults.MAP_PROVIDER,
    content: @Composable () -> Unit,
) {
    val isDark = milewayTheme?.isLight?.not() ?: darkTheme

    val seedColor =
        parseHexColor(customSeedHex)
            ?: parseHexColor(milewayTheme?.seedHex ?: palette.seedHex)
            ?: Color(0xFF00FF41)

    val style =
        remember(paletteStyle) {
            PaletteStyle.entries.firstOrNull { it.name == paletteStyle } ?: PaletteStyle.TonalSpot
        }

    // Generated scheme is still computed (cheap, remembered) for the legacy/custom-seed path and
    // as a fallback when system colours are requested but unavailable.
    val generatedScheme =
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            style = style,
        )

    val colorScheme =
        when {
            useSystemColors -> systemDynamicColorScheme(isDark) ?: (milewayTheme?.colorScheme() ?: generatedScheme)
            // A custom seed always wins over a curated theme so the colour wheel stays meaningful.
            milewayTheme != null && customSeedHex.isBlank() -> milewayTheme.colorScheme()
            else -> generatedScheme
        }

    // Semantic tokens follow the curated theme when one is active; otherwise derive a sensible
    // bundle from the generated scheme so the legacy path still themes glow / states coherently.
    val semanticColors =
        remember(milewayTheme, useSystemColors, colorScheme) {
            if (milewayTheme != null && !useSystemColors && customSeedHex.isBlank()) {
                milewayTheme.spec.semanticColors()
            } else {
                derivedSemanticColors(colorScheme, isDark)
            }
        }

    CompositionLocalProvider(
        LocalMilewaySemanticColors provides semanticColors,
        // E.2: app-wide map provider, available to any map host via LocalMapProvider.current.
        LocalMapProvider provides mapProvider,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MileTrackerTypography,
            content = content,
        )
    }
}

/** Bridge for the legacy seed / system-colour path: synthesise semantic tokens from a scheme. */
private fun derivedSemanticColors(
    scheme: ColorScheme,
    isDark: Boolean,
): MilewaySemanticColors =
    MilewaySemanticColors(
        warning = if (isDark) Color(0xFFF2C14E) else Color(0xFFB8860B),
        danger = scheme.error,
        info = if (isDark) Color(0xFF5BA8F5) else Color(0xFF1C6FD6),
        success = if (isDark) Color(0xFF00FF41) else Color(0xFF1C8F52),
        accentGlow = scheme.primary,
        accentDim = scheme.inversePrimary,
        border = scheme.outline,
        surfaceRaised = scheme.surfaceContainerHigh,
        surfaceHighest = scheme.surfaceContainerHighest,
        useGlow = isDark,
    )
