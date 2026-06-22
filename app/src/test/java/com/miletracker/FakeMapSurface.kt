package com.miletracker

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.miletracker.core.maps.MapCoordinate
import com.miletracker.core.maps.MapSurface

/**
 * No-op [MapSurface] for Robolectric/Roborazzi screenshot tests.
 *
 * The real surfaces ([com.miletracker.core.maps.krossmap.KrossMapSurface] /
 * [com.miletracker.core.maps.maplibre.MapLibreSurface]) need Google Play Services or
 * MapLibre native, neither of which runs on the JVM. Screens that inject a `MapSurface`
 * (GeoCheckInScreen, MapScreen) render an empty placeholder so the rest of the screen
 * can still be captured. Without this the test Koin graph throws
 * NoDefinitionFoundException for `MapSurface`, which previously crashed the gms unit-test
 * executor (exit 2) and failed `geoCheckInScreen` on noGms.
 */
@Suppress("ktlint:standard:function-naming")
class FakeMapSurface : MapSurface {
    @Composable
    override fun LocationPinMap(
        latitude: Double,
        longitude: Double,
        modifier: Modifier,
    ) {
        Box(modifier)
    }

    @Composable
    override fun LiveTrackMap(
        routeCoords: List<MapCoordinate>,
        filteredCoords: List<MapCoordinate>,
        abnormalCoords: List<MapCoordinate>,
        startCoord: MapCoordinate?,
        endCoord: MapCoordinate?,
        currentLat: Double,
        currentLng: Double,
        bearing: Float,
        autoCenterEnabled: Boolean,
        playbackCoord: MapCoordinate?,
        showIssueMarkers: Boolean,
        showCompass: Boolean,
        showTraffic: Boolean,
        offlineTiles: Boolean,
        modifier: Modifier,
    ) {
        Box(modifier)
    }
}
