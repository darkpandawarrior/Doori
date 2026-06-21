@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.miletracker.feature.tracking.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.PhoneLocked
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.ScreenLockPortrait
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miletracker.core.data.model.db.EventAudience
import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.data.util.DateUtils
import com.miletracker.core.ui.components.CollapsibleSectionCard
import com.miletracker.core.ui.components.LoadingScreen
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.feature.tracking.repository.HardwareEventRepository
import com.miletracker.feature.tracking.viewmodel.TrackDetailAction
import com.miletracker.feature.tracking.viewmodel.TrackDetailViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// ── Health-level domain ──────────────────────────────────────────────────────

enum class HealthLevel { EXCELLENT, GOOD, FAIR, POOR, CRITICAL }

fun computeHealthLevel(track: SavedTrack): HealthLevel {
    var score = 100
    if (track.wasMockLocationUsed) score -= 40
    if (track.wasBatteryOptimizationEnabled) score -= 10
    if (track.wasPowerSaverEnabled) score -= 10
    if (track.wasAppKilled) score -= 15
    if (track.wasPhoneShutDown) score -= 20
    if (track.wasPermissionsViolated) score -= 30
    return when {
        score >= 90 -> HealthLevel.EXCELLENT
        score >= 70 -> HealthLevel.GOOD
        score >= 50 -> HealthLevel.FAIR
        score >= 30 -> HealthLevel.POOR
        else -> HealthLevel.CRITICAL
    }
}

