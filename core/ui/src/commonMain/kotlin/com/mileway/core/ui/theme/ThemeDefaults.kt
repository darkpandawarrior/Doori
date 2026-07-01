package com.mileway.core.ui.theme

/**
 * Default values for every theme preference. Public so Settings UI, the controller, and
 * tests share one source of truth.
 */
object ThemeDefaults {
    /** Seed colour the whole Material scheme is generated from. */
    const val BASE_COLOR = "#6367FA"

    const val USE_DARK_THEME = false

    /** User-picked custom seed (hex). Blank = use the preset seed. */
    const val CUSTOM_THEME = ""

    const val THEME_VARIANT = "DEFAULT"

    /** MaterialKolor palette style name. */
    const val PALETTE_STYLE = "TonalSpot"

    /** Android 12+ wallpaper-derived dynamic colours. */
    const val USE_SYSTEM_COLORS = false

    // E.2: typed default map provider (was the String "OSM").
    val MAP_PROVIDER = MapProvider.OSM
}

/**
 * Palette-style names selectable in Settings. Mirrors `com.materialkolor.PaletteStyle`,
 * kept as strings so feature modules don't need the generator library on their classpath.
 */
val PaletteStyleNames: List<String> =
    listOf(
        "TonalSpot", "Neutral", "Vibrant", "Expressive",
        "Rainbow", "FruitSalad", "Monochrome", "Fidelity", "Content",
    )
