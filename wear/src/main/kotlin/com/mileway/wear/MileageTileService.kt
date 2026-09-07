package com.mileway.wear

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mileway.core.data.model.display.SnapshotPublisher
import com.mileway.core.data.model.display.SurfaceSnapshot
import org.koin.mp.KoinPlatform

/**
 * P2.6: Wear OS Tile service, companion tile for Mileway.
 *
 * Displays today's tracked distance and the app label on the watch face. Reads the CACHED
 * [SurfaceSnapshot] straight off [SnapshotPublisher.snapshot]'s [kotlinx.coroutines.flow.StateFlow]
 * value — never a live Room/DataLayer fetch on the tile-render path (mirrors biciradar: a tile
 * process is short-lived and re-launched cold on every timeline refresh, so it must never block on
 * I/O). [WearAppGraph] is idempotent, so calling [WearAppGraph.start] here is safe whether or not the
 * [WearActivity] process is already warm.
 */
class MileageTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> =
        Futures.immediateFuture(buildTile(readCachedSnapshot(this)))

    @Deprecated("Migrate to onTileResourcesRequest", ReplaceWith("onTileResourcesRequest"))
    @Suppress("DEPRECATION")
    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<androidx.wear.tiles.ResourceBuilders.Resources> =
        Futures.immediateFuture(
            androidx.wear.tiles.ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        )

    private fun buildTile(snapshot: SurfaceSnapshot): TileBuilders.Tile =
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MILLIS)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(buildLayout(snapshot))
            )
            .build()

    private fun buildLayout(snapshot: SurfaceSnapshot): LayoutElementBuilders.LayoutElement {
        // AMBIENT.1: the cached snapshot going stale (publisher/sync stalled) matters more to the
        // user than which exact live word would've shown — a frozen "TRACKING" is a lie, so stale
        // wins over the normal status label rather than the two being shown at once.
        val statusLabel =
            if (WearPresentation.isStale(snapshot, System.currentTimeMillis())) {
                "STALE"
            } else {
                WearPresentation.toTileStatusLabel(snapshot)
            }
        val columnBuilder =
            LayoutElementBuilders.Column.Builder()
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(openWearActivityClickable())
                        .build()
                )
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(WearPresentation.toTodayDistanceLabel(snapshot))
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(DimensionBuilders.sp(28f))
                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText("Doori")
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(DimensionBuilders.sp(12f))
                                .build()
                        )
                        .build()
                )
        if (statusLabel != null) {
            columnBuilder.addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(statusLabel)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(DimensionBuilders.sp(STATUS_LABEL_SP))
                            .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                            .build()
                    )
                    .build()
            )
        }
        return columnBuilder.build()
    }

    private fun openWearActivityClickable(): ModifiersBuilders.Clickable =
        ModifiersBuilders.Clickable.Builder()
            .setId(LAUNCH_ACTIVITY_CLICKABLE_ID)
            .setOnClick(
                ActionBuilders.launchAction(ComponentName(this, WearActivity::class.java))
            )
            .build()

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val LAUNCH_ACTIVITY_CLICKABLE_ID = "open_wear_activity"

        /** Matches P2.6's acceptance: the tile is allowed to go stale for up to a minute before the
         * system re-invokes [onTileRequest] — cheap since the read is cache-only. */
        private const val FRESHNESS_INTERVAL_MILLIS = 60_000L

        /** AMBIENT.1: the status-word line's font size, smaller than the distance (28sp) and app
         * label (12sp) above it. */
        private const val STATUS_LABEL_SP = 11f

        /**
         * Boots [WearAppGraph] if needed (idempotent — safe to call from a cold tile process) and
         * reads [SnapshotPublisher.snapshot]'s current [kotlinx.coroutines.flow.StateFlow.value] —
         * the already-cached snapshot, never a fresh Room query. Reads [SnapshotPublisher] directly
         * (rather than through [com.mileway.feature.tracking.watch.WatchFacade.observeSnapshot],
         * which widens to a plain `Flow` for the dashboard's `combine`) specifically to keep this
         * `.value` synchronous read on the tile's cache-only path.
         */
        internal fun readCachedSnapshot(context: Context): SurfaceSnapshot {
            WearAppGraph.start(context)
            val snapshotPublisher = KoinPlatform.getKoin().get<SnapshotPublisher>()
            return snapshotPublisher.snapshot.value
        }
    }
}
