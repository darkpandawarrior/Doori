package com.mileway.core.maps.maplibre

/**
 * E.3: builds the offline MapLibre style that renders from the bundled `demo_region.mbtiles` raster tiles
 * instead of the network OpenFreeMap style — so the map works with no connectivity. The JSON construction is
 * pure (no platform types) and JVM-unit-testable; resolving the mbtiles file from app assets is the
 * platform half (androidMain `cachedMbtilesPath`).
 */
object OfflineTileProvider {
    /** Bundled offline tile pack, shipped in the app's assets. */
    const val MBTILES_ASSET = "demo_region.mbtiles"

    /**
     * A minimal MapLibre style (spec v8) with a single raster source served from the local [mbtilesPath]
     * via the `mbtiles://` scheme MapLibre Native understands, over a dark background matching the app.
     */
    fun offlineStyleJson(mbtilesPath: String): String =
        """
        {
          "version": 8,
          "sources": {
            "offline-tiles": { "type": "raster", "url": "mbtiles://$mbtilesPath", "tileSize": 256 }
          },
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#0E1116" } },
            { "id": "offline-tiles", "type": "raster", "source": "offline-tiles" }
          ]
        }
        """.trimIndent()
}
