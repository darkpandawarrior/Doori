package com.mileway.feature.tracking.ui.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.core.maps.MapCoordinate
import com.mileway.core.maps.MapSurface
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.tracking.CompactSystemStatusIndicator
import com.mileway.core.ui.components.tracking.StatusChip
import com.mileway.core.ui.components.tracking.StatusLevel
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.tracking.viewmodel.TrackMilesPhase
import com.mileway.feature.tracking.viewmodel.TrackSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Everything [LiveDriveScreen] needs to render, decoupled from [TrackMilesUiState] so this file
 * has no compile dependency on the screen/viewmodel it will be dropped into — a later agent maps
 * the real VM state onto this each recomposition.
 */
data class LiveDriveState(
    val phase: TrackMilesPhase,
    val distanceKm: Double,
    val elapsedMs: Long,
    val speedKmh: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val pointsCount: Long,
    val qualityScore: Int,
    val batteryPct: Int,
    val isCharging: Boolean,
    val unsyncedPoints: Long,
    val pauseReason: String?,
    val currentLat: Double,
    val currentLng: Double,
    val bearingDegrees: Float,
    val signal: TrackSignal,
    val systemFlags: TrackingSystemFlags,
    /** No dedicated flag exists upstream yet for "no map tiles available" — defaults closed. */
    val isOffline: Boolean = false,
    val routeCoords: List<MapCoordinate> = emptyList(),
)

data class LiveDriveActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onStopConfirmed: () -> Unit,
    val onFlag: () -> Unit,
)

/** A gap this long between fix updates counts as "recovered after a gap", not routine jitter. */
private const val RECOVERED_GAP_THRESHOLD_MS = 8_000L

/** How long the "GPS signal recovered" banner stays up before clearing itself. */
private const val RECOVERED_BANNER_MS = 4_000L

private val StatusStripHeight = 44.dp
private val ControlBarHeight = 96.dp
private val PauseFlagButtonSize = 56.dp
private val StopButtonSize = 88.dp

/**
 * The hero live-tracking surface: map as background, a floating distance/elapsed/speed slab, and a
 * pinned pause/stop/flag control bar. Deliberately not a [com.mileway.core.ui.detail.DetailSpec] —
 * this screen's state moves at 1-4 Hz and a spec's `DetailField.visible` is evaluated once at
 * build time, which is exactly wrong here.
 */
