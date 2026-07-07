@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.mileway.feature.tracking.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.PhoneLocked
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.LoadingScreen
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.SparklineChart
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_cd_data_preview
import com.mileway.core.ui.resources.tracking_detail_avg_speed
import com.mileway.core.ui.resources.tracking_detail_vehicle
import com.mileway.core.ui.resources.tracking_insights_distance
import com.mileway.core.ui.resources.tracking_insights_duration
import com.mileway.core.ui.resources.tracking_insights_journey_summary
import com.mileway.core.ui.resources.tracking_preview_abnormal_removed
import com.mileway.core.ui.resources.tracking_preview_account_binding
import com.mileway.core.ui.resources.tracking_preview_actual_points
import com.mileway.core.ui.resources.tracking_preview_clean_distance
import com.mileway.core.ui.resources.tracking_preview_completeness_pct
import com.mileway.core.ui.resources.tracking_preview_context
import com.mileway.core.ui.resources.tracking_preview_created_at
import com.mileway.core.ui.resources.tracking_preview_data_completeness
import com.mileway.core.ui.resources.tracking_preview_distance_breakdown
import com.mileway.core.ui.resources.tracking_preview_email
import com.mileway.core.ui.resources.tracking_preview_employee_code
import com.mileway.core.ui.resources.tracking_preview_end_app_version
import com.mileway.core.ui.resources.tracking_preview_end_device
import com.mileway.core.ui.resources.tracking_preview_end_ocr
import com.mileway.core.ui.resources.tracking_preview_end_reading
import com.mileway.core.ui.resources.tracking_preview_end_time
import com.mileway.core.ui.resources.tracking_preview_expected_points
import com.mileway.core.ui.resources.tracking_preview_health_assessment
import com.mileway.core.ui.resources.tracking_preview_health_critical
import com.mileway.core.ui.resources.tracking_preview_health_excellent
import com.mileway.core.ui.resources.tracking_preview_health_fair
import com.mileway.core.ui.resources.tracking_preview_health_good
import com.mileway.core.ui.resources.tracking_preview_health_poor
import com.mileway.core.ui.resources.tracking_preview_issue_battery_impact
import com.mileway.core.ui.resources.tracking_preview_issue_battery_title
import com.mileway.core.ui.resources.tracking_preview_issue_killed_impact
import com.mileway.core.ui.resources.tracking_preview_issue_killed_title
import com.mileway.core.ui.resources.tracking_preview_issue_mock_impact
import com.mileway.core.ui.resources.tracking_preview_issue_mock_title
import com.mileway.core.ui.resources.tracking_preview_issue_perm_impact
import com.mileway.core.ui.resources.tracking_preview_issue_perm_title
import com.mileway.core.ui.resources.tracking_preview_issue_powersaver_impact
import com.mileway.core.ui.resources.tracking_preview_issue_powersaver_title
import com.mileway.core.ui.resources.tracking_preview_issue_shutdown_impact
import com.mileway.core.ui.resources.tracking_preview_issue_shutdown_title
import com.mileway.core.ui.resources.tracking_preview_issues
import com.mileway.core.ui.resources.tracking_preview_issues_count
import com.mileway.core.ui.resources.tracking_preview_itinerary_id
import com.mileway.core.ui.resources.tracking_preview_last_synced
import com.mileway.core.ui.resources.tracking_preview_max_speed
import com.mileway.core.ui.resources.tracking_preview_mock_removed
import com.mileway.core.ui.resources.tracking_preview_never
import com.mileway.core.ui.resources.tracking_preview_no_events
import com.mileway.core.ui.resources.tracking_preview_not_submitted
import com.mileway.core.ui.resources.tracking_preview_odometer
import com.mileway.core.ui.resources.tracking_preview_odometer_distance
import com.mileway.core.ui.resources.tracking_preview_original_distance
import com.mileway.core.ui.resources.tracking_preview_petty_id
import com.mileway.core.ui.resources.tracking_preview_route_id
import com.mileway.core.ui.resources.tracking_preview_series_accel
import com.mileway.core.ui.resources.tracking_preview_series_altitude
import com.mileway.core.ui.resources.tracking_preview_series_speed
import com.mileway.core.ui.resources.tracking_preview_spike_removed
import com.mileway.core.ui.resources.tracking_preview_start_app_version
import com.mileway.core.ui.resources.tracking_preview_start_device
import com.mileway.core.ui.resources.tracking_preview_start_ocr
import com.mileway.core.ui.resources.tracking_preview_start_reading
import com.mileway.core.ui.resources.tracking_preview_start_time
import com.mileway.core.ui.resources.tracking_preview_submission_time
import com.mileway.core.ui.resources.tracking_preview_subtitle
import com.mileway.core.ui.resources.tracking_preview_sync_interval
import com.mileway.core.ui.resources.tracking_preview_sync_status
import com.mileway.core.ui.resources.tracking_preview_tab_details
import com.mileway.core.ui.resources.tracking_preview_tab_events
import com.mileway.core.ui.resources.tracking_preview_tab_overview
import com.mileway.core.ui.resources.tracking_preview_tab_quality
import com.mileway.core.ui.resources.tracking_preview_telemetry
import com.mileway.core.ui.resources.tracking_preview_tenant
import com.mileway.core.ui.resources.tracking_preview_timestamps
import com.mileway.core.ui.resources.tracking_preview_total_points
import com.mileway.core.ui.resources.tracking_preview_transaction_id
import com.mileway.core.ui.resources.tracking_preview_trip_id
import com.mileway.core.ui.resources.tracking_preview_unsynced_points
import com.mileway.core.ui.resources.tracking_preview_version_device
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.tracking.insights.TelemetryChartData
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.ui.util.HealthLevel
import com.mileway.feature.tracking.ui.util.computeHealthLevel
import com.mileway.feature.tracking.viewmodel.TrackDetailAction
import com.mileway.feature.tracking.viewmodel.TrackDetailViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private fun HealthLevel.color(): Color =
    when (this) {
        HealthLevel.EXCELLENT -> DesignTokens.StatusColors.success
        HealthLevel.GOOD -> DesignTokens.StatusColors.success
        HealthLevel.FAIR -> DesignTokens.StatusColors.warning
        HealthLevel.POOR -> DesignTokens.StatusColors.warning
        HealthLevel.CRITICAL -> DesignTokens.StatusColors.error
    }

