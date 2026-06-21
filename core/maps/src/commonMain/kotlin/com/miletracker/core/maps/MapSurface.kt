package com.miletracker.core.maps

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-agnostic map rendering contract.
 *
 * Two surfaces are defined:
 *  - [LocationPinMap]  — static map with a single location pin (geo check-in, address preview)
 *  - [LiveTrackMap]    — full live-tracking map with route polylines, issue markers, and camera follow
 *
 * Implementations are selected at startup via the `maps` product flavor:
 *  - `gms`   → KrossMapSurface   (Google Maps on Android, MapKit on iOS)
 *  - `noGms` → MapLibreSurface   (MapLibre Native on both platforms, OSM tiles, no API key)
 */
@Suppress("ktlint:standard:function-naming")
interface MapSurface {
    /**
     * Renders a static map centred on [latitude]/[longitude] with a single pin.
     * Gestures (pan, zoom) are intentionally disabled — this is a preview, not an interactive map.
     */
    @Composable
    fun LocationPinMap(
        latitude: Double,
        longitude: Double,
        modifier: Modifier = Modifier,
    )

    /**
     * Renders a live-tracking map that reacts to location and route changes.
     *
     * @param routeCoords      Clean route points to draw as the main (blue) polyline.
     * @param filteredCoords   Mock / paused points — drawn orange when [showIssueMarkers] is true.
     * @param abnormalCoords   GPS-spike points — drawn red when [showIssueMarkers] is true.
     * @param startCoord       First clean route point; rendered as a start pin.
     * @param endCoord         Last clean route point; rendered as an end pin.
     * @param currentLat       Live device latitude (moves the bearing arrow marker).
     * @param currentLng       Live device longitude.
     * @param bearing          Device heading in degrees clockwise from north.
     * @param autoCenterEnabled When true the camera tracks [currentLat]/[currentLng].
     * @param playbackCoord    Current playback cursor position, or null when not in playback mode.
     * @param showIssueMarkers When true [filteredCoords] and [abnormalCoords] are rendered.
     */
    @Composable
    fun LiveTrackMap(
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
        showCompass: Boolean = true,
        showTraffic: Boolean = false,
        modifier: Modifier = Modifier,
    )
}
