package com.mileway

import com.mileway.core.maps.maplibre.OfflineTileProvider
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * E.3: the offline MapLibre style is a valid spec-v8 style whose single raster source points at the local
 * MBTiles file via the `mbtiles://` scheme MapLibre Native understands.
 */
class OfflineTileProviderTest {
    @Test
    fun `offline style references the local mbtiles as a raster source`() {
        val path = "/data/user/0/com.mileway/files/demo_region.mbtiles"
        val json = OfflineTileProvider.offlineStyleJson(path)

        assertTrue("spec version", json.contains("\"version\": 8"))
        assertTrue("raster source type", json.contains("\"type\": \"raster\""))
        assertTrue("mbtiles scheme + path", json.contains("mbtiles://$path"))
        assertTrue("raster layer present", json.contains("\"id\": \"offline-tiles\""))
    }

    @Test
    fun `bundled asset name is the demo region pack`() {
        assertTrue(OfflineTileProvider.MBTILES_ASSET.endsWith(".mbtiles"))
    }
}
