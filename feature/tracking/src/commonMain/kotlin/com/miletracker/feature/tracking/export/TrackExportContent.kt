package com.miletracker.feature.tracking.export

import com.miletracker.core.data.model.db.HardwareEvent
import com.miletracker.core.data.model.db.LocationData
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.feature.tracking.ui.components.ExportFormat

/** P-E.2: KMP-safe content builder (no Context, no Intent). Android share wiring via ShareSheet. */
object TrackExportContent {
    fun build(
        format: ExportFormat,
        track: SavedTrack,
        locations: List<LocationData>,
        events: List<HardwareEvent>,
    ): String =
        when (format) {
            ExportFormat.CSV -> CsvExporter.export(track, locations, events)
            ExportFormat.GPX -> GpxExporter.export(track, locations, events)
            ExportFormat.KML -> KmlExporter.export(track, locations, events)
            ExportFormat.GEOJSON -> GeoJsonExporter.export(track, locations, events)
            ExportFormat.JSON -> JsonExporter.export(track, locations, events)
        }
}
