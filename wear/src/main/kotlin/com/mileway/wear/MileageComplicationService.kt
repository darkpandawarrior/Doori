package com.mileway.wear

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.mileway.core.data.model.display.SnapshotPublisher
import com.mileway.core.data.model.display.SurfaceSnapshot
import org.koin.mp.KoinPlatform

/**
 * P2.7: Wear OS complication data source, companion complication for Mileway.
 *
 * Mirrors [MileageTileService]'s P2.6 cache-only read: complication requests are answered from the
 * CACHED [SurfaceSnapshot] off [SnapshotPublisher.snapshot]'s [kotlinx.coroutines.flow.StateFlow]
 * value — never a live Room/DataLayer fetch on the complication-render path, since a watch face
 * calls every registered [ComplicationDataSourceService] on every tick and the process may be cold.
 * [WearAppGraph] is idempotent, so calling [WearAppGraph.start] here is safe whether or not the
 * [WearActivity]/[MileageTileService] process is already warm.
 *
 * SHORT_TEXT renders today's tracked distance (shares [WearPresentation.toTodayDistanceLabel] with
 * the tile). RANGED_VALUE renders this week's progress toward [SurfaceSnapshot.weekGoalKm].
 */
class MileageComplicationService : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val preview = SurfaceSnapshot(todayDistanceKm = 3.2, weekDistanceKm = 32.0, weekGoalKm = 100.0)
        return renderComplication(type, preview)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        renderComplication(request.complicationType, readCachedSnapshot(this))

    private fun renderComplication(
        type: ComplicationType,
        snapshot: SurfaceSnapshot,
    ): ComplicationData? =
        when (type) {
            ComplicationType.SHORT_TEXT -> shortText(WearPresentation.toTodayDistanceLabel(snapshot))
            ComplicationType.RANGED_VALUE ->
                rangedValue(
                    value = snapshot.weekDistanceKm.toFloat().coerceIn(0f, snapshot.weekGoalKm.toFloat()),
                    min = 0f,
                    max = snapshot.weekGoalKm.toFloat().coerceAtLeast(RANGED_VALUE_MIN_MAX),
                    text = WearPresentation.toWeekGoalValueLabel(snapshot),
                )
            else -> null
        }

    private fun tapAction(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(Intent.ACTION_MAIN).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun shortText(text: String): ShortTextComplicationData =
        ShortTextComplicationData
            .Builder(
                text = PlainComplicationText.Builder(text).build(),
                contentDescription = PlainComplicationText.Builder("Today's mileage").build(),
            ).setTapAction(tapAction())
            .build()

    private fun rangedValue(
        value: Float,
        min: Float,
        max: Float,
        text: String,
    ): RangedValueComplicationData =
        RangedValueComplicationData
            .Builder(
                value = value,
                min = min,
                max = max,
                contentDescription = PlainComplicationText.Builder("Week goal progress").build(),
            ).setText(PlainComplicationText.Builder(text).build())
            .setTapAction(tapAction())
            .build()

    companion object {
        /** [RangedValueComplicationData] requires `max > min`; guards a misconfigured zero goal. */
        private const val RANGED_VALUE_MIN_MAX = 0.01f

        /**
         * Boots [WearAppGraph] if needed (idempotent — safe to call from a cold complication
         * process) and reads [SnapshotPublisher.snapshot]'s current
         * [kotlinx.coroutines.flow.StateFlow.value] — the already-cached snapshot, never a fresh Room
         * query. Reads [SnapshotPublisher] directly (mirrors [MileageTileService.readCachedSnapshot])
         * to keep this `.value` read synchronous on the complication's cache-only path.
         */
        internal fun readCachedSnapshot(context: Context): SurfaceSnapshot {
            WearAppGraph.start(context)
            val snapshotPublisher = KoinPlatform.getKoin().get<SnapshotPublisher>()
            return snapshotPublisher.snapshot.value
        }
    }
}
