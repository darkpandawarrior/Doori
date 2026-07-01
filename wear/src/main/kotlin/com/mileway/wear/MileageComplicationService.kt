package com.mileway.wear

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class MileageComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.SHORT_TEXT -> shortText("3.2 km")
            ComplicationType.RANGED_VALUE -> rangedValue(3.2f, 0f, 10f, "3.2")
            else -> null
        }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val distanceKm = readTodayDistanceKm()
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> shortText("${"%.1f".format(distanceKm)} km")
            ComplicationType.RANGED_VALUE -> rangedValue(distanceKm, 0f, 10f, "%.1f".format(distanceKm))
            else -> null
        }
    }

    private fun tapAction(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(Intent.ACTION_MAIN).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun shortText(text: String): ShortTextComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Today's mileage").build(),
        )
            .setTapAction(tapAction())
            .build()

    private fun rangedValue(
        value: Float,
        min: Float,
        max: Float,
        text: String,
    ): RangedValueComplicationData =
        RangedValueComplicationData.Builder(
            value = value,
            min = min,
            max = max,
            contentDescription = PlainComplicationText.Builder("Today's mileage").build(),
        )
            .setText(PlainComplicationText.Builder(text).build())
            .setTapAction(tapAction())
            .build()

    private fun readTodayDistanceKm(): Float = DEMO_DISTANCE_KM

    companion object {
        private const val DEMO_DISTANCE_KM = 0.0f
    }
}
