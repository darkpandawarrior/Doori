package com.miletracker.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
 * App theme. The entire Material colour scheme is generated from a single seed colour:
 *
 * 1. [customSeedHex] (colour-wheel pick) wins when non-blank and parseable;
 * 2. otherwise the [palette] preset's seed (default seed [ThemeDefaults.BASE_COLOR]);
 * 3. when [useSystemColors] is on and the platform supports it, wallpaper-derived
 *    dynamic colours replace the generated scheme entirely.
 *
 * [paletteStyle] selects the scheme-generation style (TonalSpot, Vibrant, …); unknown
 * names fall back to TonalSpot.
 */
@Composable
fun MileTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AccentPalette = AccentPalette.DEFAULT,
    customSeedHex: String = ThemeDefaults.CUSTOM_THEME,
    useSystemColors: Boolean = ThemeDefaults.USE_SYSTEM_COLORS,
    paletteStyle: String = ThemeDefaults.PALETTE_STYLE,
    content: @Composable () -> Unit,
) {
    val seedColor =
        parseHexColor(customSeedHex)
            ?: parseHexColor(palette.seedHex)
            ?: Color(0xFF6367FA)

    val style =
        remember(paletteStyle) {
            PaletteStyle.entries.firstOrNull { it.name == paletteStyle } ?: PaletteStyle.TonalSpot
        }

    val generatedScheme =
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme,
            style = style,
        )
    val colorScheme =
        if (useSystemColors) {
            systemDynamicColorScheme(darkTheme) ?: generatedScheme
        } else {
            generatedScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MileTrackerTypography,
        content = content,
    )
}
