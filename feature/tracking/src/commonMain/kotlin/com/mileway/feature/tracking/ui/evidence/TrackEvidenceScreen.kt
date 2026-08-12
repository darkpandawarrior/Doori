package com.mileway.feature.tracking.ui.evidence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.ui.components.DistanceLedger
import com.mileway.core.ui.components.DistanceLedgerBar
import com.mileway.core.ui.detail.DetailField
import com.mileway.core.ui.detail.DetailScreen
import com.mileway.core.ui.detail.DetailSpec
import com.mileway.core.ui.detail.buildDetail
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.previews.PreviewSurface
import com.mileway.core.ui.theme.DesignTokens
import com.siddharth.kmp.common.UiText
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.round

/**
 * The surface that makes a reimbursement claim defensible months later: renders [SavedTrack] as a
 * [DetailSpec] through the existing [DetailScreen] renderer (P28.DETAIL.1) — no bespoke composable,
 * everything expressed through the ten-type [DetailField] set plus [DistanceLedgerBar] via
 * [DetailField.Slot]. Stateless by design: the caller (navigation) owns fetching the [SavedTrack].
 */
@Composable
fun TrackEvidenceScreen(
    track: SavedTrack,
    modifier: Modifier = Modifier,
) {
    DetailScreen(spec = trackEvidenceDetailSpec(track), modifier = modifier)
}

/**
 * Pure spec builder — every value is read from an actual [SavedTrack] column, never invented.
 * Where the entity genuinely has no data for something the reference brief asks for (edit history,
 * a structured trip purpose, a per-fix accuracy distribution), the corresponding row says so
 * honestly instead of fabricating a source. See TASK_NOTES in the PR description for the full list.
 */
fun trackEvidenceDetailSpec(track: SavedTrack): DetailSpec =
    buildDetail {
        section(id = "ledger", title = UiText.of("Distance ledger")) {
            slot(id = "ledger_bar") { DistanceLedgerBar(ledger = track.toDistanceLedger()) }
        }

        section(id = "provenance", title = UiText.of("Provenance")) {
            keyValue(
                id = "provenance_algorithm",
                label = UiText.of("Algorithm"),
                value = UiText.of(if (track.locationV2) "Smart Distance V2 (cleaned pipeline)" else "Legacy V1 (raw pipeline)"),
            )
            keyValue(
                id = "provenance_tuning",
                label = UiText.of("Tuning profile"),
                value =
                    UiText.of(
                        "min ${track.minimumTrackerDistance.toInt()} m / " +
                            "${track.minimumTrackerTime / 1000} s, max ${track.maximumTrackerTime / 1000} s",
                    ),
            )
            keyValue(
                id = "provenance_app_version",
                label = UiText.of("App version"),
                value = UiText.of(rangeText(track.startAppVersion, track.endAppVersion)),
            )
            keyValue(
                id = "provenance_device",
                label = UiText.of("Device"),
                value = UiText.of(rangeText(track.startDeviceVersion, track.endDeviceVersion)),
            )
        }

        section(id = "capture_quality", title = UiText.of("Capture quality")) {
            metricGrid(
                "capture_metrics",
                DetailField.MetricGrid.Metric(UiText.of("Fix count"), UiText.of(track.totalLocationPoints.toString()), Icons.Default.Place),
                DetailField.MetricGrid.Metric(UiText.of("Mean interval"), UiText.of(meanSamplingInterval(track)), Icons.Default.Timer),
            )
            keyValue(
                id = "capture_accuracy",
                label = UiText.of("Last fix accuracy"),
                value =
                    UiText.of(
                        if (track.lastLocationAccuracy >= 0.0) {
                            "±${fmt1(track.lastLocationAccuracy)} m (last fix only — SavedTrack has no per-fix accuracy history)"
                        } else {
                            "Not recorded"
                        },
                    ),
            )
            keyValue(
                id = "capture_gaps",
                label = UiText.of("Paused / inactive time"),
                value = UiText.of("${track.trackerPausedTimeMins} min paused, ${track.trackerInactivityTimeMins} min inactive"),
            )
            keyValue(
                id = "capture_interruptions",
                label = UiText.of("Interruptions"),
                value = UiText.of(interruptionsSummary(track)),
            )
        }

        section(id = "evidence", title = UiText.of("Evidence")) {
            val photos = odometerAttachments(track)
            attachments(
                id = "evidence_attachments",
                title = UiText.of("Odometer photos"),
                attachments = photos,
                visible = photos.isNotEmpty(),
            )
            keyValue(
                id = "evidence_none",
                label = UiText.of("Odometer photos"),
                value = UiText.of("None captured for this trip."),
                visible = photos.isEmpty(),
            )
            keyValue(
                id = "evidence_odometer_fallback",
                label = UiText.of("Odometer status"),
                value = UiText.of("Marked not working — expense sourced from GPS distance instead."),
                visible = track.odometerNotWorking,
            )
        }

        section(id = "edit_history", title = UiText.of("Edit history")) {
            keyValue(
                id = "edit_history_status",
                label = UiText.of("Changes since capture"),
                value = UiText.of("No edits recorded — SavedTrack carries no audit-trail field today."),
            )
        }

        section(id = "compliance", title = UiText.of("Compliance")) {
            date(
                id = "compliance_date",
                label = UiText.of("Date"),
                value = UiText.of(formatDate(track.journeyDate?.takeIf { it > 0L } ?: track.createdAt)),
            )
            keyValue(
                id = "compliance_start",
                label = UiText.of("Start"),
                value = UiText.of("${formatDateTime(track.startTime)} · ${formatCoord(track.startLatitude, track.startLongitude)}"),
            )
            keyValue(
                id = "compliance_end",
                label = UiText.of("End"),
                value = UiText.of("${formatDateTime(track.endTime)} · ${formatCoord(track.endLatitude, track.endLongitude)}"),
            )
            keyValue(
                id = "compliance_purpose",
                label = UiText.of("Purpose"),
                value =
                    UiText.of(
                        "Not recorded — SavedTrack has no purpose column; it exists only inside an " +
                            "unparsed submission-time form blob (processorFormDataJson).",
                    ),
            )
            amount(
                id = "compliance_distance",
                label = UiText.of("Claimed distance"),
                value = UiText.of("${fmt2(track.distance / 1000.0)} km"),
            )
        }
    }

