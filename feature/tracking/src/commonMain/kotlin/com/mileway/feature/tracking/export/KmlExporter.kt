package com.mileway.feature.tracking.export

import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack

/**
 * Pure-Kotlin KML exporter. Produces KML 2.2 with a LineString + start/end Placemarks.
 */
object KmlExporter {
    fun export(
        track: SavedTrack,
        locations: List<LocationData>,
        @Suppress("UNUSED_PARAMETER") events: List<HardwareEvent>,
    ): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
            appendLine("  <Document>")
            appendLine("    <name>${xmlEsc(track.name)}</name>")
            appendLine("    <description>Route ID: ${xmlEsc(track.routeId)}</description>")

            // Route line
            appendLine("    <Placemark>")
            appendLine("      <name>Track Route</name>")
            appendLine("      <LineString>")
            appendLine("        <tessellate>1</tessellate>")
            appendLine("        <coordinates>")
            for (loc in locations) {
                appendLine("          ${loc.lng},${loc.lat},${loc.altitude}")
            }
            appendLine("        </coordinates>")
            appendLine("      </LineString>")
            appendLine("    </Placemark>")

            // Start point
            if (locations.isNotEmpty()) {
                val start = locations.first()
                appendLine("    <Placemark>")
                appendLine("      <name>Start</name>")
                appendLine("      <Point>")
                appendLine("        <coordinates>${start.lng},${start.lat},${start.altitude}</coordinates>")
                appendLine("      </Point>")
                appendLine("    </Placemark>")

                val end = locations.last()
                appendLine("    <Placemark>")
                appendLine("      <name>End</name>")
                appendLine("      <Point>")
                appendLine("        <coordinates>${end.lng},${end.lat},${end.altitude}</coordinates>")
                appendLine("      </Point>")
                appendLine("    </Placemark>")
            }

            appendLine("  </Document>")
            appendLine("</kml>")
        }

    private fun xmlEsc(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