private fun HealthLevel.color(): Color =
    when (this) {
        HealthLevel.EXCELLENT -> Color(0xFF2E7D32)
        HealthLevel.GOOD -> Color(0xFF558B2F)
        HealthLevel.FAIR -> Color(0xFFF9A825)
        HealthLevel.POOR -> Color(0xFFE65100)
        HealthLevel.CRITICAL -> Color(0xFFC62828)
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val events by hardwareEventRepository.observeEventsForRoute(routeId).collectAsState(initial = emptyList())

    LaunchedEffect(routeId) { viewModel.onAction(TrackDetailAction.Load(routeId)) }

    val tabs = listOf("Overview", "Quality", "Events", "Details")
    val pagerState = rememberPagerState { tabs.size }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DepthAwareTopBar(
                title = "Data Preview",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> OverviewTab(track = track, displayTrack = displayTrack, locationCount = uiState.locations.size)
                    1 -> QualityTab(track = track)
                    2 ->
                        EventsTab(
                            events = events,
                            onExport = {
                                scope.launch { snackbarHostState.showSnackbar("Export — use share button on Hardware Events Log") }
                            },
                        )
                    3 ->
                        DetailsTab(
                            track = track,
                            onCopy = { value ->
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("field", value))
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
    displayTrack: com.miletracker.core.data.model.display.TrackDisplayData,
    locationCount: Int,
) {
    val expectedPoints = if (track.duration > 0) (track.duration / track.minimumTrackerTime).toInt().coerceAtLeast(1) else 1
    val actualPoints = locationCount.takeIf { it > 0 } ?: track.totalLocationPoints.toInt()
    val completeness = (actualPoints.toFloat() / expectedPoints).coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        // Journey summary
        SectionCard(title = "Journey Summary") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow("Distance", displayTrack.getFormattedDistance())
                PreviewRow("Duration", displayTrack.getFormattedDuration())
                PreviewRow("Avg speed", "%.1f km/h".format(track.avgSpeed.coerceAtLeast(0.0)))
                PreviewRow("Max speed", "%.1f km/h".format(track.maxSpeed.coerceAtLeast(0.0)))
                PreviewRow("Vehicle", track.selectedVehicleType.ifBlank { "—" })
            }
        }

        // Data completeness
        SectionCard(title = "Data Completeness") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow("Actual points", actualPoints.toString())
                PreviewRow("Expected points", expectedPoints.toString())
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { completeness },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (completeness > 0.8f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                )
                Text(
                    "%.0f%% completeness".format(completeness * 100),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Distance breakdown
        SectionCard(title = "Distance Breakdown") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow("Original distance", "%.3f km".format(track.originalDistance / 1000))
                if (track.mockDistance > 0) {
                    PreviewRow("Mock removed", "-%.3f km".format(track.mockDistance / 1000), valueColor = MaterialTheme.colorScheme.error)
                }
                if (track.abnormalDistance > 0) {
                    PreviewRow("Abnormal removed", "-%.3f km".format(track.abnormalDistance / 1000), valueColor = MaterialTheme.colorScheme.error)
                }
                if (track.spikeDistance > 0) {
                    PreviewRow("Spike removed", "-%.3f km".format(track.spikeDistance / 1000), valueColor = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                PreviewRow(
                    "Clean distance",
                    "%.3f km".format(track.cleanedDistance.let { if (it > 0) it / 1000.0 else track.distance / 1000.0 }),
                    labelStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))
    }
}

// ── Tab 2: Quality ───────────────────────────────────────────────────────────

private data class IssueRow(val icon: ImageVector, val title: String, val impact: String, val severity: IssueSeverity)

private enum class IssueSeverity { LOW, MEDIUM, HIGH, CRITICAL }

private fun IssueSeverity.color(): Color =
    when (this) {
        IssueSeverity.LOW -> Color(0xFFF9A825)
        IssueSeverity.MEDIUM -> Color(0xFFE65100)
        IssueSeverity.HIGH -> Color(0xFFC62828)
        IssueSeverity.CRITICAL -> Color(0xFFB71C1C)
    }

@Composable
private fun QualityTab(track: SavedTrack) {
    val health = computeHealthLevel(track)

    val issues =
        buildList {
            if (track.wasMockLocationUsed) {
                add(
                    IssueRow(Icons.Default.GpsOff, "Mock Location Was Used", "Distance accuracy severely degraded", IssueSeverity.CRITICAL),
                )
            }
            if (track.wasPermissionsViolated) add(IssueRow(Icons.Default.Block, "Permissions Were Violated", "GPS data may be missing", IssueSeverity.HIGH))
            if (track.wasPhoneShutDown) {
                add(
                    IssueRow(Icons.Default.PowerOff, "Phone Was Shut Down", "Tracking gap — distance may be under-reported", IssueSeverity.HIGH),
                )
            }
            if (track.wasAppKilled) {
                add(
                    IssueRow(
                        Icons.Default.ScreenLockPortrait,
                        "App Was Force-Killed",
                        "Background tracking interrupted (${track.appKilledCount}×)",
                        IssueSeverity.MEDIUM,
                    ),
                )
            }
            if (track.wasBatteryOptimizationEnabled) {
                add(
                    IssueRow(Icons.Default.BatteryAlert, "Battery Optimization Was ON", "Distance may be under-reported", IssueSeverity.MEDIUM),
                )
            }
            if (track.wasPowerSaverEnabled) {
                add(
                    IssueRow(Icons.Default.PhoneLocked, "Power Saver Was ON", "GPS sampling reduced — distance affected", IssueSeverity.LOW),
                )
            }
        }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        // Health badge card
        SectionCard(title = "Health Assessment") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(health.color(), RoundedCornerShape(12.dp)),
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
                            HealthLevel.EXCELLENT -> "No issues detected"
                            HealthLevel.GOOD -> "Minor issues — data reliable"
                            HealthLevel.FAIR -> "Some issues — review recommended"
                            HealthLevel.POOR -> "Significant issues — distance may vary"
                            HealthLevel.CRITICAL -> "Critical issues — data unreliable"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Issues
        if (issues.isNotEmpty()) {
            SectionCard(title = "Issues (${issues.size})") {
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
                                        .background(issue.severity.color(), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(issue.severity.name, style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        } else {
            SectionCard(title = "Issues") {
                Text("No issues detected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Sync status
        SectionCard(title = "Sync Status") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                PreviewRow("Total location points", track.totalLocationPoints.toString())
                PreviewRow("Unsynced points", track.unsyncedLocationPoints.toString())
                PreviewRow(
                    "Last synced",
                    if (track.lastSyncedTime > 0) DateUtils.epochToDisplayDate(track.lastSyncedTime) else "Never",
                )
                PreviewRow("Sync interval", "${track.syncIntervalTime / 1000}s")
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
            Text("No hardware events recorded", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        CollapsibleSectionCard(title = "Timestamps", initiallyExpanded = true) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow("Start time", DateUtils.epochToDisplayDate(track.startTime), onCopy)
                CopyableRow("End time", if (track.endTime > 0) DateUtils.epochToDisplayDate(track.endTime) else "—", onCopy)
                CopyableRow("Submission time", if (track.submissionTime > 0) DateUtils.epochToDisplayDate(track.submissionTime) else "Not submitted", onCopy)
                CopyableRow("Created at", DateUtils.epochToDisplayDate(track.createdAt), onCopy)
            }
        }

        CollapsibleSectionCard(title = "Account Binding") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow("Employee code", track.startedByEmployeeCode.ifBlank { "—" }, onCopy)
                CopyableRow("Tenant", track.startedByTenant.ifBlank { "—" }, onCopy)
                CopyableRow("Email", track.startedByAccountEmail.ifBlank { "—" }, onCopy)
                CopyableRow("Route ID", track.routeId, onCopy)
            }
        }

        CollapsibleSectionCard(title = "Odometer") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow("Start reading", track.odometerStartReading.ifBlank { "—" }, onCopy)
                CopyableRow("End reading", track.odometerEndReading.ifBlank { "—" }, onCopy)
                CopyableRow("Start OCR", track.odometerStartOcr.ifBlank { "—" }, onCopy)
                CopyableRow("End OCR", track.odometerEndOcr.ifBlank { "—" }, onCopy)
                CopyableRow("Odometer distance", if (track.odometerDistance > 0) "%.3f km".format(track.odometerDistance / 1000) else "—", onCopy)
            }
        }

        CollapsibleSectionCard(title = "Context") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow("Trip ID", track.tripId ?: "—", onCopy)
                CopyableRow("Petty cash ID", if (track.pettyId >= 0) track.pettyId.toString() else "—", onCopy)
                CopyableRow("Itinerary ID", track.itineraryId ?: "—", onCopy)
                CopyableRow("Transaction ID", track.transId ?: "—", onCopy)
            }
        }

        CollapsibleSectionCard(title = "Version & Device") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                CopyableRow("Start app version", track.startAppVersion, onCopy)
                CopyableRow("End app version", track.endAppVersion, onCopy)
                CopyableRow("Start device", track.startDeviceVersion, onCopy)
                CopyableRow("End device", track.endDeviceVersion, onCopy)
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

@Composable
private fun CopyableRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .pointerInput(value) { detectTapGestures(onLongPress = { onCopy(value) }) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
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