internal fun SavedTrack.toDistanceLedger(): DistanceLedger =
    DistanceLedger(
        rawKm = originalDistance / 1000.0,
        cleanedKm = (if (cleanedDistance > 0.0) cleanedDistance else distance) / 1000.0,
        claimedKm = distance / 1000.0,
        abnormalKm = abnormalDistance / 1000.0,
        mockKm = mockDistance / 1000.0,
        spikeKm = spikeDistance / 1000.0,
        odometerKm = (odometerDistance / 1000.0).takeIf { useOdometer && odometerDistance > 0.0 },
    )

private fun odometerAttachments(track: SavedTrack): List<DetailField.AttachmentList.Attachment> =
    buildList {
        if (track.odometerStartUrl.isNotBlank()) {
            add(
                DetailField.AttachmentList.Attachment(
                    label = UiText.of(odometerLabel("Odo start", track.odometerStartOcr, track.odometerStartPhotoTime)),
                    contentDescription = UiText.of("Odometer photo at trip start"),
                ) { OdometerThumbnail(track.odometerStartUrl) },
            )
        }
        if (track.odometerEndUrl.isNotBlank()) {
            add(
                DetailField.AttachmentList.Attachment(
                    label = UiText.of(odometerLabel("Odo end", track.odometerEndOcr, track.odometerEndPhotoTime)),
                    contentDescription = UiText.of("Odometer photo at trip end"),
                ) { OdometerThumbnail(track.odometerEndUrl) },
            )
        }
    }

private fun odometerLabel(
    base: String,
    ocr: String,
    photoTimeMs: Long,
): String {
    val reading = ocr.takeIf { it.isNotBlank() && it != "NA" }
    val time = photoTimeMs.takeIf { it > 0L }?.let { formatDateTime(it) }
    return listOfNotNull(base, reading, time).joinToString(" · ")
}