@Composable
fun LiveDriveScreen(
    state: LiveDriveState,
    actions: LiveDriveActions,
    modifier: Modifier = Modifier,
    mapSurface: MapSurface = koinInject(),
) {
    val isPaused = state.phase == TrackMilesPhase.PAUSED
    val isTrackingOrPaused = state.phase == TrackMilesPhase.TRACKING || isPaused

    // Sentinel default from TrackMilesUiState — (0,0) means "no fix yet", same convention as
    // upstream (currentLat/currentLng default to 0.0 until the first location callback lands).
    val hasFix = state.currentLat != 0.0 || state.currentLng != 0.0

    // ── Head-position interpolation: arrives exactly when the next fix is due ──────────
    val latAnim = remember { Animatable(state.currentLat.toFloat()) }
    val lngAnim = remember { Animatable(state.currentLng.toFloat()) }
    var lastFixAtMs by remember { mutableStateOf(nowMs()) }
    var recoveredAfterGap by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentLat, state.currentLng) {
        val now = nowMs()
        val gap = now - lastFixAtMs
        if (gap >= RECOVERED_GAP_THRESHOLD_MS) recoveredAfterGap = true
        lastFixAtMs = now
        val durationMs = interpolationDurationMs(gap).toInt()
        launch { latAnim.animateTo(state.currentLat.toFloat(), tween(durationMs, easing = LinearEasing)) }
        launch { lngAnim.animateTo(state.currentLng.toFloat(), tween(durationMs, easing = LinearEasing)) }
    }
    LaunchedEffect(recoveredAfterGap) {
        if (recoveredAfterGap) {
            delay(RECOVERED_BANNER_MS)
            recoveredAfterGap = false
        }
    }

    // ── Bearing: frozen below walking-adjacent speed, GPS course-over-ground is noise there ──
    var lastValidBearing by remember { mutableStateOf(state.bearingDegrees) }
    LaunchedEffect(state.bearingDegrees, state.speedKmh) {
        if (state.speedKmh >= BEARING_FREEZE_THRESHOLD_KMH) lastValidBearing = state.bearingDegrees
    }
    val bearingDisplay = deriveBearingDisplay(state.speedKmh, state.bearingDegrees, lastValidBearing)

    // Zoom-for-speed ([zoomForSpeed], animated over an 800ms tween per spec) is ready to wire in
    // once MapSurface exposes a zoom parameter — see the ponytail note on [zoomForSpeed] itself.

    // ── Camera mode ──────────────────────────────────────────────────────────────────
    var cameraMode by remember { mutableStateOf(CameraMode.FOLLOW) }
    var lastCameraInteractionAt by remember { mutableStateOf(0L) }
    LaunchedEffect(state.phase) {
        cameraMode =
            when (state.phase) {
                TrackMilesPhase.PAUSED, TrackMilesPhase.STOPPED -> cameraMode.reduce(CameraEvent.TripPausedOrStopped)
                TrackMilesPhase.TRACKING -> cameraMode.reduce(CameraEvent.TripTracking)
                else -> cameraMode
            }
    }
    LaunchedEffect(cameraMode, lastCameraInteractionAt) {
        if (cameraMode != CameraMode.FOLLOW && isTrackingOrPaused) {
            delay(CAMERA_IDLE_TIMEOUT_MS)
            cameraMode = cameraMode.reduce(CameraEvent.IdleTimeout)
        }
    }

    val degradedState =
        deriveDegradedState(
            DegradedInputs(
                hasFix = hasFix,
                signal = state.signal,
                flags = state.systemFlags,
                isOffline = state.isOffline,
                recoveredAfterGap = recoveredAfterGap,
            ),
        )

    Box(modifier = modifier) {
        // Map fills everything — the background, not a card.
        mapSurface.LiveTrackMap(
            routeCoords = state.routeCoords,
            filteredCoords = emptyList(),
            abnormalCoords = emptyList(),
            startCoord = state.routeCoords.firstOrNull(),
            endCoord = null,
            currentLat = latAnim.value.toDouble(),
            currentLng = lngAnim.value.toDouble(),
            bearing = bearingDisplay.degrees,
            autoCenterEnabled = cameraMode == CameraMode.FOLLOW,
            playbackCoord = null,
            showIssueMarkers = false,
            // Offline degraded state maps straight onto MapSurface's own bundled-tiles switch —
            // the map keeps rendering (from the offline pack) instead of going blank.
            offlineTiles = state.isOffline,
            modifier =
                Modifier
                    .fillMaxSize()
                    // ponytail: MapSurface exposes no tap/pan callback of its own, so this is the
                    // only place to detect the "tap to cycle camera" gesture the spec asks for.
                    // It necessarily sits over the map's own gesture layer — fine for a drop-in
                    // screen; revisit once MapSurface grows a real onTap/onUserPan hook.
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                cameraMode = cameraMode.reduce(CameraEvent.Tap)
                                lastCameraInteractionAt = nowMs()
                            },
                        )
                    },
        )

        StatusStrip(
            state = state,
            hasFix = hasFix,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        degradedState.message()?.let { message ->
            DegradedBanner(
                message = message,
                tone = degradedState.toTone(),
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = StatusStripHeight),
            )
        }

        MetricSlab(
            state = state,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = DesignTokens.Spacing.l),
        )

        if (cameraMode == CameraMode.FREE) {
            RecenterChip(
                onClick = {
                    cameraMode = CameraMode.FOLLOW
                    lastCameraInteractionAt = nowMs()
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = DesignTokens.Spacing.l, bottom = ControlBarHeight + DesignTokens.Spacing.l),
            )
        }

        ControlBar(
            isPaused = isPaused,
            onPauseResume = { if (isPaused) actions.onResume() else actions.onPause() },
            onStopCommitted = actions.onStopConfirmed,
            onFlag = actions.onFlag,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(ControlBarHeight),
        )
    }
}

