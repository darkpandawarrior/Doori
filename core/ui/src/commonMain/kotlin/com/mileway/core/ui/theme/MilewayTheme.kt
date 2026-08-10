package com.mileway.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Android 12+ wallpaper-derived dynamic colour scheme, or null when unsupported
 * (older OS or non-Android target). Implemented per platform.
 */
@Composable
expect fun systemDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/**
 * App-wide "square rounded" shape scheme. Every Material 3 component that reads
 * [MaterialTheme.shapes] — Card (medium), Chip/menus/text fields (small/extraSmall),
 * FAB (large), dialogs & sheets (extraLarge) — inherits squared corners from here, so the
 * squared look lands without touching those call sites. Buttons use a fixed pill shape M3
 * does not source from this scheme; they take `shape = DesignTokens.Shape.button` explicitly.
 */
private val MilewayShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(16.dp),
    )

/**
 * App theme — Design Language v2.
 *
 * Colour resolution, in priority order:
 *
 * 1. **Curated [MilewayThemeVariant]** (Matrix / Amoled / Ion / Daybreak) — a fully hand-tuned
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
 * @param typography the type scale to apply. Defaults to the house [MilewayTypography]; a
 *   direction with its own type scale (see `core/ui/theme/direction/`) passes its own here rather
 *   than this file dispatching per-variant, so directions stay additive and merge-safe.
 * @param shapes the corner/shape language to apply. Defaults to the house shape scheme, same
 *   override pattern as [typography].
 */
@Composable
fun MilewayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    milewayTheme: MilewayThemeVariant? = MilewayThemeVariant.DEFAULT,
    palette: AccentPalette = AccentPalette.DEFAULT,
    customSeedHex: String = ThemeDefaults.CUSTOM_THEME,
    useSystemColors: Boolean = ThemeDefaults.USE_SYSTEM_COLORS,
    paletteStyle: String = ThemeDefaults.PALETTE_STYLE,
    mapProvider: MapProvider = ThemeDefaults.MAP_PROVIDER,
    // Derived from the variant, not fixed app-wide. Five design directions were built with their own
    // type scales — and the #1 finding from a vision review of 217 real screenshots was that
    // monospace on CHROME (not data) is what makes the app read as a terminal rather than a finance
    // product. If typography stayed pinned here, picking a direction would swap colours only and the
    // single most important difference between them would be invisible.
    //
    // Still an explicit parameter: an caller that passes one wins, which is how the screenshot
    // harness renders a specific scale on demand.
    typography: Typography = typographyFor(milewayTheme),
    shapes: Shapes = MilewayShapes,
    content: @Composable () -> Unit,
) {
    val isDark = milewayTheme?.isLight?.not() ?: darkTheme

    val seedColor =
        parseHexColor(customSeedHex)
            ?: parseHexColor(milewayTheme?.seedHex ?: palette.seedHex)
            ?: Color(0xFFF5A623)

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

    // Layer 2 (SEMANTIC): the product's meaning vocabulary, derived once from whichever Layer-1
    // base won above. Provided here and NOT re-derived by MilewayDomainTheme — a domain may tint
    // the accent, it may never change what "approved" or an amount looks like.
    val roleColors =
        remember(milewayTheme, useSystemColors, colorScheme, semanticColors) {
            if (milewayTheme != null && !useSystemColors && customSeedHex.isBlank()) {
                milewayTheme.spec.roleColors()
            } else {
                derivedRoleColors(colorScheme, semanticColors)
            }
        }

    CompositionLocalProvider(
        LocalMilewaySemanticColors provides semanticColors,
        LocalMilewayRoleColors provides roleColors,
        // E.2: app-wide map provider, available to any map host via LocalMapProvider.current.
        LocalMapProvider provides mapProvider,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
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
        success = if (isDark) Color(0xFF46C46B) else Color(0xFF1C8F52),
        accentGlow = scheme.primary,
        accentDim = scheme.inversePrimary,
        border = scheme.outline,
        surfaceRaised = scheme.surfaceContainerHigh,
        surfaceHighest = scheme.surfaceContainerHighest,
        useGlow = isDark,
    )


/**
 * The type scale a design direction ships with.
 *
 * Directions that do not define one fall back to the house scale, so adding a variant never forces
 * a typography decision it has no opinion about.
 */
// NOTE: the five direction files disagree on package — three declared
// `package com.mileway.core.ui.theme` (so their symbols are unqualified here) and two used the
// `.direction` subpackage. Left as-is rather than renamed: five concurrent agents each made a
// defensible call, and unifying the package is a mechanical follow-up that should happen in one
// commit rather than being half-done here.
private fun typographyFor(variant: MilewayThemeVariant?): Typography = when (variant) {
    MilewayThemeVariant.LEDGER -> LedgerTypography
    MilewayThemeVariant.SIGNAL -> SignalTypography
    MilewayThemeVariant.PAPER -> PaperTypography
    MilewayThemeVariant.INSTRUMENT -> com.mileway.core.ui.theme.direction.InstrumentTypography
    MilewayThemeVariant.REFINED_EMBER -> com.mileway.core.ui.theme.direction.RefinedEmberTypography
    else -> MilewayTypography
}
