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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayType
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

private val GlanceLineHeight = 44.dp
private val ControlBarHeight = 96.dp
private val PauseFlagButtonSize = 56.dp
private val StopButtonSize = 88.dp

/**
 * The hero live-tracking surface: map as background, a floating distance/elapsed slab (distance
 * dominant — everything else, including live speed, lives behind its swipe-up expansion), and a
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
    /** Renders the metric slab pre-expanded — a capture/preview hook, see [MetricSlab]. */
    initiallyExpanded: Boolean = false,
    /**
     * Forces the glance status line to a specific [DegradedState] instead of deriving it from
     * [state]/gap-timing — capture/preview only. Every other [DegradedState] is reachable through
     * plain [LiveDriveState] fields (see [LiveDrivePreviewStates]); [DegradedState.RECOVERED_AFTER_GAP]
     * is the one exception, since it's measured from elapsed wall-clock time between two fixes and
     * has no field of its own to set.
     */
    previewDegradedState: DegradedState? = null,
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
        previewDegradedState ?: deriveDegradedState(
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
            // Never auto-center on the (0,0) sentinel — that's Null Island, not "no fix yet", and
            // driving the camera there is exactly the broken-looking first-run map a real device
            // would never produce (a location callback that fires at all always has a real fix).
            autoCenterEnabled = cameraMode == CameraMode.FOLLOW && hasFix,
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

        // One line, always in the same place, never a stacked panel: GPS status in the clear, or
        // the single most urgent degraded condition in its place. A driver checks this with a
        // glance, not a read.
        GlanceStatusLine(
            state = state,
            hasFix = hasFix,
            degradedState = degradedState,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // Zeros before a fix ever lands ("0.00 km / 00:00") are meaningless to a first-time
        // user — the empty state they actually see is "still searching", not a stat slab of
        // nothing, so it gets its own placeholder in the same slot instead.
        if (hasFix) {
            MetricSlab(
                state = state,
                initiallyExpanded = initiallyExpanded,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = DesignTokens.Spacing.l),
            )
        } else {
            NoFixPlaceholder(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = DesignTokens.Spacing.l),
            )
        }

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

// ── Glance status line ───────────────────────────────────────────────────────────────
//
// Was two stacked surfaces: a multi-chip status strip (signal, quality%, battery saver,
// unsynced count) plus a two-line degraded banner underneath it — a panel, not a glance.
// Reduced to one line, one message, in the one place it always is. Signal/quality/battery/
// sync detail that isn't "is this drive still trustworthy right now" moved to the metric
// slab's swipe-up expansion, where a stopped-at-a-light glance can actually afford it.

@Composable
private fun GlanceStatusLine(
    state: LiveDriveState,
    hasFix: Boolean,
    degradedState: DegradedState,
    modifier: Modifier = Modifier,
) {
    val message = degradedState.message()
    val label = message?.title ?: "GPS ${state.signal.glanceLabel()}"
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(GlanceLineHeight)
                .background(if (message != null) degradedState.toTone().color.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = DesignTokens.Spacing.l)
                .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Icon(
            imageVector = if (hasFix) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
            contentDescription = null,
            tint = if (message != null) Color.White else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
        )
    }
}

private fun TrackSignal.glanceLabel(): String =
    when (this) {
        TrackSignal.GOOD -> "Strong"
        TrackSignal.FAIR -> "Fair"
        TrackSignal.POOR -> "Weak"
    }

// ── Metric slab ──────────────────────────────────────────────────────────────────────

