@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.tracking.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.util.DateUtils
import java.io.File

/** Serialises a list of [HardwareEvent] records to CSV or JSON for sharing. */
object HardwareEventExporter {
    fun buildCsvPayload(events: List<HardwareEvent>): String {
        val header = "token,eventType,audience,time,lat,lng,event\n"
        val rows =
            events.joinToString("\n") { e ->
                val time = DateUtils.epochToDisplayDate(e.time)
                val lat = "%.6f".format(e.lat)
                val lng = "%.6f".format(e.lng)
                val note = e.event.replace("\"", "\"\"")
                "\"${e.token}\",${e.eventType},${e.audience},\"$time\",$lat,$lng,\"$note\""
            }
        return header + rows
    }

    fun buildJsonPayload(events: List<HardwareEvent>): String {
        val items =
            events.joinToString(",\n  ") { e ->
                val time = DateUtils.epochToDisplayDate(e.time)
                val note = e.event.replace("\\", "\\\\").replace("\"", "\\\"")
                """{"token":"${e.token}","eventType":"${e.eventType}","audience":"${e.audience}","time":"$time","lat":${e.lat},"lng":${e.lng},"event":"$note"}"""
            }
        return "[\n  $items\n]"
    }

    fun shareEvents(
        context: Context,
        routeId: String,
        events: List<HardwareEvent>,
        useCsv: Boolean = true,
    ): Intent {
        val ts = System.currentTimeMillis()
        val ext = if (useCsv) "csv" else "json"
        val mime = if (useCsv) "text/csv" else "application/json"
        val content = if (useCsv) buildCsvPayload(events) else buildJsonPayload(events)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, "events_${routeId}_$ts.$ext")
        outFile.writeText(content)

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile,
            )
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Hardware events: $routeId")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        return Intent.createChooser(sendIntent, "Share events as $ext")
    }
}