private fun HealthLevel.label(): String =
    name.lowercase()
        .replaceFirstChar { it.uppercase() }

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrackDataPreviewScreen(
    routeId: String,
    onBack: () -> Unit,
    viewModel: TrackDetailViewModel = koinViewModel(),
    hardwareEventRepository: HardwareEventRepository = koinInject(),
) {
    val uiState by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val events by hardwareEventRepository.observeEventsForRoute(routeId).collectAsState(initial = emptyList())

    LaunchedEffect(routeId) { viewModel.onAction(TrackDetailAction.Load(routeId)) }

    val tabs =
        listOf(
            stringResource(Res.string.tracking_preview_tab_overview),
            stringResource(Res.string.tracking_preview_tab_quality),
            stringResource(Res.string.tracking_preview_tab_events),
            stringResource(Res.string.tracking_preview_tab_details),
        )
    val tabIcons = listOf(Icons.Default.Preview, Icons.Default.VerifiedUser, Icons.AutoMirrored.Filled.EventNote, Icons.Default.TableChart)
    val pagerState = rememberPagerState { tabs.size }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.tracking_cd_data_preview),
                subtitle = stringResource(Res.string.tracking_preview_subtitle),
                titleIcon = Icons.Default.TableChart,
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.tracking_cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingScreen()
            return@Scaffold
        }

        val track = uiState.rawTrack ?: return@Scaffold
        val displayTrack = uiState.track ?: return@Scaffold

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Sticky tab row
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(tabIcons[index], contentDescription = null) },
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 ->
                        OverviewTab(
                            track = track,
                            displayTrack = displayTrack,
                            locationCount = uiState.locations.size,
                            locations = uiState.locations,
                        )
                    1 -> QualityTab(track = track)
                    2 ->
                        EventsTab(
                            events = events,
                            onExport = {
                                scope.launch { snackbarHostState.showSnackbar("Export: use share button on Hardware Events Log") }
                            },
                        )
                    3 ->
                        DetailsTab(
                            track = track,
                            onCopy = { value ->
                                clipboardManager.setText(AnnotatedString(value))
                                scope.launch { snackbarHostState.showSnackbar("Copied: $value") }
                            },
                        )
                }
            }
        }
    }
}

