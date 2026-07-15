package com.mileway.feature.tracking.export

import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.siddharth.kmp.common.pad2
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Pure-Kotlin GPX exporter. Produces a valid GPX 1.1 document as a String.
 */
object GpxExporter {
    private fun formatIsoUtc(epochMs: Long): String {
        val ldt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC)
        return "${ldt.year}-${ldt.monthNumber.pad2()}-${ldt.dayOfMonth.pad2()}" +
            "T${ldt.hour.pad2()}:${ldt.minute.pad2()}:${ldt.second.pad2()}Z"
    }

    fun export(
        track: SavedTrack,
        locations: List<LocationData>,
        @Suppress("UNUSED_PARAMETER") events: List<HardwareEvent>,
    ): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine(
                """<gpx version="1.1" creator="Mileway" xmlns="http://www.topografix.com/GPX/1/1">""",
            )
            appendLine("  <trk>")
            appendLine("    <name>${xmlEsc(track.name)}</name>")
            appendLine("    <desc>routeId: ${xmlEsc(track.routeId)}</desc>")
            appendLine("    <trkseg>")

            for (loc in locations) {
                val time = formatIsoUtc(loc.date)
                appendLine("""      <trkpt lat="${loc.lat}" lon="${loc.lng}">""")
                appendLine("        <ele>${loc.altitude}</ele>")
                appendLine("        <time>$time</time>")
                appendLine("        <speed>${loc.speed}</speed>")
                if (loc.accuracy > 0f) {
                    appendLine("        <hdop>${loc.accuracy}</hdop>")
                }
                appendLine("      </trkpt>")
            }

            appendLine("    </trkseg>")
            appendLine("  </trk>")
            appendLine("</gpx>")
        }

    private fun xmlEsc(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
