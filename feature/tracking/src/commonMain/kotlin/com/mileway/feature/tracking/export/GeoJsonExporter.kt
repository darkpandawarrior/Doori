package com.mileway.feature.tracking.export

import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack

/**
 * Pure-Kotlin GeoJSON exporter.
 *
 * Output structure:
 * {
 *   "type": "FeatureCollection",
 *   "features": [
 *     { "type": "Feature", "geometry": { "type": "LineString", "coordinates": [...] }, "properties": { track metadata } },
 *     { "type": "Feature", "geometry": { "type": "Point", ... }, "properties": { "label": "Start" } },
 *     { "type": "Feature", "geometry": { "type": "Point", ... }, "properties": { "label": "End" } }
 *   ]
 * }
 */
object GeoJsonExporter {
    fun export(
        track: SavedTrack,
        locations: List<LocationData>,
        events: List<HardwareEvent>,
    ): String =
        buildString {
            appendLine("{")
            appendLine("""  "type": "FeatureCollection",""")
            appendLine("""  "properties": {""")
            appendLine("""    "trackName": ${jsonStr(track.name)},""")
            appendLine("""    "routeId": ${jsonStr(track.routeId)},""")
            appendLine("""    "distanceM": ${track.distance},""")
            appendLine("""    "durationMs": ${track.duration},""")
            appendLine("""    "startTime": ${track.startTime},""")
            appendLine("""    "endTime": ${track.endTime},""")
            appendLine("""    "pointCount": ${locations.size},""")
            appendLine("""    "eventCount": ${events.size}""")
            appendLine("  },")
            appendLine("""  "features": [""")

            val featureParts = mutableListOf<String>()

            // 1. LineString with all points
            if (locations.isNotEmpty()) {
                val coords =
                    locations.joinToString(",\n        ") { loc ->
                        if (loc.altitude != 0.0) {
                            "[${loc.lng},${loc.lat},${loc.altitude}]"
                        } else {
                            "[${loc.lng},${loc.lat}]"
                        }
                    }
                featureParts +=
                    buildString {
                        appendLine("    {")
                        appendLine("""      "type": "Feature",""")
                        appendLine("""      "geometry": {""")
                        appendLine("""        "type": "LineString",""")
                        appendLine("""        "coordinates": [""")
                        appendLine("        $coords")
                        appendLine("        ]")
                        appendLine("      },")
                        appendLine("""      "properties": {""")
                        appendLine("""        "name": ${jsonStr(track.name)},""")
                        appendLine("""        "routeId": ${jsonStr(track.routeId)}""")
                        appendLine("      }")
                        append("    }")
                    }

                // 2. Start point
                val start = locations.first()
                featureParts +=
                    buildString {
                        appendLine("    {")
                        appendLine("""      "type": "Feature",""")
                        appendLine("""      "geometry": { "type": "Point", "coordinates": [${start.lng},${start.lat}] },""")
                        appendLine("""      "properties": { "label": "Start", "timestamp": ${start.date} }""")
                        append("    }")
                    }

                // 3. End point
                val end = locations.last()
                featureParts +=
                    buildString {
                        appendLine("    {")
                        appendLine("""      "type": "Feature",""")
                        appendLine("""      "geometry": { "type": "Point", "coordinates": [${end.lng},${end.lat}] },""")
                        appendLine("""      "properties": { "label": "End", "timestamp": ${end.date} }""")
                        append("    }")
                    }
            }

            append(featureParts.joinToString(",\n"))
            appendLine()
            appendLine("  ]")
            append("}")
        }

    private fun jsonStr(s: String): String = "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}
