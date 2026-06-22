package com.miletracker.core.maps.maplibre

import androidx.compose.runtime.Composable

/**
 * E.3 (iOS): offline MBTiles bundling is not wired on iOS yet, so the surface always uses the network
 * style. Returning `null` keeps the shared [MapLibreSurface] offline branch a no-op on iOS.
 */
@Composable
actual fun rememberOfflineMbtilesPath(): String? = null