@Composable
private fun OdometerThumbnail(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .size(80.dp)
                .clip(DesignTokens.Shape.button)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

private fun interruptionsSummary(track: SavedTrack): String {
    val events =
        buildList {
            if (track.wasAppKilled) add("app killed ${track.appKilledCount}x")
            if (track.foregroundServiceTerminated) add("service terminated ${track.foregroundServiceTerminatedCount}x")
            if (track.wasPhoneShutDown) add("phone shut down ${track.phoneShutdownCount}x")
            if (track.wasMockLocationUsed) add("mock location used")
            if (track.wasPermissionsViolated) add("permissions violated")
        }
    return events.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None recorded"
}

private fun meanSamplingInterval(track: SavedTrack): String {
    if (track.totalLocationPoints <= 0L || track.duration <= 0L) return "—"
    val seconds = (track.duration / 1000.0) / track.totalLocationPoints
    return "${fmt1(seconds)} s"
}

private fun rangeText(
    start: String,
    end: String,
): String =
    when {
        start.isBlank() && end.isBlank() -> "—"
        start == end -> start
        start.isBlank() -> end
        end.isBlank() -> start
        else -> "$start → $end"
    }

private fun formatCoord(
    lat: Double,
    lng: Double,
): String = "${fmt4(lat)}, ${fmt4(lng)}"

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return "—"
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${pad(dt.monthNumber)}-${pad(dt.dayOfMonth)}"
}

private fun formatDateTime(millis: Long): String {
    if (millis <= 0L) return "—"
    val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${pad(dt.monthNumber)}-${pad(dt.dayOfMonth)} ${pad(dt.hour)}:${pad(dt.minute)}"
}

private fun pad(n: Int): String = n.toString().padStart(2, '0')

// Manual round-and-split formatting, matching DistanceLedgerBar's own private helpers — no
// String.format (not safe across every KMP target) and no new dependency.
private fun fmt1(v: Double): String {
    val r = round(v * 10.0) / 10.0
    val whole = r.toLong()
    val frac = round(abs(r - whole) * 10.0).toLong()
    return "$whole.$frac"
}

private fun fmt2(v: Double): String {
    val r = round(v * 100.0) / 100.0
    val whole = r.toLong()
    val frac = round(abs(r - whole) * 100.0).toLong()
    return "$whole.${frac.toString().padStart(2, '0')}"
}

private fun fmt4(v: Double): String {
    val r = round(v * 10000.0) / 10000.0
    val whole = r.toLong()
    val frac = round(abs(r - whole) * 10000.0).toLong()
    return "$whole.${frac.toString().padStart(4, '0')}"
}

internal fun sampleEvidenceTrack(): SavedTrack =
    SavedTrack(
        routeId = "route-preview",
        name = "Preview trip",
        isCompleted = true,
        startedByEmployeeCode = "E123",
        startLatitude = 18.5204,
        startLongitude = 73.8567,
        endLatitude = 18.5304,
        endLongitude = 73.8667,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        startTime = 1_754_000_000_000L,
        endTime = 1_754_003_600_000L,
        distance = 12_400.0,
        duration = 3_600_000L,
        createdAt = 1_754_000_000_000L,
        avgSpeed = 34.5,
        maxSpeed = 62.0,
        originalDistance = 13_100.0,
        cleanedDistance = 12_400.0,
        mockDistance = 200.0,
        abnormalDistance = 400.0,
        spikeDistance = 100.0,
        odometerDistance = 12_600.0,
        useOdometer = true,
        totalLocationPoints = 720L,
        lastLocationAccuracy = 8.5,
        trackerPausedTimeMins = 4L,
        trackerInactivityTimeMins = 1L,
        odometerStartUrl = "https://example.invalid/odo-start.jpg",
        odometerEndUrl = "https://example.invalid/odo-end.jpg",
        odometerStartOcr = "10234",
        odometerEndOcr = "10247",
        odometerStartPhotoTime = 1_754_000_000_000L,
        odometerEndPhotoTime = 1_754_003_600_000L,
        startAppVersion = "3.4.0",
        endAppVersion = "3.4.0",
        startDeviceVersion = "Android 14",
        endDeviceVersion = "Android 14",
        locationV2 = true,
    )

@PreviewLightDark
@Composable
private fun TrackEvidenceScreenPreview() {
    PreviewSurface {
        TrackEvidenceScreen(track = sampleEvidenceTrack())
    }
}
