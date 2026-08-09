@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

/**
 * RouteReplayScreen.kt
 *
 * Split off MapScreen.kt (2,694 lines) once LiveDriveScreen (ui/live/) became the hero 1-4Hz
 * live-tracking surface. What remains here is the review/playback half only: a fullscreen map of
 * an already-recorded route, with animated route-playback (0.25x - 50x) and a bottom control panel
 * (Layers/Settings/Playback tabs).
 *
 * Deleted along with the split (see git history on MapScreen.kt if any of this is ever wanted for
 * real): the live-mode chrome (recording badge, gyroscope tilt visualization, bearing-confidence
 * and device-orientation indicators, start/pause-tracking controls, the location/notification
 * permission gate — none of it applies to reviewing a route that already finished); the dead
 * zoom in/out control cluster (both buttons were wired to `{}`); the marker layer (MapMarkerData/
 * MarkerFilters/MarkerInfoDialog/MarkerFilterChips — `markers` was hardcoded `emptyList()`, so none
 * of it ever had anything to show); and the speed-heatmap/accuracy/battery/issues filter chips plus
 * their legend text, none of which were ever wired to the actual map render (`showIssueMarkers`
 * aside, none of those booleans reached `mapSurface.LiveTrackMap` at all).
 */

package com.mileway.feature.tracking.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_unit_kmh
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_cd_collapse
import com.mileway.core.ui.resources.tracking_cd_expand
import com.mileway.core.ui.resources.tracking_map_cd_heading
import com.mileway.core.ui.resources.tracking_map_current_speed
import com.mileway.core.ui.resources.tracking_map_data_quality
import com.mileway.core.ui.resources.tracking_map_getting_location
import com.mileway.core.ui.resources.tracking_map_info_gps_desc
import com.mileway.core.ui.resources.tracking_map_info_gps_title
import com.mileway.core.ui.resources.tracking_map_information
import com.mileway.core.ui.resources.tracking_map_layer_compass
import com.mileway.core.ui.resources.tracking_map_layer_offline_tiles
import com.mileway.core.ui.resources.tracking_map_layer_traffic
import com.mileway.core.ui.resources.tracking_map_overlays
import com.mileway.core.ui.resources.tracking_map_pause
import com.mileway.core.ui.resources.tracking_map_play
import com.mileway.core.ui.resources.tracking_map_playback_empty
import com.mileway.core.ui.resources.tracking_map_playback_speed
import com.mileway.core.ui.resources.tracking_map_playback_x
import com.mileway.core.ui.resources.tracking_map_route_playback
import com.mileway.core.ui.resources.tracking_map_setting_autocenter_desc
import com.mileway.core.ui.resources.tracking_map_setting_autocenter_title
import com.mileway.core.ui.resources.tracking_map_settings
import com.mileway.core.ui.resources.tracking_map_stop
import com.mileway.core.ui.resources.tracking_map_status_playback
import com.mileway.core.ui.resources.tracking_map_tab_layers
import com.mileway.core.ui.resources.tracking_map_tab_playback
import com.mileway.core.ui.resources.tracking_qa_settings
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.LocalMapProvider
import com.mileway.core.ui.theme.MapProvider
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.tracking.map.LiveMapOverlayData
import com.mileway.feature.tracking.map.MapRouteBuilder
import com.mileway.feature.tracking.viewmodel.LiveTrackAction
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackingUiState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// ---------------------------------------------------------------------------
// Entry point: review a saved route (map + animated playback)
// ---------------------------------------------------------------------------

