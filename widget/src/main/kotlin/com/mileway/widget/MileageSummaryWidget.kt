package com.mileway.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mileway.core.data.model.display.TrackingState
import com.mileway.core.data.watch.SnapshotCache
import com.mileway.core.data.watch.WatchSyncPayload
import com.mileway.feature.tracking.service.TrackingNotificationMapper
import com.mileway.feature.tracking.service.TrackingServiceApi
import com.mileway.feature.tracking.service.TrackingSnapshot
import com.mileway.feature.tracking.watch.WatchFacade
import kotlin.math.round
import org.koin.mp.KoinPlatform

// Fixed palette (T.2: mirrors core:ui's Ember spec — warm-dark surface + amber accent) — plain
// Glance colors keep the widget free of the Material-You/glance-material3 surface so it renders
// identically across hosts.
private val SurfaceColor = Color(0xFF17110B)
private val AccentColor = Color(0xFFF5A623)
private val LiveColor = Color(0xFFFF453A)
private val OnSurfaceColor = Color(0xFFF7EFE3)
private val StaleColor = Color(0xFF8A7F6E)

/**
 * P6.2/AMBIENT.1: state a widget renders — a pure projection of [WatchSyncPayload] (the same wire
 * shape [SnapshotCache] persists) plus the live in-process [TrackingSnapshot] down to the
 * strings/flags [MileageSummaryContent] lays out. Kept separate from the payload itself (rather
 * than rendering [WatchSyncPayload] fields directly) so the render layer never needs to know the
 * payload's field names, and so the mapping is unit-testable without a Glance host (see
 * `WidgetUiModelTest`).
 *
 * AMBIENT.1: [isStale] flags a cached [WatchSyncPayload] that claims a drive is in progress but
 * hasn't been refreshed in a while — see [buildWidgetUiModel]'s doc for why this can happen and
 * why showing that distinctly beats a confidently frozen number.
 */
data class WidgetUiModel(
    val todayLabel: String,
    val weekLabel: String,
    val statusLabel: String?,
    val isTracking: Boolean,
    val isStale: Boolean = false,
)

/**
 * AMBIENT.1: pure state->widget mapper (P6.2's original acceptance, extended). [payload] supplies
 * today's/this week's rollups (only source for those — a [TrackingSnapshot] doesn't carry them).
 * [liveSnapshot] is the widget's in-process read of [TrackingServiceApi.trackingState] (the widget
 * runs in-process on Android — see [MileageSummaryWidget]'s doc comment — so it can read the exact
 * same live state [TrackingNotificationMapper] renders for the phone's foreground notification, not
 * just the periodically-synced [payload]).
 *
 * When [liveSnapshot] shows a live session, its [TrackingNotificationMapper.fromSnapshot] title
 * *is* the status line — the same words the phone notification/Wear ongoing activity show, so a
 * GPS-lost or permission-missing state (which [payload]'s isTracking/isPaused booleans can't
 * represent) surfaces on the widget too, not just "tracking"/"paused". When no live snapshot is
 * available (Koin not started yet — rare, but the widget can be re-launched before the app process
 * finishes booting), it falls back to [payload]'s isTracking/isPaused, worded to match the mapper's
 * own ACTIVE/PAUSED titles, and treats that fallback reading as possibly stale if [payload] hasn't
 * been refreshed in over [STALE_THRESHOLD_MS] — [liveSnapshot], being read synchronously in the
 * same process, can never itself be stale.
 */
