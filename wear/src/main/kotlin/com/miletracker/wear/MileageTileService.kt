package com.miletracker.wear

import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Wear OS Tile service, companion tile for Mileway.
 *
 * Displays today's tracked distance and the app label on the watch face.
 * Production implementation would read from a shared DataStore via Wearable DataLayer API.
 */
class MileageTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> =
        Futures.immediateFuture(buildTile())

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

    private fun buildTile(): TileBuilders.Tile =
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(buildLayout())
            )
            .build()

    private fun buildLayout(): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("0.0 km")
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
                    .setText("Mileway")
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(DimensionBuilders.sp(12f))
                            .build()
                    )
                    .build()
            )
            .build()

    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