@Composable
fun RouteReplayScreen(
    viewModel: LiveTrackViewModel = koinViewModel(),
    onNavigateBack: (() -> Unit)? = null,
) {
    val ui by viewModel.state.collectAsState()
    val currentTrackData: CurrentTrackData? =
        when (val s = ui.liveTrackingState) {
            is LiveTrackingUiState.Success -> s.trackData
            else -> null
        }
    val locationPoints: List<LocationData> =
        when (val s = ui.locationPointsState) {
            is UiState.Success -> s.data
            else -> emptyList()
        }

    // Synthesise a minimal LocationData for the map's centre point from CurrentTrackData when no
    // recorded points exist yet.
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

    var controlsExpanded by remember { mutableStateOf(false) }
    var selectedControlTab by remember { mutableIntStateOf(1) }
    var autoCenterEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { viewModel.onAction(LiveTrackAction.Refresh) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentLocation != null) {
            RouteReplayUI(
                currentLocation = currentLocation,
                locationPoints = locationPoints,
                controlsExpanded = controlsExpanded,
                selectedControlTab = selectedControlTab,
                autoCenterEnabled = autoCenterEnabled,
                onToggleControls = { controlsExpanded = !controlsExpanded },
                onTabChange = { selectedControlTab = it },
                onToggleAutoCenter = { autoCenterEnabled = !autoCenterEnabled },
                onNavigateBack = onNavigateBack,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    // ponytail: reused copy from the live screen ("Getting location…") — this path is
                    // really "loading the recorded route", but no dedicated string exists yet and the
                    // wording gap is cosmetic only.
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
// Map + floating chrome
// ---------------------------------------------------------------------------

@Composable
fun RouteReplayUI(
    currentLocation: LocationData,
    locationPoints: List<LocationData>,
    controlsExpanded: Boolean,
    selectedControlTab: Int,
    autoCenterEnabled: Boolean,
    onToggleControls: () -> Unit,
    onTabChange: (Int) -> Unit,
    onToggleAutoCenter: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    mapSurface: MapSurface = koinInject(),
) {
    // E.2: the app-wide selected map provider; a satellite basemap defaults the traffic overlay on.
    val mapProvider = LocalMapProvider.current

    // Layer toggles — only the ones that actually reach mapSurface.LiveTrackMap below (see file KDoc
    // for what was dropped).
    var showOfflineTiles by remember { mutableStateOf(false) }
    var showCompass by remember { mutableStateOf(true) }
    var showTraffic by remember { mutableStateOf(mapProvider == MapProvider.SATELLITE) }

    // Playback state
    var isPlayingBack by remember { mutableStateOf(false) }
    var playbackIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    val currentBearing = currentLocation.bearing

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
    // Real recorded duration (first fix -> last fix), not a live ticker — there is no "now" for an
    // already-finished route.
    val recordedDurationMs =
        if (locationPoints.size > 1) locationPoints.last().date - locationPoints.first().date else 0L

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
            autoCenterEnabled = autoCenterEnabled && isPlayingBack,
            playbackCoord = playbackCoord,
            // The marker layer this fed had nothing to show (see file KDoc) — always off here.
            showIssueMarkers = false,
            showCompass = showCompass,
            showTraffic = showTraffic,
            offlineTiles = showOfflineTiles,
            modifier = Modifier.fillMaxSize(),
        )

        // Address chip + heading indicator (top-end): last-fix reverse-geocode (fully offline, via
        // OfflineLocationNameResolver) plus a small rotated arrow from the recorded bearing.
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
                liveDuration = recordedDurationMs,
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
        ReplayControlPanel(
            locationPoints = locationPoints,
            showOfflineTiles = showOfflineTiles,
            showCompass = showCompass,
            showTraffic = showTraffic,
            autoCenterEnabled = autoCenterEnabled,
            controlsExpanded = controlsExpanded,
            selectedTab = selectedControlTab,
            isPlayingBack = isPlayingBack,
            playbackIndex = playbackIndex,
            playbackSpeed = playbackSpeed,
            onToggleExpanded = onToggleControls,
            onTabChange = onTabChange,
            onToggleOfflineTiles = { showOfflineTiles = it },
            onToggleCompass = { showCompass = it },
            onToggleTraffic = { showTraffic = it },
            onToggleAutoCenter = onToggleAutoCenter,
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
    }
}

// ---------------------------------------------------------------------------
// Bottom control panel: collapsed header + expanded Layers/Settings/Playback tabs
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayControlPanel(
    locationPoints: List<LocationData>,
    showOfflineTiles: Boolean,
    showCompass: Boolean,
    showTraffic: Boolean,
    autoCenterEnabled: Boolean,
    controlsExpanded: Boolean,
    selectedTab: Int,
    isPlayingBack: Boolean,
    playbackIndex: Int,
    playbackSpeed: Float,
    onToggleExpanded: () -> Unit,
    onTabChange: (Int) -> Unit,
    onToggleOfflineTiles: (Boolean) -> Unit,
    onToggleCompass: (Boolean) -> Unit,
    onToggleTraffic: (Boolean) -> Unit,
    onToggleAutoCenter: () -> Unit,
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
            CompactReplayControlHeader(
                isPlayingBack = isPlayingBack,
                pointCount = locationPoints.size,
                isExpanded = controlsExpanded,
                onToggleExpanded = onToggleExpanded,
                rotationAngle = rotationAngle,
            )

            AnimatedVisibility(
                visible = controlsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    val layersTabLabel = stringResource(Res.string.tracking_map_tab_layers)
                    val settingsTabLabel = stringResource(Res.string.tracking_qa_settings)
                    val playbackTabLabel = stringResource(Res.string.tracking_map_tab_playback)
                    val availableTabs =
                        remember(locationPoints.isNotEmpty()) {
                            buildList {
                                add(1 to layersTabLabel)
                                add(2 to settingsTabLabel)
                                if (locationPoints.isNotEmpty()) add(3 to playbackTabLabel)
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
                                        val activeCount = listOf(showOfflineTiles, showTraffic).count { it }
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
                        1 ->
                            ReplayLayersTab(
                                showOfflineTiles = showOfflineTiles,
                                showCompass = showCompass,
                                showTraffic = showTraffic,
                                onToggleOfflineTiles = onToggleOfflineTiles,
                                onToggleCompass = onToggleCompass,
                                onToggleTraffic = onToggleTraffic,
                            )
                        2 ->
                            ReplaySettingsTab(
                                autoCenterEnabled = autoCenterEnabled,
                                onToggleAutoCenter = onToggleAutoCenter,
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
fun CompactReplayControlHeader(
    isPlayingBack: Boolean,
    pointCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
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
            if (isPlayingBack) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(Res.string.tracking_map_status_playback),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            Text(
                text = "• $pointCount pts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReplayLayersTab(
    showOfflineTiles: Boolean,
    showCompass: Boolean,
    showTraffic: Boolean,
    onToggleOfflineTiles: (Boolean) -> Unit,
    onToggleCompass: (Boolean) -> Unit,
    onToggleTraffic: (Boolean) -> Unit,
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
            text = stringResource(Res.string.tracking_map_overlays),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = showOfflineTiles,
                onClick = { onToggleOfflineTiles(!showOfflineTiles) },
                label = { Text(stringResource(Res.string.tracking_map_layer_offline_tiles), style = MaterialTheme.typography.labelLarge) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
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
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
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
                    Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
            )
        }
    }
}

@Composable
fun ReplaySettingsTab(
    autoCenterEnabled: Boolean,
    onToggleAutoCenter: () -> Unit,
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

// ---------------------------------------------------------------------------
// Playback controls tab
// ---------------------------------------------------------------------------

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
                            text = stringResource(Res.string.core_unit_kmh),
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
// Compact stats card (distance / duration / points / data quality)
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
                            text = stringResource(Res.string.core_unit_kmh),
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
// Address chip + heading indicator (fully offline)
// ---------------------------------------------------------------------------

/**
 * Small pill showing the fix's offline-resolved place name (via [OfflineLocationNameResolver], no
 * network) plus a heading arrow rotated from [bearing]. Renders nothing when the resolver has no
 * gazetteer match near [latitude]/[longitude].
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

/** Small arrow icon rotated to [bearing] degrees clockwise from north (recorded heading). */
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

// ---------------------------------------------------------------------------
// Helper functions
// ---------------------------------------------------------------------------

fun calculateTotalDistance(locations: List<LocationData>): Float {
    if (locations.size < 2) return 0f
    return locations.map { it.displacement }.sum().toFloat()
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
