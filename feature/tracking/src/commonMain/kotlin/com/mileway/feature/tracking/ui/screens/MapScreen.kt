@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

/**
 * MapScreen.kt
 *
 * Ultimate Enhanced Live Tracking Screen for Mileway.
 *
 * Feature Set:
 * - Fullscreen map with animated direction marker using bearing/gyroscope data
 * - Gyroscope-based tilt visualization and device orientation detection
 * - Bearing accuracy confidence indicators
 * - Route playback animation with speed controls (0.25x - 50x)
 * - Compact live stats card with quality metrics
 * - Bottom control panel with Journey/Layers/Settings/Playback tabs
 * - Real-time bearing-based direction animations (100ms updates)
 * - Smart visibility management to prevent overlaps
 * - Responsive design for all screen sizes
 * - Device mounting detection (mounted vs handheld)
 * - Data quality scoring and feedback
 */

package com.mileway.feature.tracking.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.state.UiState
import com.mileway.core.maps.MapCoordinate
import com.mileway.core.maps.MapSurface
import com.mileway.core.platform.AppPermission
import com.mileway.core.platform.PermissionsProvider
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_close
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_cd_center_map
import com.mileway.core.ui.resources.tracking_cd_collapse
import com.mileway.core.ui.resources.tracking_cd_collapse_markers
import com.mileway.core.ui.resources.tracking_cd_expand
import com.mileway.core.ui.resources.tracking_cd_expand_markers
import com.mileway.core.ui.resources.tracking_cd_gps
import com.mileway.core.ui.resources.tracking_cd_zoom_in
import com.mileway.core.ui.resources.tracking_cd_zoom_out
import com.mileway.core.ui.resources.tracking_map_cd_heading
import com.mileway.core.ui.resources.tracking_map_confidence_high
import com.mileway.core.ui.resources.tracking_map_confidence_low
import com.mileway.core.ui.resources.tracking_map_confidence_medium
import com.mileway.core.ui.resources.tracking_map_current_speed
import com.mileway.core.ui.resources.tracking_map_data_quality
import com.mileway.core.ui.resources.tracking_map_filters_unavailable
import com.mileway.core.ui.resources.tracking_map_getting_location
import com.mileway.core.ui.resources.tracking_map_grant_permissions
import com.mileway.core.ui.resources.tracking_map_gyroscope
import com.mileway.core.ui.resources.tracking_map_heatmap_legend
import com.mileway.core.ui.resources.tracking_map_info_gps_desc
import com.mileway.core.ui.resources.tracking_map_info_gps_title
import com.mileway.core.ui.resources.tracking_map_info_gyro_desc
import com.mileway.core.ui.resources.tracking_map_info_gyro_title
import com.mileway.core.ui.resources.tracking_map_information
import com.mileway.core.ui.resources.tracking_map_journey_controls
import com.mileway.core.ui.resources.tracking_map_keep_active
import com.mileway.core.ui.resources.tracking_map_layer_accuracy
import com.mileway.core.ui.resources.tracking_map_layer_battery
import com.mileway.core.ui.resources.tracking_map_layer_compass
import com.mileway.core.ui.resources.tracking_map_layer_issues
import com.mileway.core.ui.resources.tracking_map_layer_offline_tiles
import com.mileway.core.ui.resources.tracking_map_layer_traffic
import com.mileway.core.ui.resources.tracking_map_marker_default
import com.mileway.core.ui.resources.tracking_map_marker_type
import com.mileway.core.ui.resources.tracking_map_markers
import com.mileway.core.ui.resources.tracking_map_overlays
import com.mileway.core.ui.resources.tracking_map_pause
import com.mileway.core.ui.resources.tracking_map_perm_location
import com.mileway.core.ui.resources.tracking_map_perm_notification
import com.mileway.core.ui.resources.tracking_map_permissions_required
import com.mileway.core.ui.resources.tracking_map_play
import com.mileway.core.ui.resources.tracking_map_playback_empty
import com.mileway.core.ui.resources.tracking_map_playback_speed
import com.mileway.core.ui.resources.tracking_map_playback_x
import com.mileway.core.ui.resources.tracking_map_route_playback
import com.mileway.core.ui.resources.tracking_map_setting_autocenter_desc
import com.mileway.core.ui.resources.tracking_map_setting_autocenter_title
import com.mileway.core.ui.resources.tracking_map_setting_bearing_desc
import com.mileway.core.ui.resources.tracking_map_setting_bearing_title
import com.mileway.core.ui.resources.tracking_map_setting_gyro_desc
import com.mileway.core.ui.resources.tracking_map_setting_gyro_title
import com.mileway.core.ui.resources.tracking_map_setting_orientation_desc
import com.mileway.core.ui.resources.tracking_map_setting_orientation_title
import com.mileway.core.ui.resources.tracking_map_settings
import com.mileway.core.ui.resources.tracking_map_speed_heatmap
import com.mileway.core.ui.resources.tracking_map_speed_legend
import com.mileway.core.ui.resources.tracking_map_start
import com.mileway.core.ui.resources.tracking_map_status_legend_text
import com.mileway.core.ui.resources.tracking_map_status_legend_title
import com.mileway.core.ui.resources.tracking_map_status_mode
import com.mileway.core.ui.resources.tracking_map_status_paused
import com.mileway.core.ui.resources.tracking_map_status_playback
import com.mileway.core.ui.resources.tracking_map_status_recording
import com.mileway.core.ui.resources.tracking_map_stop
import com.mileway.core.ui.resources.tracking_map_stop_hint
import com.mileway.core.ui.resources.tracking_map_tilt
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.LocalMapProvider
import com.mileway.core.ui.theme.MapProvider
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.tracking.map.LiveMapOverlayData
import com.mileway.feature.tracking.map.MapRouteBuilder
import com.mileway.feature.tracking.viewmodel.LiveTrackAction
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "LocationMapScreen"

// ---------------------------------------------------------------------------
// Data model for the map-marker overlay system
// ---------------------------------------------------------------------------

data class MapMarkerData(
    val id: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val title: String = "",
    val type: String = "UNKNOWN",
)

data class MarkerFilters(
    val showCheckIn: Boolean = true,
    val showIssues: Boolean = true,
    val showPause: Boolean = true,
)

// ---------------------------------------------------------------------------
// GPS signal quality levels
// ---------------------------------------------------------------------------

enum class GPSQuality(val label: String, val color: Color) {
    EXCELLENT("Excellent", DesignTokens.StatusColors.success),
    GOOD("Good", DesignTokens.StatusColors.success),
    FAIR("Fair", DesignTokens.StatusColors.warning),
    POOR("Poor", DesignTokens.StatusColors.error),
}