fun buildWidgetUiModel(
    payload: WatchSyncPayload,
    liveSnapshot: TrackingSnapshot?,
    nowEpochMs: Long,
): WidgetUiModel {
    val liveContent =
        liveSnapshot
            ?.takeIf { it.state == TrackingState.LIVE_TRACKING || it.state == TrackingState.PAUSED }
            ?.let { TrackingNotificationMapper.fromSnapshot(it) }
    val fallbackStatus =
        when {
            payload.isTracking && payload.isPaused -> "Tracking paused"
            payload.isTracking -> "Tracking active"
            else -> null
        }
    return WidgetUiModel(
        todayLabel = "Today   ${format1(payload.todayKm)} km",
        weekLabel = "Week    ${format1(payload.weekKm)} km · ${payload.tripCount} trips",
        statusLabel = liveContent?.title ?: fallbackStatus,
        isTracking = liveContent != null || payload.isTracking,
        isStale = liveContent == null && payload.isTracking && nowEpochMs - payload.updatedAtMs > STALE_THRESHOLD_MS,
    )
}

/**
 * P6.2: the original cache-only mapper, kept byte-for-byte unchanged. `:app`'s
 * `MileageSummaryWidgetTest` (outside this module, not touched by AMBIENT.1 — see that task's
 * ownership boundary) pins this exact wording/glyphs, so this stays a pure passthrough to
 * [buildWidgetUiModel] with no live snapshot rather than being reworded to match it. New callers
 * (this module's own [MileageSummaryWidget.provideGlance]) should prefer [buildWidgetUiModel]
 * directly, which also considers the live [TrackingSnapshot] and staleness.
 */
fun WatchSyncPayload.toWidgetUiModel(): WidgetUiModel =
    WidgetUiModel(
        todayLabel = "Today   ${format1(todayKm)} km",
        weekLabel = "Week    ${format1(weekKm)} km · $tripCount trips",
        statusLabel =
            when {
                isTracking && isPaused -> "‖ Paused"
                isTracking -> "● Tracking now"
                else -> null
            },
        isTracking = isTracking,
    )

private const val ONE_DECIMAL_SCALE = 10.0

/** AMBIENT.1: mirrors [com.mileway.wear.WearPresentation]'s tile threshold — same reasoning: long
 * enough to tolerate a normal update gap, short enough to catch a genuinely dead sync well before
 * the user notices a frozen distance on their own. */
private const val STALE_THRESHOLD_MS = 5 * 60_000L

private fun format1(value: Double): String {
    val scaled = round(value * ONE_DECIMAL_SCALE) / ONE_DECIMAL_SCALE
    return scaled.toString()
}

/**
 * P6.2/AMBIENT.1: a home-screen [GlanceAppWidget] summarising today's/this-week's mileage, plus an
 * interactive Start/Stop button. Reads [SnapshotCache] (P6.1) rather than opening the Room
 * database directly (widgets are re-launched cold on every timeline refresh — see
 * [SnapshotCache]'s doc comment for why touching Room from here is the anti-pattern P6.1 replaces).
 * Resolves its dependencies via `KoinPlatform.getKoin()` — same pattern
 * `WearTrackingCommandService`/`MileageTileService` use for framework-instantiated Android
 * components Koin cannot constructor-inject — since a home-screen widget runs in-process on
 * Android (unlike an iOS WidgetKit extension), the app's already-started Koin graph is always
 * reachable here — which is also why it can additionally resolve [TrackingServiceApi] (see
 * [buildWidgetUiModel]'s doc) alongside the cache-only [SnapshotCache] read P6.1 already relies on.
 */
class MileageSummaryWidget : GlanceAppWidget() {
    // AMBIENT.1: mileage_widget_info.xml declares resizeMode="horizontal|vertical", so the host can
    // resize this widget anywhere between its minWidth/minHeight and target cell size. SizeMode.Exact
    // keeps LocalSize.current tracking the real current size on every resize; the SizeMode.Single
    // default would instead freeze content at the widget's minimum declared size regardless of how
    // much room the host actually gives it, which is the "assumes one size" bug this replaces.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val koin = KoinPlatform.getKoin()
        val payload = koin.getOrNull<SnapshotCache>()?.read() ?: WatchSyncPayload()
        val liveSnapshot = koin.getOrNull<TrackingServiceApi>()?.trackingState?.value
        val model = buildWidgetUiModel(payload, liveSnapshot, System.currentTimeMillis())
        provideContent {
            MileageSummaryContent(model)
        }
    }
}

