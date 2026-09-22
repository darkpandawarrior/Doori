package com.mileway.feature.tracking.export

import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack

/**
 * Pure-Kotlin Excel exporter, matching the other exporters' `object.export(...): String` shape so
 * it plugs straight into [TrackExportContent] and the existing text/ShareSheet path.
 *
 * ponytail: emits SpreadsheetML 2003 (`.xls` XML), NOT a real `.xlsx`. A true xlsx is a ZIP of
 * OOXML parts, which would drag a zip/compression dependency (or a hand-rolled deflate) into a
 * previously String-only export pipeline for no functional gain — Excel, LibreOffice and Numbers
 * all open SpreadsheetML natively. Upgrade path: if a consumer requires a strict `.xlsx`, swap this
 * body for an OOXML zip writer behind the same `export()` signature and give [ExportFormat.EXCEL]
 * the `xlsx` extension.
 */
object ExcelExporter {
    fun export(
        track: SavedTrack,
        locations: List<LocationData>,
        @Suppress("UNUSED_PARAMETER") events: List<HardwareEvent>,
    ): String =
        buildString {
            appendLine("""<?xml version="1.0"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine(
                """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" """ +
                    """xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""",
            )

            // Summary sheet: one row of track metadata.
            appendLine("""  <Worksheet ss:Name="Summary">""")
            appendLine("    <Table>")
            appendLine(headerRow("Route Id", "Name", "Distance (m)", "Duration (ms)", "Start Time", "End Time", "Vehicle"))
            appendLine(
                dataRow(
                    str(track.routeId),
                    str(track.name),
                    num(track.distance),
                    num(track.duration),
                    num(track.startTime),
                    num(track.endTime),
                    str(track.selectedVehicleType),
                ),
            )
            appendLine("    </Table>")
            appendLine("  </Worksheet>")

            // Points sheet: one row per GPS point.
            appendLine("""  <Worksheet ss:Name="Points">""")
            appendLine("    <Table>")
            appendLine(headerRow("Timestamp", "Latitude", "Longitude", "Accuracy (m)", "Speed (m/s)", "Provider", "Activity"))
            for (loc in locations) {
                appendLine(
                    dataRow(
                        num(loc.date),
                        num(loc.lat),
                        num(loc.lng),
                        num(loc.accuracy),
                        num(loc.speed),
                        str(loc.provider),
                        str(loc.activity),
                    ),
                )
            }
            appendLine("    </Table>")
            appendLine("  </Worksheet>")

            append("</Workbook>")
        }

    private fun headerRow(vararg headers: String): String =
        "      <Row>" + headers.joinToString("") { """<Cell><Data ss:Type="String">${xml(it)}</Data></Cell>""" } + "</Row>"

    private fun dataRow(vararg cells: String): String = "      <Row>" + cells.joinToString("") + "</Row>"

    private fun str(v: String): String = """<Cell><Data ss:Type="String">${xml(v)}</Data></Cell>"""

    private fun num(v: Number): String = """<Cell><Data ss:Type="Number">$v</Data></Cell>"""

    /** Escape the five XML predefined entities so cell text can't break the document. */
    private fun xml(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
