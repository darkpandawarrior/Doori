package com.mileway.core.maps.maplibre

import androidx.compose.runtime.Composable

/**
 * E.3: resolves the bundled [OfflineTileProvider.MBTILES_ASSET] to an on-device file path MapLibre Native
 * can open via `mbtiles://`, extracting it from app assets on first use. Returns `null` when offline tiles
 * are unavailable on this platform (iOS, for now) or the pack is missing — callers then fall back to the
 * network style. Platform-specific because asset extraction needs a platform file system + context.
 */
@Composable
expect fun rememberOfflineMbtilesPath(): String?