/** AMBIENT.1: below this height there isn't room for the title/week-line/button without clipping
 * on a host launcher (the widget's declared minHeight is 110dp) — the compact layout drops down to
 * just today's distance and the status line. */
private val COMPACT_HEIGHT_THRESHOLD = 130.dp

/**
 * Stateless render of a [WidgetUiModel]. Public so the Glance render test can drive it directly
 * with a fixed model (no cache/Koin), matching the "test trivially" contract of the shared model.
 *
 * AMBIENT.1: reads [LocalSize] (populated because [MileageSummaryWidget.sizeMode] is
 * [SizeMode.Exact]) to switch between the full layout and a [COMPACT_HEIGHT_THRESHOLD]-gated
 * compact one, so a user who resizes the widget down to its minimum declared size still gets a
 * legible (not clipped) render instead of the fixed layout this replaces.
 */
@Composable
fun MileageSummaryContent(model: WidgetUiModel) {
    val isCompact = LocalSize.current.height < COMPACT_HEIGHT_THRESHOLD
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(SurfaceColor)
                .cornerRadius(16.dp)
                .padding(if (isCompact) 8.dp else 16.dp),
    ) {
        if (!isCompact) {
            Text(
                text = "Mileway",
                style = TextStyle(color = ColorProvider(AccentColor), fontWeight = FontWeight.Bold, fontSize = 16.sp),
            )
            Spacer(GlanceModifier.height(8.dp))
        }
        Text(
            text = model.todayLabel,
            style = TextStyle(color = ColorProvider(OnSurfaceColor), fontSize = 14.sp),
        )
        if (!isCompact) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = model.weekLabel,
                style = TextStyle(color = ColorProvider(OnSurfaceColor), fontSize = 14.sp),
            )
        }
        // AMBIENT.1: a stale cache wins over the normal status word — a frozen "Tracking active" is
        // actively misleading, so this must never render underneath/alongside it.
        val statusText = if (model.isStale) "Data may be out of date" else model.statusLabel
        if (statusText != null) {
            Spacer(GlanceModifier.height(if (isCompact) 2.dp else 6.dp))
            Text(
                text = statusText,
                style =
                    TextStyle(
                        color = ColorProvider(if (model.isStale) StaleColor else if (model.isTracking) LiveColor else AccentColor),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
            )
        }
        if (!isCompact) {
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = if (model.isTracking) "■ Stop" else "▶ Start",
                style = TextStyle(color = ColorProvider(AccentColor), fontWeight = FontWeight.Bold, fontSize = 14.sp),
                modifier =
                    GlanceModifier
                        .clickable(
                            actionRunCallback<ToggleTrackingAction>(
                                actionParametersOf(IsTrackingKey.to(model.isTracking)),
                            ),
                        )
                        .semantics {
                            contentDescription = if (model.isTracking) "Stop tracking" else "Start tracking"
                        },
            )
        }
    }
}

private val IsTrackingKey = ActionParameters.Key<Boolean>("is_tracking")

/**
 * The widget's quick-start/stop action (P6.2 acceptance: "the action toggles tracking"). Proxies
 * to [WatchFacade.startTracking]/[WatchFacade.stopTracking] — the same start/stop seam the Wear OS
 * UI already binds to — so a widget-initiated trip behaves identically to a watch-initiated one.
 */
class ToggleTrackingAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val facade = KoinPlatform.getKoin().getOrNull<WatchFacade>() ?: return
        val wasTracking = parameters[IsTrackingKey] ?: false
        if (wasTracking) facade.stopTracking() else facade.startTracking()
        MileageSummaryWidget().update(context, glanceId)
    }
}

/** Registers [MileageSummaryWidget] with the platform (declared in the manifest). */
class MileageSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MileageSummaryWidget()
}