// ---------------------------------------------------------------------------
// Device orientation based on gyroscope data
// ---------------------------------------------------------------------------

enum class DeviceOrientation(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MOUNTED("Mounted", Icons.Default.PhoneAndroid),
    HANDHELD("Handheld", Icons.Default.PhoneAndroid),
    UNKNOWN("Unknown", Icons.Default.PhoneAndroid),
}

// ---------------------------------------------------------------------------
// Main live tracking screen with fullscreen map and floating UI
// ---------------------------------------------------------------------------

@Composable
fun LocationMapScreen(
    viewModel: LiveTrackViewModel = koinViewModel(),
    onNavigateBack: (() -> Unit)? = null,
) {
    val permissionsProvider = koinInject<PermissionsProvider>()
    val scope = rememberCoroutineScope()

    // ViewModel state
    val ui by viewModel.state.collectAsState()
    val liveTrackingState = ui.liveTrackingState
    val locationPointsState = ui.locationPointsState

    val currentTrackData: CurrentTrackData? =
        when (val s = liveTrackingState) {
            is com.mileway.feature.tracking.viewmodel.LiveTrackingUiState.Success -> s.trackData
            else -> null
        }
    val locationPoints: List<LocationData> =
        when (val s = locationPointsState) {
            is UiState.Success -> s.data
            else -> emptyList()
        }
    val isTracking = currentTrackData?.isTracking ?: false
    val activeTrack: CurrentTrackData? = currentTrackData

    // Synthesise a minimal LocationData for current position from CurrentTrackData
    val currentLocation: LocationData? =
        currentTrackData?.let { t ->
            locationPoints.lastOrNull() ?: LocationData(
                activity = t.trackingActivity,
                speed = t.speed.toFloat(),
                lat = t.startLatitude,
                lng = t.startLongitude,
                token = t.token,
                date = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                batteryPercentage = 0.0,
            )
        }

    // Marker state, no-op stubs for demo
    val markers: List<MapMarkerData> = emptyList()
    var markerFilters by remember { mutableStateOf(MarkerFilters()) }
    var selectedMarker by remember { mutableStateOf<MapMarkerData?>(null) }

    // Azimuth from last location bearing (GPS bearing takes priority; magnetometer omitted on KMP)
    val currentAzimuth = 0f

    // Permission states — async-checked via PermissionsProvider
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasLocationPermission = permissionsProvider.isGranted(AppPermission.LOCATION)
        hasNotificationPermission = permissionsProvider.isGranted(AppPermission.NOTIFICATIONS)
    }

    // UI state
    var controlsExpanded by remember { mutableStateOf(false) }
    var selectedControlTab by remember { mutableIntStateOf(0) }
    var autoCenterEnabled by remember { mutableStateOf(true) }

    // Enhanced features
    var showGyroscopeVisualization by remember { mutableStateOf(false) }
    var showBearingConfidence by remember { mutableStateOf(true) }
    var showOrientationDetection by remember { mutableStateOf(true) }

    // Request missing permissions on launch
    LaunchedEffect(hasLocationPermission, hasNotificationPermission) {
        if (!hasLocationPermission) {
            permissionsProvider.request(AppPermission.LOCATION)
            hasLocationPermission = permissionsProvider.isGranted(AppPermission.LOCATION)
        }
        if (!hasNotificationPermission) {
            permissionsProvider.request(AppPermission.NOTIFICATIONS)
            hasNotificationPermission = permissionsProvider.isGranted(AppPermission.NOTIFICATIONS)
        }
    }

    // Refresh data
    LaunchedEffect(Unit) { viewModel.onAction(LiveTrackAction.Refresh) }

    // Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasLocationPermission || !hasNotificationPermission) {
            PermissionRequestScreen(
                hasLocationPermission = hasLocationPermission,
                hasNotificationPermission = hasNotificationPermission,
                onRequestPermissions = {
                    scope.launch {
                        permissionsProvider.request(AppPermission.LOCATION)
                        hasLocationPermission = permissionsProvider.isGranted(AppPermission.LOCATION)
                        permissionsProvider.request(AppPermission.NOTIFICATIONS)
                        hasNotificationPermission = permissionsProvider.isGranted(AppPermission.NOTIFICATIONS)
                    }
                },
            )
        } else if (currentLocation != null) {
            EnhancedLiveTrackingUI(
                currentLocation = currentLocation,
                azimuth = currentAzimuth,
                isTracking = isTracking,
                locationPoints = locationPoints,
                smoothedLocationPoints = locationPoints,
                activeTrack = activeTrack,
                markers = markers,
                markerFilters = markerFilters,
                selectedMarker = selectedMarker,
                controlsExpanded = controlsExpanded,
                selectedControlTab = selectedControlTab,
                autoCenterEnabled = autoCenterEnabled,
                showGyroscopeVisualization = showGyroscopeVisualization,
                showBearingConfidence = showBearingConfidence,
                showOrientationDetection = showOrientationDetection,
                onToggleControls = { controlsExpanded = !controlsExpanded },
                onTabChange = { selectedControlTab = it },
                onToggleAutoCenter = { autoCenterEnabled = !autoCenterEnabled },
                onToggleGyroscope = { showGyroscopeVisualization = !showGyroscopeVisualization },
                onToggleBearingConfidence = { showBearingConfidence = !showBearingConfidence },
                onToggleOrientation = { showOrientationDetection = !showOrientationDetection },
                onStartTracking = { viewModel.onAction(LiveTrackAction.Refresh) },
                onPauseTracking = { viewModel.onAction(LiveTrackAction.Refresh) },
                onMarkerClick = { selectedMarker = it },
                onDismissMarker = { selectedMarker = null },
                onMarkerFiltersChanged = { markerFilters = it },
                onNavigateBack = onNavigateBack,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(Res.string.tracking_map_getting_location),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Permission request screen
// ---------------------------------------------------------------------------

@Composable
fun PermissionRequestScreen(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestPermissions: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier =
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 400.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = stringResource(Res.string.tracking_map_permissions_required),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                if (!hasLocationPermission) {
                    InfoRow(
                        icon = Icons.Default.MyLocation,
                        text = stringResource(Res.string.tracking_map_perm_location),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (!hasNotificationPermission) {
                    InfoRow(
                        icon = Icons.Default.Info,
                        text = stringResource(Res.string.tracking_map_perm_notification),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    shape = DesignTokens.Shape.button,
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.tracking_map_grant_permissions))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------------------------------------------------------------------
// Enhanced live tracking UI with fullscreen map and floating elements
// ---------------------------------------------------------------------------

@OptIn(FlowPreview::class)
@Composable
fun EnhancedLiveTrackingUI(
    currentLocation: LocationData,
    azimuth: Float,
    isTracking: Boolean,
    locationPoints: List<LocationData>,
    smoothedLocationPoints: List<LocationData>,
    activeTrack: CurrentTrackData?,
    markers: List<MapMarkerData>,
    markerFilters: MarkerFilters,
    selectedMarker: MapMarkerData?,
    controlsExpanded: Boolean,
    selectedControlTab: Int,
    autoCenterEnabled: Boolean,
    showGyroscopeVisualization: Boolean,
    showBearingConfidence: Boolean,
    showOrientationDetection: Boolean,
    onToggleControls: () -> Unit,
    onTabChange: (Int) -> Unit,
    onToggleAutoCenter: () -> Unit,
    onToggleGyroscope: () -> Unit,
    onToggleBearingConfidence: () -> Unit,
    onToggleOrientation: () -> Unit,
    onStartTracking: () -> Unit,
    onPauseTracking: () -> Unit,
    onMarkerClick: (MapMarkerData) -> Unit,
    onDismissMarker: () -> Unit,
    onMarkerFiltersChanged: (MarkerFilters) -> Unit,
    onNavigateBack: (() -> Unit)?,
    mapSurface: MapSurface = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()

    // E.2: the app-wide selected map provider; a satellite basemap defaults the traffic overlay on.
    val mapProvider = LocalMapProvider.current

    // Layer toggles
    var speedHeatmap by remember { mutableStateOf(false) }
    var showAccuracy by remember { mutableStateOf(false) }
    var showBattery by remember { mutableStateOf(false) }
    var showIssues by remember { mutableStateOf(false) }
    var showOfflineTiles by remember { mutableStateOf(false) }
    var showCompass by remember { mutableStateOf(true) }
    var showTraffic by remember { mutableStateOf(mapProvider == MapProvider.SATELLITE) }

    // Playback state
    var isPlayingBack by remember { mutableStateOf(false) }
    var playbackIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    // Live duration ticker
    var liveDuration by remember { mutableLongStateOf(0L) }

    // Bearing calculations
    val currentBearing = currentLocation.bearing.takeIf { it != 0f } ?: azimuth
    val deviceOrientation = detectDeviceOrientation(currentLocation)

    // Live duration ticker
    LaunchedEffect(isTracking, activeTrack) {
        if (isTracking && activeTrack != null) {
            while (isTracking) {
                liveDuration = kotlin.time.Clock.System.now().toEpochMilliseconds() - activeTrack.startTime
                delay(1000)
            }
        }
    }

    // Playback animation, advance playback index on each tick.
    LaunchedEffect(isPlayingBack, playbackSpeed, playbackIndex) {
        if (isPlayingBack && locationPoints.isNotEmpty() && playbackIndex < locationPoints.size) {
            delay((500 / playbackSpeed).toLong())
            if (playbackIndex < locationPoints.size - 1) {
                playbackIndex++
            } else {
                isPlayingBack = false
                playbackIndex = 0
            }
        }
    }

    // Derive route data from location points (pure Kotlin, no map dependency)
    val routeData = remember(locationPoints) { MapRouteBuilder.build(locationPoints) }
    val playbackCoord =
        if (isPlayingBack && playbackIndex < locationPoints.size) {
            locationPoints[playbackIndex].let { MapCoordinate(it.lat, it.lng) }
        } else {
            null
        }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map: rendered by the active flavor's MapSurface implementation
        mapSurface.LiveTrackMap(
            routeCoords = routeData.routeCoords.map { MapCoordinate(it.lat, it.lng) },
            filteredCoords = routeData.filteredCoords.map { MapCoordinate(it.lat, it.lng) },
            abnormalCoords = routeData.abnormalCoords.map { MapCoordinate(it.lat, it.lng) },
            startCoord = routeData.startCoord?.let { MapCoordinate(it.lat, it.lng) },
            endCoord =
                routeData.endCoord?.takeIf { locationPoints.size > 1 }
                    ?.let { MapCoordinate(it.lat, it.lng) },
            currentLat = currentLocation.lat,
            currentLng = currentLocation.lng,
            bearing = currentBearing,
            autoCenterEnabled = autoCenterEnabled && (isTracking || isPlayingBack),
            playbackCoord = playbackCoord,
            showIssueMarkers = showIssues,
            showCompass = showCompass,
            showTraffic = showTraffic,
            offlineTiles = showOfflineTiles,
            modifier = Modifier.fillMaxSize(),
        )

        // Address chip + heading indicator (top-end): current-fix reverse-geocode (fully offline,
        // via OfflineLocationNameResolver) plus a small rotated arrow from the live bearing.
        AnimatedVisibility(
            visible = !controlsExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 16.dp)
                    .zIndex(17f),
        ) {
            LiveMapAddressChip(
                latitude = currentLocation.lat,
                longitude = currentLocation.lng,
                bearing = currentBearing,
            )
        }

        // Live indicator badge (top-center)
        AnimatedVisibility(
            visible = isTracking && !controlsExpanded && !isPlayingBack,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 130.dp)
                    .zIndex(14f),
        ) {
            EnhancedLiveIndicatorBadge(
                duration = liveDuration,
                gpsQuality = getGPSQuality(currentLocation.accuracy),
                isRecording = isTracking,
                bearingAccuracy = currentLocation.bearingAccuracyDegrees,
                showBearingConfidence = showBearingConfidence,
                deviceOrientation = deviceOrientation,
                showOrientation = showOrientationDetection,
            )
        }

        // Gyroscope visualization overlay
        AnimatedVisibility(
            visible = showGyroscopeVisualization && !controlsExpanded && isTracking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 200.dp, end = 16.dp)
                    .zIndex(15f),
        ) {
            GyroscopeVisualization(
                gyroscopeX = currentLocation.gyroscopeX,
                gyroscopeY = currentLocation.gyroscopeY,
                gyroscopeZ = currentLocation.gyroscopeZ,
            )
        }

        // Map control cluster (right-side)
        AnimatedVisibility(
            visible = !controlsExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .zIndex(16f),
        ) {
            LiveMapControlCluster(
                onCenterMap = { onToggleAutoCenter() },
                onZoomIn = {},
                onZoomOut = {},
            )
        }

        // Compact stats card
        AnimatedVisibility(
            visible = !controlsExpanded && !isPlayingBack,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .zIndex(12f),
        ) {
            EnhancedCompactLiveStatsCard(
                currentLocation = currentLocation,
                locationPoints = locationPoints,
                liveDuration = liveDuration,
                showDataQuality = true,
            )
        }

        // Playback indicator
        AnimatedVisibility(
            visible = isPlayingBack,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .zIndex(12f),
        ) {
            val currentPoint = locationPoints.getOrNull(playbackIndex)
            PlaybackIndicator(
                currentIndex = playbackIndex,
                totalPoints = locationPoints.size,
                playbackSpeed = playbackSpeed,
                currentLocation = currentPoint,
            )
        }

        // Bottom control panel
        EnhancedLiveControlPanel(
            isTracking = isTracking,
            locationPoints = locationPoints,
            speedHeatmap = speedHeatmap,
            showAccuracy = showAccuracy,
            showBattery = showBattery,
            showIssues = showIssues,
            showOfflineTiles = showOfflineTiles,
            showCompass = showCompass,
            showTraffic = showTraffic,
            autoCenterEnabled = autoCenterEnabled,
            showGyroscope = showGyroscopeVisualization,
            showBearingConfidence = showBearingConfidence,
            showOrientation = showOrientationDetection,
            controlsExpanded = controlsExpanded,
            selectedTab = selectedControlTab,
            isPlayingBack = isPlayingBack,
            playbackIndex = playbackIndex,
            playbackSpeed = playbackSpeed,
            markerFilters = markerFilters,
            onToggleExpanded = onToggleControls,
            onTabChange = onTabChange,
            onStartTracking = onStartTracking,
            onPauseTracking = onPauseTracking,
            onToggleSpeedHeatmap = { speedHeatmap = it },
            onToggleAccuracy = { showAccuracy = it },
            onToggleBattery = { showBattery = it },
            onToggleIssues = { showIssues = it },
            onToggleOfflineTiles = { showOfflineTiles = it },
            onToggleCompass = { showCompass = it },
            onToggleTraffic = { showTraffic = it },
            onToggleAutoCenter = onToggleAutoCenter,
            onToggleGyroscope = onToggleGyroscope,
            onToggleBearingConfidence = onToggleBearingConfidence,
            onToggleOrientation = onToggleOrientation,
            onMarkerFiltersChanged = onMarkerFiltersChanged,
            onStartPlayback = {
                if (locationPoints.isNotEmpty()) {
                    isPlayingBack = true
                    playbackIndex = 0
                }
            },
            onPausePlayback = { isPlayingBack = false },
            onStopPlayback = {
                isPlayingBack = false
                playbackIndex = 0
            },
            onSeekPlayback = { newIndex ->
                playbackIndex = newIndex.coerceIn(0, locationPoints.size - 1)
            },
            onChangePlaybackSpeed = { playbackSpeed = it },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(13f),
        )

        // Back button
        SmallFloatingActionButton(
            onClick = { onNavigateBack?.invoke() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 56.dp, start = 16.dp)
                    .size(56.dp)
                    .zIndex(20f),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.tracking_cd_back),
                modifier = Modifier.size(28.dp),
            )
        }

        // Marker info dialog (stub, no-op in demo)
        if (selectedMarker != null) {
            MarkerInfoDialog(
                marker = selectedMarker,
                onDismiss = onDismissMarker,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Marker info dialog
// ---------------------------------------------------------------------------

@Composable
fun MarkerInfoDialog(
    marker: MapMarkerData,
    onDismiss: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = DesignTokens.Shape.roundedMd,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(marker.title.ifEmpty { stringResource(Res.string.tracking_map_marker_default) }, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.tracking_map_marker_type, marker.type), style = MaterialTheme.typography.bodySmall)
                Button(
                    shape = DesignTokens.Shape.button,
                    onClick = onDismiss,
                ) { Text(stringResource(Res.string.tracking_action_close)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Marker filter chips
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkerFilterChips(
    filters: MarkerFilters,
    onFiltersChanged: (MarkerFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    // No-op stub for demo
    Text(
        text = stringResource(Res.string.tracking_map_filters_unavailable),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(8.dp),
    )
}

// ---------------------------------------------------------------------------
// Map legend composables
// ---------------------------------------------------------------------------

@Composable
fun HeatmapLegend() {
    Text(
        text = stringResource(Res.string.tracking_map_heatmap_legend),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun StatusLegend() {
    Text(
        text = stringResource(Res.string.tracking_map_status_legend_text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// Enhanced live indicator badge
// ---------------------------------------------------------------------------

@Composable
fun EnhancedLiveIndicatorBadge(
    duration: Long,
    gpsQuality: GPSQuality,
    isRecording: Boolean,
    bearingAccuracy: Float,
    showBearingConfidence: Boolean,
    deviceOrientation: DeviceOrientation,
    showOrientation: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val screenWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp().value }
    val isSmallScreen = screenWidthDp < 400

    val alpha by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0.3f,
        animationSpec = tween(500),
        label = "recordingAlpha",
    )

    Card(
        modifier =
            modifier
                .widthIn(max = if (isSmallScreen) 240.dp else 280.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
            ),
        shape = DesignTokens.Shape.roundedLg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = stringResource(Res.string.tracking_map_status_recording),
                tint = Color.Red.copy(alpha = alpha),
                modifier = Modifier.size(12.dp),
            )

            Text(
                text = formatLiveDuration(duration),
                style = MaterialTheme.typography.labelLarge.dataStyle(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 1,
            )

            Box(
                modifier =
                    Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f)),
            )

            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = stringResource(Res.string.tracking_cd_gps),
                tint = gpsQuality.color,
                modifier = Modifier.size(12.dp),
            )

            Text(
                text = gpsQuality.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 1,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Bearing confidence indicator
// ---------------------------------------------------------------------------

@Composable
fun BearingConfidenceIndicator(
    accuracy: Float,
    modifier: Modifier = Modifier,
) {
    val confidence =
        when {
            accuracy <= 15 -> stringResource(Res.string.tracking_map_confidence_high)
            accuracy <= 45 -> stringResource(Res.string.tracking_map_confidence_medium)
            else -> stringResource(Res.string.tracking_map_confidence_low)
        }

    val color =
        when {
            accuracy <= 15 -> MilewayColors.success
            accuracy <= 45 -> MilewayColors.warning
            else -> MilewayColors.danger
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(DesignTokens.Shape.button)
                    .background(color),
        )
        Text(
            text = "$confidence (${accuracy.toInt()}°)",
            style = MaterialTheme.typography.labelSmall.dataStyle(),
            color = MaterialTheme.colorScheme.onErrorContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Device orientation indicator
// ---------------------------------------------------------------------------

@Composable
fun DeviceOrientationIndicator(
    orientation: DeviceOrientation,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = orientation.icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = orientation.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Gyroscope visualization overlay
// ---------------------------------------------------------------------------

@Composable
fun GyroscopeVisualization(
    gyroscopeX: Float,
    gyroscopeY: Float,
    gyroscopeZ: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ),
        shape = DesignTokens.Shape.roundedSm,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.tracking_map_gyroscope),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            GyroscopeAxis("X", gyroscopeX, Color(0xFFFF5722))
            GyroscopeAxis("Y", gyroscopeY, Color(0xFF4CAF50))
            GyroscopeAxis("Z", gyroscopeZ, Color(0xFF2196F3))

            val tiltMagnitude = sqrt(gyroscopeX * gyroscopeX + gyroscopeY * gyroscopeY + gyroscopeZ * gyroscopeZ)
            Text(
                text = stringResource(Res.string.tracking_map_tilt, (kotlin.math.round(tiltMagnitude * 100).toLong() / 100.0).toString()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun GyroscopeAxis(
    label: String,
    value: Float,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(12.dp),
        )

        LinearProgressIndicator(
            progress = { (abs(value) / 5f).coerceIn(0f, 1f) },
            modifier =
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(DesignTokens.Shape.button),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )

        Text(
            text = (kotlin.math.round(value * 10).toLong() / 10.0).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End,
        )
    }
}

// ---------------------------------------------------------------------------
// Playback indicator
// ---------------------------------------------------------------------------

@Composable
fun PlaybackIndicator(
    currentIndex: Int,
    totalPoints: Int,
    playbackSpeed: Float,
    currentLocation: LocationData? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
            ),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.tracking_map_playback_x, playbackSpeed.toString()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "${currentIndex + 1} / $totalPoints",
                    style = MaterialTheme.typography.bodyMedium.dataStyle(),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            val progress = if (totalPoints > 0) currentIndex.toFloat() / totalPoints else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
            )

            if (currentLocation != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = kotlin.math.round(currentLocation.speed * 3.6f).toInt().toString(),
                            style = MaterialTheme.typography.labelLarge.dataStyle(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "km/h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint =
                                when {
                                    currentLocation.accuracy <= 10f -> MilewayColors.success
                                    currentLocation.accuracy <= 20f -> MilewayColors.warning
                                    else -> MilewayColors.danger
                                },
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${currentLocation.accuracy.toInt()}m",
                            style = MaterialTheme.typography.labelLarge.dataStyle(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Enhanced compact live stats card
// ---------------------------------------------------------------------------

@Composable
fun EnhancedCompactLiveStatsCard(
    currentLocation: LocationData,
    locationPoints: List<LocationData>,
    liveDuration: Long,
    showDataQuality: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val screenWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp().value }
    val isSmallScreen = screenWidthDp < 400
    val maxCardWidth = if (isSmallScreen) (screenWidthDp * 0.92).dp else 520.dp

    val totalDistance = calculateTotalDistance(locationPoints)
    val currentSpeed = currentLocation.speed * 3.6f
    val dataQualityScore = if (showDataQuality) calculateDataQualityScore(locationPoints) else null

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSmallScreen) 8.dp else 16.dp)
                .widthIn(max = maxCardWidth),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(if (isSmallScreen) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.tracking_map_current_speed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = kotlin.math.round(currentSpeed).toInt().toString(),
                            style = (if (isSmallScreen) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall).dataStyle(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                        Text(
                            text = "km/h",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = if (isSmallScreen) 2.dp else 3.dp),
                            maxLines = 1,
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            modifier = Modifier.size(if (isSmallScreen) 12.dp else 14.dp),
                            tint = Color(0xFF2196F3),
                        )
                        Text(
                            text = "${kotlin.math.round((totalDistance / 1000.0) * 100).toLong() / 100.0} km",
                            style = (if (isSmallScreen) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium).dataStyle(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(if (isSmallScreen) 12.dp else 14.dp),
                            tint = Color(0xFF9C27B0),
                        )
                        Text(
                            text = formatLiveDuration(liveDuration),
                            style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(if (isSmallScreen) 12.dp else 14.dp),
                            tint = Color(0xFF00BCD4),
                        )
                        Text(
                            text = "${locationPoints.size} pts",
                            style = MaterialTheme.typography.bodySmall.dataStyle(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (dataQualityScore != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                DataQualityIndicator(score = dataQualityScore)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Data quality indicator
// ---------------------------------------------------------------------------

@Composable
fun DataQualityIndicator(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val color =
        when {
            score >= 80 -> MilewayColors.success
            score >= 60 -> MilewayColors.warning
            else -> MilewayColors.danger
        }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.tracking_map_data_quality),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier =
                    Modifier
                        .width(60.dp)
                        .height(6.dp)
                        .clip(DesignTokens.Shape.button),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
            )
            Text(
                text = "$score%",
                style = MaterialTheme.typography.labelSmall.dataStyle(),
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Map control cluster
// ---------------------------------------------------------------------------

@Composable
private fun LiveMapControlCluster(
    onCenterMap: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmallFloatingActionButton(
            onClick = onZoomIn,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            elevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                ),
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(Res.string.tracking_cd_zoom_in))
        }

        SmallFloatingActionButton(
            onClick = onCenterMap,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                ),
        ) {
            Icon(imageVector = Icons.Default.MyLocation, contentDescription = stringResource(Res.string.tracking_cd_center_map))
        }

        SmallFloatingActionButton(
            onClick = onZoomOut,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            elevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                ),
        ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(Res.string.tracking_cd_zoom_out))
        }
    }
}

// ---------------------------------------------------------------------------
// Enhanced live control panel with 4 tabs
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedLiveControlPanel(
    isTracking: Boolean,
    locationPoints: List<LocationData>,
    speedHeatmap: Boolean,
    showAccuracy: Boolean,
    showBattery: Boolean,
    showIssues: Boolean,
    showOfflineTiles: Boolean,
    showCompass: Boolean,
    showTraffic: Boolean,
    autoCenterEnabled: Boolean,
    showGyroscope: Boolean,
    showBearingConfidence: Boolean,
    showOrientation: Boolean,
    controlsExpanded: Boolean,
    selectedTab: Int,
    isPlayingBack: Boolean,
    playbackIndex: Int,
    playbackSpeed: Float,
    markerFilters: MarkerFilters,
    onToggleExpanded: () -> Unit,
    onTabChange: (Int) -> Unit,
    onStartTracking: () -> Unit,
    onPauseTracking: () -> Unit,
    onToggleSpeedHeatmap: (Boolean) -> Unit,
    onToggleAccuracy: (Boolean) -> Unit,
    onToggleBattery: (Boolean) -> Unit,
    onToggleIssues: (Boolean) -> Unit,
    onToggleOfflineTiles: (Boolean) -> Unit,
    onToggleCompass: (Boolean) -> Unit,
    onToggleTraffic: (Boolean) -> Unit,
    onToggleAutoCenter: () -> Unit,
    onToggleGyroscope: () -> Unit,
    onToggleBearingConfidence: () -> Unit,
    onToggleOrientation: () -> Unit,
    onMarkerFiltersChanged: (MarkerFilters) -> Unit,
    onStartPlayback: () -> Unit,
    onPausePlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onSeekPlayback: (Int) -> Unit,
    onChangePlaybackSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (controlsExpanded) 180f else 0f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "rotation",
    )

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ),
        ) {
            if (!isTracking) {
                CompactLiveControlHeader(
                    isTracking = isTracking,
                    isPlayingBack = isPlayingBack,
                    pointCount = locationPoints.size,
                    isExpanded = controlsExpanded,
                    onToggleExpanded = onToggleExpanded,
                    onPlayPause = { if (isTracking) onPauseTracking() else onStartTracking() },
                    rotationAngle = rotationAngle,
                )
            }

            AnimatedVisibility(
                visible = controlsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    val availableTabs =
                        remember(isTracking, locationPoints.isNotEmpty()) {
                            buildList {
                                if (!isTracking) add(0 to "Journey")
                                add(1 to "Layers")
                                add(2 to "Settings")
                                if (!isTracking && locationPoints.isNotEmpty()) add(3 to "Playback")
                            }
                        }

                    val selectedTabIndex = availableTabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)

                    PrimaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        availableTabs.forEach { (id, label) ->
                            Tab(
                                selected = selectedTab == id,
                                onClick = { onTabChange(id) },
                                text = {
                                    if (id == 1) {
                                        val activeCount =
                                            listOf(
                                                speedHeatmap,
                                                showAccuracy,
                                                showBattery,
                                                showIssues,
                                                showOfflineTiles,
                                                showTraffic,
                                            ).count { it }
                                        BadgedBox(badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }) {
                                            Text(label, style = MaterialTheme.typography.labelLarge)
                                        }
                                    } else {
                                        Text(label, style = MaterialTheme.typography.labelLarge)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector =
                                            when (id) {
                                                0 -> Icons.Default.Route
                                                1 -> Icons.Default.Layers
                                                2 -> Icons.Default.Settings
                                                else -> Icons.Default.PlayArrow
                                            },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }

                    when (selectedTab) {
                        0 ->
                            LiveJourneyTab(
                                isTracking = isTracking,
                                onStartTracking = onStartTracking,
                                onPauseTracking = onPauseTracking,
                            )
                        1 ->
                            LiveLayersTab(
                                speedHeatmap = speedHeatmap,
                                showAccuracy = showAccuracy,
                                showBattery = showBattery,
                                showIssues = showIssues,
                                showOfflineTiles = showOfflineTiles,
                                showCompass = showCompass,
                                showTraffic = showTraffic,
                                markerFilters = markerFilters,
                                onToggleSpeedHeatmap = onToggleSpeedHeatmap,
                                onToggleAccuracy = onToggleAccuracy,
                                onToggleBattery = onToggleBattery,
                                onToggleIssues = onToggleIssues,
                                onToggleOfflineTiles = onToggleOfflineTiles,
                                onToggleCompass = onToggleCompass,
                                onToggleTraffic = onToggleTraffic,
                                onMarkerFiltersChanged = onMarkerFiltersChanged,
                            )
                        2 ->
                            EnhancedLiveSettingsTab(
                                autoCenterEnabled = autoCenterEnabled,
                                showGyroscope = showGyroscope,
                                showBearingConfidence = showBearingConfidence,
                                showOrientation = showOrientation,
                                onToggleAutoCenter = onToggleAutoCenter,
                                onToggleGyroscope = onToggleGyroscope,
                                onToggleBearingConfidence = onToggleBearingConfidence,
                                onToggleOrientation = onToggleOrientation,
                            )
                        3 ->
                            LivePlaybackTab(
                                locationPoints = locationPoints,
                                isPlaying = isPlayingBack,
                                currentIndex = playbackIndex,
                                playbackSpeed = playbackSpeed,
                                onStartPlayback = onStartPlayback,
                                onPausePlayback = onPausePlayback,
                                onStopPlayback = onStopPlayback,
                                onSeek = onSeekPlayback,
                                onChangeSpeed = onChangePlaybackSpeed,
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactLiveControlHeader(
    isTracking: Boolean,
    isPlayingBack: Boolean,
    pointCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPlayPause: () -> Unit,
    rotationAngle: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val screenWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp().value }
    val isSmallScreen = screenWidthDp < 400

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(
                    horizontal = if (isSmallScreen) 12.dp else 16.dp,
                    vertical = 12.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = Icons.Default.ExpandLess,
            contentDescription = if (isExpanded) stringResource(Res.string.tracking_cd_collapse) else stringResource(Res.string.tracking_cd_expand),
            modifier =
                Modifier
                    .size(28.dp)
                    .rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.primary,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
        ) {
            Icon(
                imageVector =
                    when {
                        isPlayingBack -> Icons.Default.PlayArrow
                        isTracking -> Icons.Default.FiberManualRecord
                        else -> Icons.Default.Pause
                    },
                contentDescription = null,
                tint =
                    when {
                        isPlayingBack -> MaterialTheme.colorScheme.tertiary
                        isTracking -> Color.Red
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(14.dp),
            )
            Text(
                text =
                    when {
                        isPlayingBack -> stringResource(Res.string.tracking_map_status_playback)
                        isTracking -> stringResource(Res.string.tracking_map_status_recording)
                        else -> stringResource(Res.string.tracking_map_status_paused)
                    },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = "• $pointCount pts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        if (!isPlayingBack) {
            FloatingActionButton(
                shape = DesignTokens.Shape.button,
                onClick = onPlayPause,
                modifier = Modifier.size(if (isSmallScreen) 44.dp else 48.dp),
                containerColor = if (isTracking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) stringResource(Res.string.tracking_map_pause) else stringResource(Res.string.tracking_map_start),
                    tint = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (isSmallScreen) 20.dp else 24.dp),
                )
            }
        }
    }
}

@Composable
fun LiveJourneyTab(
    isTracking: Boolean,
    onStartTracking: () -> Unit,
    onPauseTracking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.tracking_map_journey_controls),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Button(
            shape = DesignTokens.Shape.button,
            onClick = if (isTracking) onPauseTracking else onStartTracking,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = if (isTracking) MilewayColors.warning else MilewayColors.success,
                ),
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isTracking) stringResource(Res.string.tracking_map_pause) else stringResource(Res.string.tracking_map_start), fontWeight = FontWeight.Bold)
        }

        HorizontalDivider()

        Text(
            text = stringResource(Res.string.tracking_map_keep_active),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        HorizontalDivider()

        Text(
            text = stringResource(Res.string.tracking_map_stop_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveLayersTab(
    speedHeatmap: Boolean,
    showAccuracy: Boolean,
    showBattery: Boolean,
    showIssues: Boolean,
    showOfflineTiles: Boolean,
    showCompass: Boolean,
    showTraffic: Boolean,
    markerFilters: MarkerFilters,
    onToggleSpeedHeatmap: (Boolean) -> Unit,
    onToggleAccuracy: (Boolean) -> Unit,
    onToggleBattery: (Boolean) -> Unit,
    onToggleIssues: (Boolean) -> Unit,
    onToggleOfflineTiles: (Boolean) -> Unit,
    onToggleCompass: (Boolean) -> Unit,
    onToggleTraffic: (Boolean) -> Unit,
    onMarkerFiltersChanged: (MarkerFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlaysExpanded by remember { mutableStateOf(true) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { overlaysExpanded = !overlaysExpanded }
                    .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.tracking_map_overlays),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = if (overlaysExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (overlaysExpanded) stringResource(Res.string.tracking_cd_collapse) else stringResource(Res.string.tracking_cd_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = overlaysExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LiveLayersInnerContent(
                    speedHeatmap = speedHeatmap,
                    showAccuracy = showAccuracy,
                    showBattery = showBattery,
                    showIssues = showIssues,
                    showOfflineTiles = showOfflineTiles,
                    showCompass = showCompass,
                    showTraffic = showTraffic,
                    markerFilters = markerFilters,
                    onToggleSpeedHeatmap = onToggleSpeedHeatmap,
                    onToggleAccuracy = onToggleAccuracy,
                    onToggleBattery = onToggleBattery,
                    onToggleIssues = onToggleIssues,
                    onToggleOfflineTiles = onToggleOfflineTiles,
                    onToggleCompass = onToggleCompass,
                    onToggleTraffic = onToggleTraffic,
                    onMarkerFiltersChanged = onMarkerFiltersChanged,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveLayersInnerContent(
    speedHeatmap: Boolean,
    showAccuracy: Boolean,
    showBattery: Boolean,
    showIssues: Boolean,
    showOfflineTiles: Boolean,
    showCompass: Boolean,
    showTraffic: Boolean,
    markerFilters: MarkerFilters,
    onToggleSpeedHeatmap: (Boolean) -> Unit,
    onToggleAccuracy: (Boolean) -> Unit,
    onToggleBattery: (Boolean) -> Unit,
    onToggleIssues: (Boolean) -> Unit,
    onToggleOfflineTiles: (Boolean) -> Unit,
    onToggleCompass: (Boolean) -> Unit,
    onToggleTraffic: (Boolean) -> Unit,
    onMarkerFiltersChanged: (MarkerFilters) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = speedHeatmap,
            onClick = { onToggleSpeedHeatmap(!speedHeatmap) },
            label = {
                Text(
                    if (speedHeatmap) stringResource(Res.string.tracking_map_speed_heatmap) else stringResource(Res.string.tracking_map_status_mode),
                    style = MaterialTheme.typography.labelLarge,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
        )

        FilterChip(
            selected = showAccuracy,
            onClick = { onToggleAccuracy(!showAccuracy) },
            label = { Text(stringResource(Res.string.tracking_map_layer_accuracy), style = MaterialTheme.typography.labelLarge) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        )

        FilterChip(
            selected = showBattery,
            onClick = { onToggleBattery(!showBattery) },
            label = { Text(stringResource(Res.string.tracking_map_layer_battery), style = MaterialTheme.typography.labelLarge) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        )

        FilterChip(
            selected = showIssues,
            onClick = { onToggleIssues(!showIssues) },
            label = { Text(stringResource(Res.string.tracking_map_layer_issues), style = MaterialTheme.typography.labelLarge) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
        )

        FilterChip(
            selected = showOfflineTiles,
            onClick = { onToggleOfflineTiles(!showOfflineTiles) },
            label = { Text(stringResource(Res.string.tracking_map_layer_offline_tiles), style = MaterialTheme.typography.labelLarge) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
        )

        FilterChip(
            selected = showCompass,
            onClick = { onToggleCompass(!showCompass) },
            label = { Text(stringResource(Res.string.tracking_map_layer_compass), style = MaterialTheme.typography.labelLarge) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
        )

        FilterChip(
            selected = showTraffic,
            onClick = { onToggleTraffic(!showTraffic) },
            label = { Text(stringResource(Res.string.tracking_map_layer_traffic), style = MaterialTheme.typography.labelLarge) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
        )
    }

    HorizontalDivider()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (speedHeatmap) stringResource(Res.string.tracking_map_speed_legend) else stringResource(Res.string.tracking_map_status_legend_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (speedHeatmap) {
            HeatmapLegend()
        } else {
            StatusLegend()
        }
    }

    HorizontalDivider()

    var markerFiltersExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { markerFiltersExpanded = !markerFiltersExpanded }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.tracking_map_markers),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = if (markerFiltersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription =
                if (markerFiltersExpanded) {
                    stringResource(
                        Res.string.tracking_cd_collapse_markers,
                    )
                } else {
                    stringResource(Res.string.tracking_cd_expand_markers)
                },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    AnimatedVisibility(
        visible = markerFiltersExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        MarkerFilterChips(
            filters = markerFilters,
            onFiltersChanged = onMarkerFiltersChanged,
        )
    }
}

@Composable
fun EnhancedLiveSettingsTab(
    autoCenterEnabled: Boolean,
    showGyroscope: Boolean,
    showBearingConfidence: Boolean,
    showOrientation: Boolean,
    onToggleAutoCenter: () -> Unit,
    onToggleGyroscope: () -> Unit,
    onToggleBearingConfidence: () -> Unit,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.tracking_map_settings),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SettingCard(
            title = stringResource(Res.string.tracking_map_setting_autocenter_title),
            description = stringResource(Res.string.tracking_map_setting_autocenter_desc),
            enabled = autoCenterEnabled,
            onToggle = onToggleAutoCenter,
        )

        SettingCard(
            title = stringResource(Res.string.tracking_map_setting_gyro_title),
            description = stringResource(Res.string.tracking_map_setting_gyro_desc),
            enabled = showGyroscope,
            onToggle = onToggleGyroscope,
        )

        SettingCard(
            title = stringResource(Res.string.tracking_map_setting_bearing_title),
            description = stringResource(Res.string.tracking_map_setting_bearing_desc),
            enabled = showBearingConfidence,
            onToggle = onToggleBearingConfidence,
        )

        SettingCard(
            title = stringResource(Res.string.tracking_map_setting_orientation_title),
            description = stringResource(Res.string.tracking_map_setting_orientation_desc),
            enabled = showOrientation,
            onToggle = onToggleOrientation,
        )

        HorizontalDivider()

        Text(
            text = stringResource(Res.string.tracking_map_information),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        InfoCard(
            icon = Icons.Default.GpsFixed,
            title = stringResource(Res.string.tracking_map_info_gps_title),
            description = stringResource(Res.string.tracking_map_info_gps_desc),
        )

        InfoCard(
            icon = Icons.Default.PhoneAndroid,
            title = stringResource(Res.string.tracking_map_info_gyro_title),
            description = stringResource(Res.string.tracking_map_info_gyro_desc),
        )
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
fun LivePlaybackTab(
    locationPoints: List<LocationData>,
    isPlaying: Boolean,
    currentIndex: Int,
    playbackSpeed: Float,
    onStartPlayback: () -> Unit,
    onPausePlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onSeek: (Int) -> Unit,
    onChangeSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val speedOptions = listOf(0.25f, 0.5f, 1f, 2f, 5f, 10f, 20f, 50f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.tracking_map_route_playback),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (locationPoints.isEmpty()) {
            Text(
                text = stringResource(Res.string.tracking_map_playback_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isPlaying) {
                    Button(
                        shape = DesignTokens.Shape.button,
                        onClick = onStartPlayback,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.tracking_map_play), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        shape = DesignTokens.Shape.button,
                        onClick = onPausePlayback,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MilewayColors.warning,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.tracking_map_pause), fontWeight = FontWeight.Bold)
                    }
                }

                if (isPlaying || currentIndex > 0) {
                    OutlinedButton(
                        shape = DesignTokens.Shape.button,
                        onClick = onStopPlayback,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.tracking_map_stop), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Position: ${currentIndex + 1} / ${locationPoints.size}",
                    style = MaterialTheme.typography.bodyMedium.dataStyle(),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Slider(
                    value = currentIndex.toFloat(),
                    onValueChange = { newValue -> onSeek(newValue.toInt()) },
                    valueRange = 0f..(locationPoints.size - 1).coerceAtLeast(0).toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.tracking_map_playback_speed),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    speedOptions.forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        Surface(
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            shape = DesignTokens.Shape.roundedMd,
                            modifier =
                                Modifier
                                    .clickable { onChangeSpeed(speed) }
                                    .padding(2.dp),
                        ) {
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.labelMedium,
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onTertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helper functions
// ---------------------------------------------------------------------------

fun calculateTotalDistance(locations: List<LocationData>): Float {
    if (locations.size < 2) return 0f
    return locations.map { it.displacement }.sum().toFloat()
}

fun getGPSQuality(accuracy: Float): GPSQuality {
    return when {
        accuracy <= 10 -> GPSQuality.EXCELLENT
        accuracy <= 20 -> GPSQuality.GOOD
        accuracy <= 50 -> GPSQuality.FAIR
        else -> GPSQuality.POOR
    }
}

fun detectDeviceOrientation(location: LocationData): DeviceOrientation {
    val tiltMagnitude =
        sqrt(
            location.gyroscopeX * location.gyroscopeX +
                location.gyroscopeY * location.gyroscopeY +
                location.gyroscopeZ * location.gyroscopeZ,
        )

    return when {
        tiltMagnitude < 0.5f -> DeviceOrientation.MOUNTED
        tiltMagnitude < 2.0f -> DeviceOrientation.HANDHELD
        else -> DeviceOrientation.UNKNOWN
    }
}

// ---------------------------------------------------------------------------
// Address chip + heading indicator (Wave 3 live-map polish, fully offline)
// ---------------------------------------------------------------------------

/**
 * Small pill showing the current fix's offline-resolved place name (via
 * [OfflineLocationNameResolver], no network) plus a heading arrow rotated from [bearing]. Renders
 * nothing when the resolver has no gazetteer match near [latitude]/[longitude].
 */
@Composable
private fun LiveMapAddressChip(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    modifier: Modifier = Modifier,
) {
    val resolver = remember { com.mileway.core.platform.OfflineLocationNameResolver() }
    val place = remember(latitude, longitude) { resolver.resolveSync(latitude, longitude) }
    val chipText = remember(place) { LiveMapOverlayData.addressChipText(place) } ?: return

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        shape = DesignTokens.Shape.roundedLg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeadingArrow(bearing = bearing)
            Text(
                text = chipText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
    }
}

/** Small arrow icon rotated to [bearing] degrees clockwise from north (device heading). */
@Composable
private fun HeadingArrow(
    bearing: Float,
    modifier: Modifier = Modifier,
) {
    val rotation = remember(bearing) { LiveMapOverlayData.headingRotationDegrees(bearing) }
    Icon(
        imageVector = Icons.Default.MyLocation,
        contentDescription = stringResource(Res.string.tracking_map_cd_heading, rotation.toInt()),
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(16.dp).rotate(rotation),
    )
}

fun calculateDataQualityScore(locations: List<LocationData>): Int {
    if (locations.isEmpty()) return 0

    var score = 100

    val avgAccuracy = locations.map { it.accuracy }.average().toFloat()
    when {
        avgAccuracy > 50 -> score -= 30
        avgAccuracy > 20 -> score -= 15
        avgAccuracy > 10 -> score -= 5
    }

    if (locations.size > 1) {
        var gapCount = 0
        for (i in 1 until locations.size) {
            val timeDiff = locations[i].date - locations[i - 1].date
            if (timeDiff > 10000) gapCount++
        }
        score -= (gapCount * 5).coerceAtMost(20)
    }

    val mockCount = locations.count { it.isMock }
    if (mockCount > 0) score -= 25

    val abnormalCount = locations.count { it.isAbnormal }
    score -= (abnormalCount * 10).coerceAtMost(40)

    return score.coerceIn(0, 100)
}

fun formatLiveDuration(durationMillis: Long): String {
    val seconds = (durationMillis / 1000) % 60
    val minutes = (durationMillis / (1000 * 60)) % 60
    val hours = (durationMillis / (1000 * 60 * 60))
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:$mm:$ss"
    } else {
        "$mm:$ss"
    }
}