private fun nowMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

private fun DegradedState.toTone(): StatusTone =
    when (this) {
        DegradedState.NONE -> StatusTone.Neutral
        DegradedState.PERMISSION_REVOKED, DegradedState.NO_FIX -> StatusTone.Error
        DegradedState.OFFLINE, DegradedState.POOR_ACCURACY, DegradedState.BATTERY_SAVER -> StatusTone.Warning
        DegradedState.RECOVERED_AFTER_GAP -> StatusTone.Success
    }

// ── Status strip ─────────────────────────────────────────────────────────────────────

@Composable
private fun StatusStrip(
    state: LiveDriveState,
    hasFix: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(StatusStripHeight)
                .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        CompactSystemStatusIndicator(
            chips = statusChipsFor(state, hasFix),
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l),
        )
    }
}

private fun statusChipsFor(
    state: LiveDriveState,
    hasFix: Boolean,
): List<StatusChip> =
    buildList {
        add(
            StatusChip(
                icon = if (hasFix) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                label = if (hasFix) state.signal.name else "No fix",
                level = if (!hasFix) StatusLevel.BAD else state.signal.toStatusLevel(),
            ),
        )
        add(
            StatusChip(
                icon = Icons.Filled.Insights,
                label = "${state.qualityScore}%",
                level = state.qualityScore.toQualityLevel(),
            ),
        )
        if (state.systemFlags.powerSaverOn || state.systemFlags.batteryOptimized) {
            add(StatusChip(Icons.Filled.BatterySaver, "Saver", StatusLevel.WARN))
        }
        if (state.unsyncedPoints > 0) {
            add(StatusChip(Icons.Filled.CloudQueue, "${state.unsyncedPoints}", StatusLevel.WARN))
        }
    }

private fun TrackSignal.toStatusLevel(): StatusLevel =
    when (this) {
        TrackSignal.GOOD -> StatusLevel.OK
        TrackSignal.FAIR -> StatusLevel.WARN
        TrackSignal.POOR -> StatusLevel.BAD
    }

private fun Int.toQualityLevel(): StatusLevel =
    when {
        this >= 80 -> StatusLevel.OK
        this >= 50 -> StatusLevel.WARN
        else -> StatusLevel.BAD
    }

// ── Degraded-state banner ───────────────────────────────────────────────────────────

@Composable
private fun DegradedBanner(
    message: DegradedMessage,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = listOfNotNull(message.title, message.action).joinToString(". ") },
        color = tone.color.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Column {
                Text(message.title, style = MaterialTheme.typography.labelMedium, color = Color.White)
                message.action?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

// ── Metric slab ──────────────────────────────────────────────────────────────────────

@Composable
private fun MetricSlab(
    state: LiveDriveState,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -12f) expanded = true
                        if (dragAmount > 12f) expanded = false
                    }
                },
        shape = DesignTokens.Shape.roundedLg,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = DesignTokens.Elevation.raised,
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatKm2(state.distanceKm),
                style = MaterialTheme.typography.headlineLarge.dataStyle(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Distance ${formatKm2(state.distanceKm)} kilometres"
                    },
            )
            Text(
                text = formatElapsed(state.elapsedMs),
                style = MaterialTheme.typography.titleMedium.dataStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatSpeed1(state.speedKmh)} km/h",
                style = MaterialTheme.typography.bodyMedium.dataStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = if (expanded) "Collapse trip stats" else "Expand trip stats",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(top = DesignTokens.Spacing.xs)
                        .rotate(if (expanded) 180f else 0f)
                        .clip(CircleShape)
                        .padding(4.dp),
            )

            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier.padding(top = DesignTokens.Spacing.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ExpansionRow("Avg speed", "${formatSpeed1(state.avgSpeedKmh)} km/h")
                    ExpansionRow("Max speed", "${formatSpeed1(state.maxSpeedKmh)} km/h")
                    ExpansionRow("Points", "${state.pointsCount}")
                    ExpansionRow("Quality", "${state.qualityScore}%")
                    ExpansionRow(
                        "Battery",
                        "${state.batteryPct}%" + if (state.isCharging) " (charging)" else "",
                    )
                    state.pauseReason?.let { ExpansionRow("Pause reason", it) }
                }
            }
        }
    }
}

