package com.miletracker.core.maps.maplibre

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

/**
 * E.3 (Android): extracts the bundled MBTiles pack from app assets into `filesDir` once (MapLibre Native
 * can only open `mbtiles://` from a real file, not from inside the APK) and returns its absolute path.
 * Returns `null` if the asset is absent so the surface falls back to the network style.
 */
@Composable
actual fun rememberOfflineMbtilesPath(): String? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val out = File(context.filesDir, OfflineTileProvider.MBTILES_ASSET)
            if (!out.exists() || out.length() == 0L) {
                context.assets.open(OfflineTileProvider.MBTILES_ASSET).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            }
            out.absolutePath
        }.getOrNull()
    }
}