@Composable
private fun MetricSlab(
    state: LiveDriveState,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
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
            // The claim is made of this number. It is the reason the screen exists, so it
            // dominates unambiguously — the biggest fixed data style in the design system,
            // nothing else on this slab competes with it.
            Text(
                text = formatKm2(state.distanceKm),
                style = MilewayType.dataLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Distance ${formatKm2(state.distanceKm)} kilometres"
                    },
            )
            // Elapsed time is the only other thing worth a glance while driving (a sanity check
            // that the trip hasn't been running longer than expected). Current speed isn't part
            // of the reimbursement math and the car's own speedometer already shows it — it moved
            // into the expansion below with the rest of the trip stats.
            Text(
                text = formatElapsed(state.elapsedMs),
                style = MaterialTheme.typography.titleMedium.dataStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // The swipe gesture on the slab itself expands/collapses this, but a rotating chevron
            // reads as a button — it needs its own tap target too, not just a hidden gesture on
            // the surface behind it (this was previously decorative only, unreachable by TalkBack).
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = if (expanded) "Collapse trip stats" else "Expand trip stats",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(top = DesignTokens.Spacing.xs)
                        .rotate(if (expanded) 180f else 0f)
                        .clip(CircleShape)
                        .pointerInput(Unit) { detectTapGestures(onTap = { expanded = !expanded }) }
                        .padding(4.dp),
            )

            AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier.padding(top = DesignTokens.Spacing.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ExpansionRow("Speed", "${formatSpeed1(state.speedKmh)} km/h")
                    ExpansionRow("Avg speed", "${formatSpeed1(state.avgSpeedKmh)} km/h")
                    ExpansionRow("Max speed", "${formatSpeed1(state.maxSpeedKmh)} km/h")
                    ExpansionRow("Points", "${state.pointsCount}")
                    ExpansionRow("Quality", "${state.qualityScore}%")
                    ExpansionRow(
                        "Battery",
                        "${state.batteryPct}%" + if (state.isCharging) " (charging)" else "",
                    )
                    if (state.unsyncedPoints > 0) ExpansionRow("Unsynced", "${state.unsyncedPoints}")
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

/**
 * What a first-time user actually sees before their first GPS fix lands — takes the exact same
 * slot [MetricSlab] would, so there is never a moment of "0.00 km" floating over a map centered on
 * the middle of the ocean. Reuses [DegradedState.NO_FIX]'s copy (never a second, drifting copy of
 * the same message) — the top glance line is the one-line notification, this is the empty state
 * for the main content area itself.
 */
@Composable
private fun NoFixPlaceholder(modifier: Modifier = Modifier) {
    val message = DegradedState.NO_FIX.message()!!
    Surface(
        modifier = modifier,
        shape = DesignTokens.Shape.roundedLg,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = DesignTokens.Elevation.raised,
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.GpsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = message.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = DesignTokens.Spacing.m),
            )
            message.action?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
                )
            }
        }
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

// ── Preview / capture states ────────────────────────────────────────────────────────

/**
 * Canonical [LiveDriveState] snapshots for capture/preview — every degraded state is driven by a
 * specific, easy-to-get-wrong-by-hand combination of fields (which flag, which enum, which
 * sentinel), so the gallery gets one call per state instead of re-deriving that combination five
 * times. [LiveDriveScreen] is already a stateless composable; these are just data, no `@Composable`
 * needed. For [DegradedState.RECOVERED_AFTER_GAP], pair [tracking] with
 * `LiveDriveScreen(..., previewDegradedState = DegradedState.RECOVERED_AFTER_GAP)` — that one
 * state is measured from elapsed time between two fixes, not a field here.
 */
object LiveDrivePreviewStates {
    private val base =
        LiveDriveState(
            phase = TrackMilesPhase.TRACKING,
            distanceKm = 12.42,
            elapsedMs = 1_421_000L,
            speedKmh = 48.0,
            avgSpeedKmh = 31.5,
            maxSpeedKmh = 62.0,
            pointsCount = 842L,
            qualityScore = 94,
            batteryPct = 68,
            isCharging = false,
            unsyncedPoints = 12L,
            pauseReason = null,
            currentLat = 18.5204,
            currentLng = 73.8567,
            bearingDegrees = 118f,
            signal = TrackSignal.GOOD,
            systemFlags = TrackingSystemFlags(),
        )

    /** Actively tracking, everything healthy — the same state a normal capture already shows. */
    fun tracking(): LiveDriveState = base

    /** Paused mid-trip, e.g. auto-paused on a detected stop. */
    fun paused(): LiveDriveState = base.copy(phase = TrackMilesPhase.PAUSED, pauseReason = "Speed suggests you're not driving")

    /** Before the first GPS callback ever lands — the (0,0) sentinel, zero everything. */
    fun noFix(): LiveDriveState =
        base.copy(currentLat = 0.0, currentLng = 0.0, distanceKm = 0.0, elapsedMs = 0L, pointsCount = 0L, qualityScore = 0, speedKmh = 0.0)

    fun poorAccuracy(): LiveDriveState = base.copy(signal = TrackSignal.POOR, qualityScore = 38)

    fun permissionRevoked(): LiveDriveState = base.copy(systemFlags = TrackingSystemFlags(permissionMissing = true))

    fun offline(): LiveDriveState = base.copy(isOffline = true)

    fun batterySaver(): LiveDriveState = base.copy(systemFlags = TrackingSystemFlags(powerSaverOn = true), batteryPct = 14)
}
