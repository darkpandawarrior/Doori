package com.mileway.core.maps.krossmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.farimarwat.krossmap.core.KrossMap
import com.farimarwat.krossmap.core.KrossMapProperties
import com.farimarwat.krossmap.core.rememberKrossCameraPositionState
import com.farimarwat.krossmap.core.rememberKrossMapState
import com.farimarwat.krossmap.model.KrossCoordinate
import com.farimarwat.krossmap.model.KrossMarker
import com.farimarwat.krossmap.model.KrossPolyLine
import com.mileway.core.maps.MapCoordinate
import com.mileway.core.maps.MapSurface
import kotlinx.coroutines.launch

class KrossMapSurface : MapSurface {

    @Composable
    override fun LocationPinMap(
        latitude: Double,
        longitude: Double,
        modifier: Modifier,
    ) {
        val mapState = rememberKrossMapState()
        val cameraState = rememberKrossCameraPositionState(
            latitude = latitude,
            longitude = longitude,
            zoom = 15f,
            cameraFollow = false,
        )
        // KrossMapProperties positional order (v1.3 bytecode):
        // showTraffic, showCompass, showBuildings, showPointOfInterest,
        // enableRotationGesture, enableTiltGesture, enableScrollGesture
        val staticProps = remember {
            KrossMapProperties(false, false, false, false, false, false, false)
        }

        LaunchedEffect(latitude, longitude) {
            mapState.addOrUpdateMarker(
                KrossMarker(
                    coordinate = KrossCoordinate(latitude, longitude),
                    title = "location_pin",
                )
            )
        }

        KrossMap(
            modifier = modifier,
            mapState = mapState,
            cameraPositionState = cameraState,
            properties = staticProps,
        )
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
        // Google Maps serves its own tiles; the bundled offline MBTiles pack only applies to MapLibre.
        @Suppress("UNUSED_PARAMETER") offlineTiles: Boolean,
        modifier: Modifier,
    ) {
        val scope = rememberCoroutineScope()
        val mapState = rememberKrossMapState()
        val cameraState = rememberKrossCameraPositionState(
            latitude = currentLat,
            longitude = currentLng,
            zoom = 17f,
            cameraFollow = false,
        )

        // Camera follow
        LaunchedEffect(currentLat, currentLng, autoCenterEnabled) {
            if (autoCenterEnabled) {
                scope.launch {
                    cameraState.animateCamera(
                        latitude = currentLat,
                        longitude = currentLng,
                        bearing = bearing,
                        durationMillis = 600,
                    )
                }
            }
        }

        // Current position marker
        LaunchedEffect(currentLat, currentLng) {
            mapState.addOrUpdateMarker(
                KrossMarker(KrossCoordinate(currentLat, currentLng), title = "current_pos")
            )
        }

        // Playback cursor
        LaunchedEffect(playbackCoord) {
            if (playbackCoord != null) {
                mapState.addOrUpdateMarker(
                    KrossMarker(KrossCoordinate(playbackCoord.lat, playbackCoord.lng), "playback")
                )
            }
        }

        // Route polyline, remove previous, add updated
        var prevRoute by remember { mutableStateOf<KrossPolyLine?>(null) }
        LaunchedEffect(routeCoords) {
            prevRoute?.let { mapState.removePolyLine(it) }
            if (routeCoords.isNotEmpty()) {
                val pl = KrossPolyLine(
                    points = routeCoords.map { KrossCoordinate(it.lat, it.lng) },
                    title = "route",
                    color = Color(0xFF1565C0),
                    width = 6f,
                )
                mapState.addPolyLine(pl)
                prevRoute = pl
            }
        }

        // Filtered points polyline (orange, issue markers)
        var prevFiltered by remember { mutableStateOf<KrossPolyLine?>(null) }
        LaunchedEffect(filteredCoords, showIssueMarkers) {
            prevFiltered?.let { mapState.removePolyLine(it) }
            prevFiltered = null
            if (showIssueMarkers && filteredCoords.isNotEmpty()) {
                val pl = KrossPolyLine(
                    points = filteredCoords.map { KrossCoordinate(it.lat, it.lng) },
                    title = "filtered",
                    color = Color(0xFFFF9800),
                    width = 4f,
                )
                mapState.addPolyLine(pl)
                prevFiltered = pl
            }
        }

        // Abnormal points polyline (red, issue markers)
        var prevAbnormal by remember { mutableStateOf<KrossPolyLine?>(null) }
        LaunchedEffect(abnormalCoords, showIssueMarkers) {
            prevAbnormal?.let { mapState.removePolyLine(it) }
            prevAbnormal = null
            if (showIssueMarkers && abnormalCoords.isNotEmpty()) {
                val pl = KrossPolyLine(
                    points = abnormalCoords.map { KrossCoordinate(it.lat, it.lng) },
                    title = "abnormal",
                    color = Color(0xFFD32F2F),
                    width = 4f,
                )
                mapState.addPolyLine(pl)
                prevAbnormal = pl
            }
        }

        // Start / end pin markers
        LaunchedEffect(startCoord) {
            startCoord?.let {
                mapState.addOrUpdateMarker(KrossMarker(KrossCoordinate(it.lat, it.lng), "start"))
            }
        }
        LaunchedEffect(endCoord) {
            endCoord?.let {
                mapState.addOrUpdateMarker(KrossMarker(KrossCoordinate(it.lat, it.lng), "end"))
            }
        }

        KrossMap(
            modifier = modifier,
            mapState = mapState,
            cameraPositionState = cameraState,
            // positional: showTraffic, showCompass, showBuildings, showPOI, rotation, tilt, scroll
        properties = KrossMapProperties(showTraffic, showCompass, false, false, true, true, true),
        )
    }
}
