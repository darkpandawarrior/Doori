package com.mileway.feature.tracking.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.feature.tracking.ui.components.ExportFormat
import java.io.File

/**
 * Writes the exported String to a cache file, then fires an ACTION_SEND share Intent.
 *
 * The caller is responsible for starting the Intent (pass it to startActivity).
 */
object TrackExportManager {
    /**
     * Build the export content string for the given format.
     * Pure function, safe to call from a background coroutine.
     */
    fun buildContent(
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

    /**
     * Write [content] to a cache file and return a chooser Intent ready to pass to
     * [android.app.Activity.startActivity].
     *
     * FileProvider authority must be declared in AndroidManifest.xml as
     *   "${applicationId}.fileprovider"
     */
    fun buildShareIntent(
        context: Context,
        format: ExportFormat,
        trackName: String,
        content: String,
    ): Intent {
        val sanitized = trackName.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        val fileName = "$sanitized.${format.fileExtension}"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, fileName)
        outFile.writeText(content)

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile,
            )

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Track export: $trackName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        return Intent.createChooser(shareIntent, "Share track as ${format.displayName}")
    }
}
