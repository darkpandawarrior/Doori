package com.mileway.feature.tracking.export

import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack

/**
 * Pure-Kotlin JSON exporter.
 *
 * Produces a structured object:
 * {
 *   "track": { ...metadata... },
 *   "points": [ { ...LocationData fields... } ],
 *   "events": [ { ...HardwareEvent fields... } ]
 * }
 *
 * Intentionally hand-built (no org.json dependency needed in pure-JVM tests).
 */
object JsonExporter {
    fun export(
        track: SavedTrack,
        locations: List<LocationData>,
        events: List<HardwareEvent>,
    ): String =
        buildString {
            appendLine("{")
            appendLine("""  "track": {""")
            appendLine("""    "routeId": ${js(track.routeId)},""")
            appendLine("""    "name": ${js(track.name)},""")
            appendLine("""    "distanceM": ${track.distance},""")
            appendLine("""    "durationMs": ${track.duration},""")
            appendLine("""    "startTime": ${track.startTime},""")
            appendLine("""    "endTime": ${track.endTime},""")
            appendLine("""    "startLat": ${track.startLatitude},""")
            appendLine("""    "startLng": ${track.startLongitude},""")
            appendLine("""    "endLat": ${track.endLatitude},""")
            appendLine("""    "endLng": ${track.endLongitude},""")
            appendLine("""    "avgSpeedMps": ${track.avgSpeed},""")
            appendLine("""    "maxSpeedMps": ${track.maxSpeed},""")
            appendLine("""    "vehicleType": ${js(track.selectedVehicleType)},""")
            appendLine("""    "isCompleted": ${track.isCompleted},""")
            appendLine("""    "serverUploaded": ${track.serverUploaded}""")
            appendLine("  },")

            // Points array
            appendLine("""  "points": [""")
            val pointParts = locations.map { loc -> locationJson(loc) }
            appendLine(pointParts.joinToString(",\n"))
            appendLine("  ],")

            // Events array
            appendLine("""  "events": [""")
            val eventParts = events.map { ev -> eventJson(ev) }
            appendLine(eventParts.joinToString(",\n"))
            appendLine("  ]")
            append("}")
        }

    private fun locationJson(loc: LocationData): String =
        buildString {
            appendLine("    {")
            appendLine("""      "id": ${loc.id},""")
            appendLine("""      "timestamp": ${loc.date},""")
            appendLine("""      "lat": ${loc.lat},""")
            appendLine("""      "lng": ${loc.lng},""")
            appendLine("""      "accuracy": ${loc.accuracy},""")
            appendLine("""      "speed": ${loc.speed},""")
            appendLine("""      "altitude": ${loc.altitude},""")
            appendLine("""      "bearing": ${loc.bearing},""")
            appendLine("""      "provider": ${js(loc.provider)},""")
            appendLine("""      "activity": ${js(loc.activity)},""")
            appendLine("""      "displacement": ${loc.displacement},""")
            appendLine("""      "isMock": ${loc.isMock},""")
            appendLine("""      "isAbnormal": ${loc.isAbnormal},""")
            appendLine("""      "isPaused": ${loc.isPaused},""")
            appendLine("""      "batteryPct": ${loc.batteryPercentage},""")
            appendLine("""      "wasCheckIn": ${loc.wasCheckInPoint}""")
            append("    }")
        }

    private fun eventJson(ev: HardwareEvent): String =
        buildString {
            appendLine("    {")
            appendLine("""      "id": ${ev.id},""")
            appendLine("""      "time": ${ev.time},""")
            appendLine("""      "event": ${js(ev.event)},""")
            appendLine("""      "eventType": ${js(ev.eventType.name)},""")
            appendLine("""      "lat": ${ev.lat ?: "null"},""")
            appendLine("""      "lng": ${ev.lng ?: "null"},""")
            appendLine("""      "battery": ${ev.battery ?: "null"},""")
            appendLine("""      "audience": ${js(ev.audience.name)}""")
            append("    }")
        }

    /** Escape a string value for JSON output. */
    private fun js(s: String): String = "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}
