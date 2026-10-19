package com.mileway.core.maps.maplibre

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mileway.core.maps.MapCoordinate
import com.mileway.core.maps.MapSurface
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.MapOverlayScope
import org.maplibre.compose.overlay.MaplibreLogo
import org.maplibre.compose.overlay.include
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

// OpenFreeMap provides free OSM-based vector tiles with no API key.
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

class MapLibreSurface : MapSurface {
    @Composable
    override fun LocationPinMap(
        latitude: Double,
        longitude: Double,
        modifier: Modifier,
    ) {
        // Sources and layers are the MapState content now, not a MaplibreMap trailing lambda.
        val mapState =
            rememberMapState(
                baseStyle = BaseStyle.Uri(STYLE_URL),
                initialCameraPosition =
                    CameraPosition(
                        center = Position(longitude, latitude),
                        zoom = 15.0,
                    ),
            ) {
                val pinJson =
                    remember(latitude, longitude) {
                        MapCoordinate(latitude, longitude).toPointJson()
                    }
                val pinSource = rememberGeoJsonSource(GeoJsonData.JsonString(pinJson))
                CircleLayer(
                    id = "location-pin",
                    source = pinSource,
                    radius = const(9.dp),
                    color = const(Color(0xFF1565C0)),
                    strokeWidth = const(2.dp),
                    strokeColor = const(Color.White),
                )
            }
        MaplibreMap(modifier = modifier, state = mapState)
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
        // E.3: render from the bundled offline MBTiles when requested and available; else the network style.
        val offlinePath = if (offlineTiles) rememberOfflineMbtilesPath() else null
        val baseStyle =
            remember(offlinePath) {
                if (offlinePath != null) {
                    BaseStyle.Json(OfflineTileProvider.offlineStyleJson(offlinePath))
                } else {
                    BaseStyle.Uri(STYLE_URL)
                }
            }

        // showTraffic has no equivalent in open-tile MapLibre, silently ignored.
        //
        // maplibre-compose 0.15.0 removed MapOptions.ornamentOptions: ornaments are composables in
        // a MapOverlay now, not flags. MapOverlay.Default is the old OrnamentOptions.AllEnabled
        // (compass + logo + attribution + scale bar). The no-compass branch is spelled out rather
        // than using MapOverlay.None, because None drops the MapLibre logo and the attribution with
        // it, and attribution is a licence condition of the tiles, not decoration.
        val overlay: @Composable MapOverlayScope.() -> Unit = {
            if (showCompass) {
                include(MapOverlay.Default)
            } else {
                MaplibreLogo(modifier = Modifier.align(Alignment.BottomStart))
            }
        }
        val mapState =
            rememberMapState(
                baseStyle = baseStyle,
                initialCameraPosition =
                    CameraPosition(
                        center = Position(currentLng, currentLat),
                        zoom = 17.0,
                    ),
            ) {
                // Main route polyline (blue)
                if (routeCoords.isNotEmpty()) {
                    val routeJson = remember(routeCoords.size) { routeCoords.toLineStringJson() }
                    val routeSource =
                        rememberGeoJsonSource(GeoJsonData.JsonString(routeJson)) { synchronousTiling = true }
                    LineLayer(
                        id = "route",
                        source = routeSource,
                        color = const(Color(0xFF1565C0)),
                        width = const(4.dp),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round),
                    )
                }

                // Filtered points (orange), mock / paused
                if (showIssueMarkers && filteredCoords.isNotEmpty()) {
                    val filteredJson = remember(filteredCoords.size) { filteredCoords.toMultiPointJson() }
                    val filteredSource = rememberGeoJsonSource(GeoJsonData.JsonString(filteredJson))
                    CircleLayer(
                        id = "filtered-pts",
                        source = filteredSource,
                        radius = const(6.dp),
                        color = const(Color(0xFFFF9800)),
                        strokeWidth = const(1.5.dp),
                        strokeColor = const(Color.White),
                    )
                }

                // Abnormal points (red), GPS spikes
                if (showIssueMarkers && abnormalCoords.isNotEmpty()) {
                    val abnormalJson = remember(abnormalCoords.size) { abnormalCoords.toMultiPointJson() }
                    val abnormalSource = rememberGeoJsonSource(GeoJsonData.JsonString(abnormalJson))
                    CircleLayer(
                        id = "abnormal-pts",
                        source = abnormalSource,
                        radius = const(6.dp),
                        color = const(Color(0xFFD32F2F)),
                        strokeWidth = const(1.5.dp),
                        strokeColor = const(Color.White),
                    )
                }

                // Start pin (green)
                startCoord?.let { start ->
                    val startJson = remember(start) { start.toPointJson() }
                    val startSource = rememberGeoJsonSource(GeoJsonData.JsonString(startJson))
                    CircleLayer(
                        id = "start-pin",
                        source = startSource,
                        radius = const(8.dp),
                        color = const(Color(0xFF388E3C)),
                        strokeWidth = const(2.dp),
                        strokeColor = const(Color.White),
                    )
                }

                // End pin (deep orange)
                endCoord?.let { end ->
                    val endJson = remember(end) { end.toPointJson() }
                    val endSource = rememberGeoJsonSource(GeoJsonData.JsonString(endJson))
                    CircleLayer(
                        id = "end-pin",
                        source = endSource,
                        radius = const(8.dp),
                        color = const(Color(0xFFE64A19)),
                        strokeWidth = const(2.dp),
                        strokeColor = const(Color.White),
                    )
                }

                // Current position, pulsing ring outer
                val currentJson =
                    remember(currentLat, currentLng) {
                        MapCoordinate(currentLat, currentLng).toPointJson()
                    }
                val currentSource =
                    rememberGeoJsonSource(GeoJsonData.JsonString(currentJson)) { synchronousTiling = true }
                CircleLayer(
                    id = "current-pos-ring",
                    source = currentSource,
                    radius = const(16.dp),
                    color = const(Color(0x261565C0)),
                    strokeWidth = const(1.5.dp),
                    strokeColor = const(Color(0xFF1565C0)),
                )
                CircleLayer(
                    id = "current-pos",
                    source = currentSource,
                    radius = const(7.dp),
                    color = const(Color(0xFF1565C0)),
                    strokeWidth = const(2.dp),
                    strokeColor = const(Color.White),
                )

                // Playback cursor (amber)
                playbackCoord?.let { pb ->
                    val pbJson = remember(pb) { pb.toPointJson() }
                    val pbSource = rememberGeoJsonSource(GeoJsonData.JsonString(pbJson))
                    CircleLayer(
                        id = "playback-cursor",
                        source = pbSource,
                        radius = const(8.dp),
                        color = const(Color(0xFFFFC107)),
                        strokeWidth = const(2.dp),
                        strokeColor = const(Color.White),
                    )
                }
            }

        LaunchedEffect(currentLat, currentLng, autoCenterEnabled) {
            if (autoCenterEnabled) {
                mapState.animateCamera(
                    CameraPosition(
                        center = Position(currentLng, currentLat),
                        zoom = 17.0,
                        bearing = bearing.toDouble(),
                    ).toCameraUpdate(),
                )
            }
        }

        MaplibreMap(modifier = modifier, state = mapState, overlay = overlay)
    }
}