@Composable
private fun ExpansionRow(
    label: String,
    value: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(88.dp))
        Text(value, style = MaterialTheme.typography.labelSmall.dataStyle(), color = MaterialTheme.colorScheme.onSurface)
    }
}

// ── Recenter chip (FREE camera mode) ────────────────────────────────────────────────

@Composable
private fun RecenterChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = DesignTokens.Elevation.raised,
    ) {
        Row(
            modifier =
                Modifier
                    .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
                    .padding(horizontal = DesignTokens.Spacing.m, vertical = DesignTokens.Spacing.s)
                    .semantics { contentDescription = "Recenter map" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            Text("Recenter", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

// ── Control bar ──────────────────────────────────────────────────────────────────────

@Composable
private fun ControlBar(
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onStopCommitted: () -> Unit,
    onFlag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.background(Color.Black.copy(alpha = 0.35f)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = if (isPaused) "Resume tracking" else "Pause tracking",
            size = PauseFlagButtonSize,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onPauseResume,
        )

        HoldToStopButton(
            onCommitted = onStopCommitted,
            modifier = Modifier.size(StopButtonSize),
        )

        RoundIconButton(
            icon = Icons.Filled.Flag,
            contentDescription = "Check in now",
            size = PauseFlagButtonSize,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onFlag,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier =
            Modifier
                .size(size.coerceAtLeastTouchTarget())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                    )
                }
                .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = containerColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.4f))
        }
    }
}

private fun androidx.compose.ui.unit.Dp.coerceAtLeastTouchTarget(): androidx.compose.ui.unit.Dp =
    if (this < DesignTokens.IconSize.minTouchTarget) DesignTokens.IconSize.minTouchTarget else this

/**
 * STOP: press-and-hold [HOLD_DURATION_MS] with a radial fill, haptic exactly on commit (not on
 * release). Releasing before the ring completes cancels — an accidental tap must never stop a
 * live trip.
 */
private const val HOLD_DURATION_MS = 600

@Composable
private fun HoldToStopButton(
    onCommitted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }

    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            var committed = false
                            val fillJob =
                                scope.launch {
                                    progress.snapTo(0f)
                                    progress.animateTo(1f, tween(HOLD_DURATION_MS, easing = LinearEasing))
                                    committed = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCommitted()
                                }
                            tryAwaitRelease()
                            if (!committed) {
                                fillJob.cancel()
                                scope.launch { progress.animateTo(0f, tween(150)) }
                            }
                        },
                    )
                }
                .semantics { contentDescription = "Hold to stop tracking" },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(StopButtonSize)) {
            drawCircle(color = Color(0xFFB91C1C))
            if (progress.value > 0f) {
                // Radial fill: a pie wedge growing clockwise from 12 o'clock, not a ring — a ring
                // reads as a loading spinner, a wedge reads as "how much further to hold".
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = true,
                )
            }
        }
        Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

// ── Formatting ───────────────────────────────────────────────────────────────────────

private fun formatKm2(km: Double): String {
    val r = kotlin.math.round(km * 100.0) / 100.0
    val whole = r.toLong()
    val frac = kotlin.math.round(kotlin.math.abs(r - whole) * 100.0).toLong()
    return "$whole.${frac.toString().padStart(2, '0')}"
}

private fun formatSpeed1(kmh: Double): String {
    val r = kotlin.math.round(kmh * 10.0) / 10.0
    val whole = r.toLong()
    val frac = kotlin.math.round(kotlin.math.abs(r - whole) * 10.0).toLong()
    return "$whole.$frac"
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:$mm:$ss" else "$mm:$ss"
}
