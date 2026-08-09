package com.mileway.wear

import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.core.data.model.display.TrackingState
import com.mileway.feature.tracking.service.TrackingNotificationMapper
import com.mileway.feature.tracking.service.TrackingSnapshot
import com.mileway.feature.tracking.watch.TripSummary
import kotlin.math.round

/**
 * P2.4: pure, watchos-adjacent (JVM-testable) mapper from the shared [SurfaceSnapshot] to the
 * dashboard's rendering-ready [WearRootUiState]. Kept free of any `androidx.wear`/Compose import so
 * it can be unit-tested with a plain JUnit/kotlin.test runner (see `WearPresentationTest.kt`) —
 * mirrors the split `core:data`'s own `SurfaceSnapshotProducer` uses (pure fold, platform renderers
 * just lay the result out).
 *
 * P2.5 adds [toTripListItems], the equally pure mapper from [TripSummary] (the [WatchFacade]
 * value type) to [TripListItemUi] — trip-list/detail rendering-ready rows.
 */
object WearPresentation {
    fun toUiState(snapshot: SurfaceSnapshot): WearRootUiState =
        WearRootUiState(
            todayDistanceKm = snapshot.todayDistanceKm,
            weekDistanceKm = snapshot.weekDistanceKm,
            isTracking = snapshot.isTracking,
            weekGoalKm = snapshot.weekGoalKm,
            weekGoalProgress = snapshot.weekGoalProgress,
        )

    /** Maps [WatchFacade.recentTrips]' raw [TripSummary]s into display-ready [TripListItemUi] rows. */
    fun toTripListItems(trips: List<TripSummary>): List<TripListItemUi> = trips.map { it.toTripListItemUi() }

    /**
     * P2.6: the tile's/complication's shared today-distance label, e.g. `"12.4 km"` — one-decimal
     * rounding, pure so [MileageTileService]/[MileageComplicationService] never hand-roll their own
     * formatting (mirrors `MileageSummaryWidget`'s `format1` on the phone side).
     */
    fun toTodayDistanceLabel(snapshot: SurfaceSnapshot): String = "${formatOneDecimal(snapshot.todayDistanceKm)} km"

    /**
     * P2.7: the RANGED_VALUE complication's short text, e.g. `"58.7"` — the week's tracked distance
     * (km) rendered without a unit suffix, since [androidx.wear.watchface.complications.data.RangedValueComplicationData]
     * already conveys min/max/value; the text is just the numeric label shown inside the ring.
     */
    fun toWeekGoalValueLabel(snapshot: SurfaceSnapshot): String = formatOneDecimal(snapshot.weekDistanceKm)

    /**
     * P2.8/AMBIENT.1: pure mapper from the live [TrackingSnapshot] ([TrackingServiceApi.trackingState][com.mileway.feature.tracking.service.TrackingServiceApi.trackingState])
     * to the ongoing-activity's rendering-ready state — [WearActivity][com.mileway.wear.WearActivity]
     * drives [TrackingOngoingActivity.post]/[TrackingOngoingActivity.cancel] off this, never off the
     * raw [TrackingSnapshot] directly, so the "which states count as live" decision is unit-tested
     * here rather than duplicated at the call site.
     *
     * AMBIENT.1: [title]/[text] come straight from [TrackingNotificationMapper.fromSnapshot] — the
     * same copy the phone's foreground-service notification renders (see
     * `LocationTrackingService.updateNotification`) — rather than a second, hand-rolled "X km
     * tracked" string, so the watch's ongoing activity and the phone's notification never drift.
     * The mapper has no idle/READY branch (it assumes it is only ever called while a session is
     * live, exactly like the phone's call site), so [isLive] gates the call: an idle/completed
     * snapshot never reaches the mapper and just clears the notification.
     */
    fun toOngoingActivityState(snapshot: TrackingSnapshot): OngoingActivityUi {
        val isLive = snapshot.state == TrackingState.LIVE_TRACKING || snapshot.state == TrackingState.PAUSED
        if (!isLive) return OngoingActivityUi(isLive = false)
        val content = TrackingNotificationMapper.fromSnapshot(snapshot)
        return OngoingActivityUi(isLive = true, title = content.title, text = content.text)
    }