// ── Tab 1: Overview ──────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    track: SavedTrack,
    displayTrack: com.mileway.core.data.model.display.TrackDisplayData,
    locationCount: Int,
    locations: List<LocationData> = emptyList(),
) {
    val expectedPoints = if (track.duration > 0) (track.duration / track.minimumTrackerTime).toInt().coerceAtLeast(1) else 1
    val actualPoints = locationCount.takeIf { it > 0 } ?: track.totalLocationPoints.toInt()
    val completeness = (actualPoints.toFloat() / expectedPoints).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        // Journey summary
        SectionCard(title = stringResource(Res.string.tracking_insights_journey_summary)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow(stringResource(Res.string.tracking_insights_distance), displayTrack.getFormattedDistance())
                PreviewRow(stringResource(Res.string.tracking_insights_duration), displayTrack.getFormattedDuration())
                PreviewRow(stringResource(Res.string.tracking_detail_avg_speed), "${(track.avgSpeed.coerceAtLeast(0.0) * 10).toLong() / 10.0} km/h")
                PreviewRow(stringResource(Res.string.tracking_preview_max_speed), "${(track.maxSpeed.coerceAtLeast(0.0) * 10).toLong() / 10.0} km/h")
                PreviewRow(stringResource(Res.string.tracking_detail_vehicle), track.selectedVehicleType.ifBlank { "—" })
            }
        }

        // Data completeness
        SectionCard(title = stringResource(Res.string.tracking_preview_data_completeness)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow(stringResource(Res.string.tracking_preview_actual_points), actualPoints.toString())
                PreviewRow(stringResource(Res.string.tracking_preview_expected_points), expectedPoints.toString())
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { completeness },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (completeness > 0.8f) MilewayColors.success else MaterialTheme.colorScheme.error,
                )
                Text(
                    stringResource(Res.string.tracking_preview_completeness_pct, (completeness * 100).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Distance breakdown
        SectionCard(title = stringResource(Res.string.tracking_preview_distance_breakdown)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow(stringResource(Res.string.tracking_preview_original_distance), "${(track.originalDistance / 1000 * 1000).toLong() / 1000.0} km")
                if (track.mockDistance > 0) {
                    PreviewRow(
                        stringResource(Res.string.tracking_preview_mock_removed),
                        "-${(track.mockDistance / 1000 * 1000).toLong() / 1000.0} km",
                        valueColor = MaterialTheme.colorScheme.error,
                    )
                }
                if (track.abnormalDistance > 0) {
                    PreviewRow(
                        stringResource(Res.string.tracking_preview_abnormal_removed),
                        "-${(track.abnormalDistance / 1000 * 1000).toLong() / 1000.0} km",
                        valueColor = MaterialTheme.colorScheme.error,
                    )
                }
                if (track.spikeDistance > 0) {
                    PreviewRow(
                        stringResource(Res.string.tracking_preview_spike_removed),
                        "-${(track.spikeDistance / 1000 * 1000).toLong() / 1000.0} km",
                        valueColor = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                PreviewRow(
                    stringResource(Res.string.tracking_preview_clean_distance),
                    "${(track.cleanedDistance.let { if (it > 0) it / 1000.0 else track.distance / 1000.0 } * 1000).toLong() / 1000.0} km",
                    labelStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        // Wave 3 live-map polish: telemetry graphs derived from the raw LocationData points.
        TelemetryGraphsCard(locations = locations)

        Spacer(Modifier.height(DesignTokens.Spacing.l))
    }
}

/** Small speed/altitude/accel sparklines from the track's raw fixes. Hidden when there's no data. */
@Composable
private fun TelemetryGraphsCard(locations: List<LocationData>) {
    val series = remember(locations) { TelemetryChartData.derive(locations) }
    if (series.speedKmh.isEmpty && series.altitudeM.isEmpty && series.accelMagnitude.isEmpty) return

    SectionCard(title = stringResource(Res.string.tracking_preview_telemetry)) {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            TelemetrySeriesRow(stringResource(Res.string.tracking_preview_series_speed), series.speedKmh, MilewayColors.info)
            TelemetrySeriesRow(stringResource(Res.string.tracking_preview_series_altitude), series.altitudeM, MilewayColors.success)
            TelemetrySeriesRow(stringResource(Res.string.tracking_preview_series_accel), series.accelMagnitude, MilewayColors.warning)
        }
    }
}

@Composable
private fun TelemetrySeriesRow(
    label: String,
    series: TelemetryChartData.Series,
    color: Color,
) {
    if (series.isEmpty) return
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${series.min.toInt()} – ${series.max.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        SparklineChart(values = series.values, color = color)
    }
}

// ── Tab 2: Quality ───────────────────────────────────────────────────────────

private data class IssueRow(val icon: ImageVector, val title: String, val impact: String, val severity: IssueSeverity)

private enum class IssueSeverity { LOW, MEDIUM, HIGH, CRITICAL }

private fun IssueSeverity.color(): Color =
    when (this) {
        IssueSeverity.LOW -> DesignTokens.StatusColors.warning
        IssueSeverity.MEDIUM -> DesignTokens.StatusColors.warning
        IssueSeverity.HIGH -> DesignTokens.StatusColors.error
        IssueSeverity.CRITICAL -> DesignTokens.StatusColors.error
    }

@Composable
private fun QualityTab(track: SavedTrack) {
    val health = computeHealthLevel(track)

    val issues =
        buildList {
            if (track.wasMockLocationUsed) {
                add(
                    IssueRow(
                        Icons.Default.GpsOff,
                        stringResource(Res.string.tracking_preview_issue_mock_title),
                        stringResource(Res.string.tracking_preview_issue_mock_impact),
                        IssueSeverity.CRITICAL,
                    ),
                )
            }
            if (track.wasPermissionsViolated) {
                add(
                    IssueRow(
                        Icons.Default.Block,
                        stringResource(Res.string.tracking_preview_issue_perm_title),
                        stringResource(Res.string.tracking_preview_issue_perm_impact),
                        IssueSeverity.HIGH,
                    ),
                )
            }
            if (track.wasPhoneShutDown) {
                add(
                    IssueRow(
                        Icons.Default.PowerOff,
                        stringResource(Res.string.tracking_preview_issue_shutdown_title),
                        stringResource(Res.string.tracking_preview_issue_shutdown_impact),
                        IssueSeverity.HIGH,
                    ),
                )
            }
            if (track.wasAppKilled) {
                add(
                    IssueRow(
                        Icons.Default.ScreenLockPortrait,
                        stringResource(Res.string.tracking_preview_issue_killed_title),
                        stringResource(Res.string.tracking_preview_issue_killed_impact, track.appKilledCount),
                        IssueSeverity.MEDIUM,
                    ),
                )
            }
            if (track.wasBatteryOptimizationEnabled) {
                add(
                    IssueRow(
                        Icons.Default.BatteryAlert,
                        stringResource(Res.string.tracking_preview_issue_battery_title),
                        stringResource(Res.string.tracking_preview_issue_battery_impact),
                        IssueSeverity.MEDIUM,
                    ),
                )
            }
            if (track.wasPowerSaverEnabled) {
                add(
                    IssueRow(
                        Icons.Default.PhoneLocked,
                        stringResource(Res.string.tracking_preview_issue_powersaver_title),
                        stringResource(Res.string.tracking_preview_issue_powersaver_impact),
                        IssueSeverity.LOW,
                    ),
                )
            }
        }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        // Health badge card
        SectionCard(title = stringResource(Res.string.tracking_preview_health_assessment)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(health.color(), DesignTokens.Shape.roundedSm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        health.label().first().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(DesignTokens.Spacing.m))
                Column {
                    Text(health.label(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = health.color())
                    Text(
                        when (health) {
                            HealthLevel.EXCELLENT -> stringResource(Res.string.tracking_preview_health_excellent)
                            HealthLevel.GOOD -> stringResource(Res.string.tracking_preview_health_good)
                            HealthLevel.FAIR -> stringResource(Res.string.tracking_preview_health_fair)
                            HealthLevel.POOR -> stringResource(Res.string.tracking_preview_health_poor)
                            HealthLevel.CRITICAL -> stringResource(Res.string.tracking_preview_health_critical)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Issues
        if (issues.isNotEmpty()) {
            SectionCard(title = stringResource(Res.string.tracking_preview_issues_count, issues.size)) {
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    issues.forEach { issue ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(issue.icon, contentDescription = null, tint = issue.severity.color(), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(DesignTokens.Spacing.m))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(issue.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(issue.impact, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .wrapContentWidth()
                                        .background(issue.severity.color(), DesignTokens.Shape.button)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(issue.severity.name, style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            SectionCard(title = stringResource(Res.string.tracking_preview_issues)) {
                Text(
                    stringResource(Res.string.tracking_preview_health_excellent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Sync status
        SectionCard(title = stringResource(Res.string.tracking_preview_sync_status)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow(stringResource(Res.string.tracking_preview_total_points), track.totalLocationPoints.toString())
                PreviewRow(stringResource(Res.string.tracking_preview_unsynced_points), track.unsyncedLocationPoints.toString())
                PreviewRow(
                    stringResource(Res.string.tracking_preview_last_synced),
                    if (track.lastSyncedTime > 0) DateUtils.epochToDisplayDate(track.lastSyncedTime) else stringResource(Res.string.tracking_preview_never),
                )
                PreviewRow(stringResource(Res.string.tracking_preview_sync_interval), "${track.syncIntervalTime / 1000}s")
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))
    }
}

// ── Tab 3: Events ─────────────────────────────────────────────────────────────

@Composable
private fun EventsTab(
    events: List<HardwareEvent>,
    onExport: () -> Unit,
) {
    val grouped = events.groupBy { it.audience }
    val audienceOrder = listOf(EventAudience.USER, EventAudience.SUMMARY, EventAudience.SUPPORT, EventAudience.DEBUG, EventAudience.UNKNOWN)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        if (events.isEmpty()) {
            Text(
                stringResource(Res.string.tracking_preview_no_events),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            audienceOrder.forEach { audience ->
                val bucket = grouped[audience] ?: return@forEach
                CollapsibleSectionCard(
                    title = "${audience.name.lowercase().replaceFirstChar { it.uppercase() }} (${bucket.size})",
                    initiallyExpanded = audience == EventAudience.USER,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                        bucket.forEach { event ->
                            Column {
                                Text(
                                    DateUtils.epochToDisplayDate(event.time),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(event.event, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(DesignTokens.Spacing.l))
    }
}

// ── Tab 4: Details ──────────────────────────────────────────────────────────

@Composable
private fun DetailsTab(
    track: SavedTrack,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        CollapsibleSectionCard(title = stringResource(Res.string.tracking_preview_timestamps), initiallyExpanded = true) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow(stringResource(Res.string.tracking_preview_start_time), DateUtils.epochToDisplayDate(track.startTime), onCopy)
                CopyableRow(
                    stringResource(Res.string.tracking_preview_end_time),
                    if (track.endTime > 0) DateUtils.epochToDisplayDate(track.endTime) else "—",
                    onCopy,
                )
                CopyableRow(
                    stringResource(Res.string.tracking_preview_submission_time),
                    if (track.submissionTime > 0) {
                        DateUtils.epochToDisplayDate(
                            track.submissionTime,
                        )
                    } else {
                        stringResource(Res.string.tracking_preview_not_submitted)
                    },
                    onCopy,
                )
                CopyableRow(stringResource(Res.string.tracking_preview_created_at), DateUtils.epochToDisplayDate(track.createdAt), onCopy)
            }
        }

        CollapsibleSectionCard(title = stringResource(Res.string.tracking_preview_account_binding)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow(stringResource(Res.string.tracking_preview_employee_code), track.startedByEmployeeCode.ifBlank { "—" }, onCopy, showStatus = true)
                CopyableRow(stringResource(Res.string.tracking_preview_tenant), track.startedByTenant.ifBlank { "—" }, onCopy, showStatus = true)
                CopyableRow(stringResource(Res.string.tracking_preview_email), track.startedByAccountEmail.ifBlank { "—" }, onCopy, showStatus = true)
                CopyableRow(stringResource(Res.string.tracking_preview_route_id), track.routeId, onCopy, showStatus = true)
            }
        }

        // G8: the odometer section is the reviewer's canonical tri-state example — each reading is
        // green when captured, red when missing, on this completed-journey preview.
        CollapsibleSectionCard(title = stringResource(Res.string.tracking_preview_odometer)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow(stringResource(Res.string.tracking_preview_start_reading), track.odometerStartReading.ifBlank { "—" }, onCopy, showStatus = true)
                CopyableRow(stringResource(Res.string.tracking_preview_end_reading), track.odometerEndReading.ifBlank { "—" }, onCopy, showStatus = true)
                CopyableRow(stringResource(Res.string.tracking_preview_start_ocr), track.odometerStartOcr.ifBlank { "—" }, onCopy, showStatus = true)
                CopyableRow(stringResource(Res.string.tracking_preview_end_ocr), track.odometerEndOcr.ifBlank { "—" }, onCopy, showStatus = true)
                CopyableRow(
                    stringResource(Res.string.tracking_preview_odometer_distance),
                    if (track.odometerDistance > 0) "${(track.odometerDistance / 1000 * 1000).toLong() / 1000.0} km" else "—",
                    onCopy,
                    showStatus = true,
                )
            }
        }

        CollapsibleSectionCard(title = stringResource(Res.string.tracking_preview_context)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow(stringResource(Res.string.tracking_preview_trip_id), track.tripId ?: "—", onCopy, showStatus = true)
                CopyableRow(
                    stringResource(Res.string.tracking_preview_petty_id),
                    if (track.pettyId >= 0) track.pettyId.toString() else "—",
                    onCopy,
                    showStatus = true,
                )
                CopyableRow(stringResource(Res.string.tracking_preview_itinerary_id), track.itineraryId ?: "—", onCopy, showStatus = true)
                CopyableRow(stringResource(Res.string.tracking_preview_transaction_id), track.transId ?: "—", onCopy, showStatus = true)
            }
        }

        CollapsibleSectionCard(title = stringResource(Res.string.tracking_preview_version_device)) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow(stringResource(Res.string.tracking_preview_start_app_version), track.startAppVersion, onCopy)
                CopyableRow(stringResource(Res.string.tracking_preview_end_app_version), track.endAppVersion, onCopy)
                CopyableRow(stringResource(Res.string.tracking_preview_start_device), track.startDeviceVersion, onCopy)
                CopyableRow(stringResource(Res.string.tracking_preview_end_device), track.endDeviceVersion, onCopy)
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))
    }
}

// ── Shared components ────────────────────────────────────────────────────────

@Composable
private fun PreviewRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = labelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

/**
 * G8: per-field value status, distinct from the [HealthLevel] / [IssueSeverity] severity model.
 * A field is [SET] (captured) or [MISSING] (expected but absent) on this completed-journey preview;
 * [PENDING] (gray) is reserved for the in-progress capture flow where a field is not yet applicable,
 * so this finished screen only ever shows green/red — per the reviewer's field-clarity rule.
 */
private enum class FieldStatus { SET, MISSING, PENDING }

@Composable
private fun FieldStatus.color(): Color =
    when (this) {
        FieldStatus.SET -> DesignTokens.StatusColors.success
        FieldStatus.MISSING -> DesignTokens.StatusColors.error
        FieldStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/** Derives [FieldStatus] from a display string using the screen's "—" missing-value convention. */
private fun fieldStatusOf(value: String): FieldStatus = if (value.isBlank() || value == "—") FieldStatus.MISSING else FieldStatus.SET

@Composable
private fun CopyableRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit,
    showStatus: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .pointerInput(value) { detectTapGestures(onLongPress = { onCopy(value) }) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        if (showStatus) {
            // G8: leading tri-state dot — green = captured, red = missing.
            Box(
                modifier =
                    Modifier
                        .padding(top = 3.dp, end = DesignTokens.Spacing.s)
                        .size(8.dp)
                        .background(fieldStatusOf(value).color(), DesignTokens.Shape.button),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}
