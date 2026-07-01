package com.mileway.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * E.2: the user's selected map tile provider, replacing the loose `mapProvider` String that lived in
 * ThemeController. A typed enum so map surfaces can branch on it via a `when` instead of string-matching.
 */
enum class MapProvider {
    /** OpenStreetMap raster tiles (the offline-friendly default). */
    OSM,

    /** Satellite imagery basemap. */
    SATELLITE,

    /** Terrain / topographic basemap. */
    TERRAIN,

    ;

    companion object {
        /** Parse a persisted name back to a [MapProvider], falling back to [OSM] for unknown values. */
        fun fromName(name: String?): MapProvider = entries.firstOrNull { it.name == name } ?: OSM
    }
}

/**
 * E.2: app-wide selected [MapProvider], seeded at the theme root from ThemeController. Map hosts read
 * `LocalMapProvider.current` instead of threading a provider string through every call site.
 */
val LocalMapProvider = staticCompositionLocalOf { MapProvider.OSM }