    /**
     * AMBIENT.1: the tile's compact live-state word — "TRACKING"/"PAUSED", or `null` when idle (no
     * active drive, so the tile shows only today's distance, no status line). Reuses the exact
     * two-word vocabulary [WearRootScreen][com.mileway.wear.WearRootScreen]'s dashboard pill already
     * shows ("TRACKING"/"IDLE") rather than [TrackingNotificationMapper]'s prose: the tile only has
     * the cached [SurfaceSnapshot] (P2.6's cache-only, cold-process-safe read), which carries
     * [SurfaceSnapshot.isPaused] but none of the GPS/permission/policy flags the mapper keys off, so
     * calling the mapper here would mean fabricating flags the tile doesn't actually know — see
     * AGENTS.md "report the gap, don't fork the vocabulary."
     */
    fun toTileStatusLabel(snapshot: SurfaceSnapshot): String? =
        when {
            snapshot.isTracking && snapshot.isPaused -> "PAUSED"
            snapshot.isTracking -> "TRACKING"
            else -> null
        }

    /**
     * AMBIENT.1: true when a [SurfaceSnapshot] claims to be live but hasn't been refreshed in over
     * [thresholdMs] — the tile/complication read [SnapshotPublisher.snapshot][com.mileway.core.data.model.display.SnapshotPublisher.snapshot]'s
     * cached value on every cold process launch, so a snapshot frozen mid-trip (the publishing
     * process died, or Wear's own DataLayer sync stalled) must be flagged rather than shown as a
     * confident live distance. Idle snapshots are never "stale" — there's nothing live to go stale.
     */
    fun isStale(
        snapshot: SurfaceSnapshot,
        nowEpochMs: Long,
        thresholdMs: Long = STALE_THRESHOLD_MS,
    ): Boolean = snapshot.isTracking && (nowEpochMs - snapshot.lastUpdatedEpochMs) > thresholdMs

    private fun formatOneDecimal(value: Double): String {
        val scaled = round(value * ONE_DECIMAL_SCALE) / ONE_DECIMAL_SCALE
        return scaled.toString()
    }

    private fun TripSummary.toTripListItemUi() =
        TripListItemUi(
            id = id,
            label = label.ifBlank { UNNAMED_TRIP_LABEL },
            km = km,
            endMs = endMs,
        )

    private const val UNNAMED_TRIP_LABEL = "Trip"
    private const val ONE_DECIMAL_SCALE = 10.0

    /** AMBIENT.1: how long a cached [SurfaceSnapshot] claiming to be live may go unrefreshed before
     * [isStale] flags it — generous enough to tolerate a normal GPS-fix gap, tight enough to catch
     * a genuinely dead publisher/sync well before a user would notice a frozen number on their own. */
    private const val STALE_THRESHOLD_MS = 5 * 60_000L
}

/**
 * Rendering-ready state for [WearRootScreen] — today/week distance, the tracking pill and the
 * week-goal progress ring, per P2.4's acceptance. Deliberately narrower than [SurfaceSnapshot]
 * (no trip counts/action badges — those aren't part of this task's dashboard).
 *
 * P2.5: [trips] backs the trip-list surface and [selectedTripId] drives which of [WearScreen]s is
 * shown — `null` means the dashboard, [WearScreen.TripList] the list, [WearScreen.TripDetail] the
 * detail surface for the trip matching [selectedTripId] in [trips].
 */
data class WearRootUiState(
    val todayDistanceKm: Double = 0.0,
    val weekDistanceKm: Double = 0.0,
    val isTracking: Boolean = false,
    val weekGoalKm: Double = SurfaceSnapshot.DEFAULT_WEEK_GOAL_KM,
    val weekGoalProgress: Float = 0f,
    val trips: List<TripListItemUi> = emptyList(),
    val screen: WearScreen = WearScreen.Dashboard,
    val selectedTripId: String? = null,
) {
    /** The [TripListItemUi] matching [selectedTripId], resolved once for [WearRootScreen] to render. */
    val selectedTrip: TripListItemUi?
        get() = trips.firstOrNull { it.id == selectedTripId }
}

/** P2.5: the single-activity screen `when` — [WearActivity][com.mileway.wear.WearActivity] never
 * navigates to a different Activity/Composable destination, it just swaps which of these renders
 * inside the one [WearRootScreen] (biciradar/P2.4 pattern, continued). */
enum class WearScreen {
    Dashboard,
    TripList,
    TripDetail,
}

/** A single rendering-ready row for the trip list/detail surfaces. */
data class TripListItemUi(
    val id: String,
    val label: String,
    val km: Double,
    val endMs: Long,
)

/**
 * P2.8/AMBIENT.1: rendering-ready state for the ongoing-activity notification — [isLive] drives
 * whether [WearActivity][com.mileway.wear.WearActivity] should have [TrackingOngoingActivity]
 * posted right now; [title]/[text] are [TrackingNotificationMapper]'s copy for the live snapshot
 * (empty when not live — the notification is cancelled instead of posted with blank text).
 */
data class OngoingActivityUi(
    val isLive: Boolean = false,
    val title: String = "",
    val text: String = "",
)
