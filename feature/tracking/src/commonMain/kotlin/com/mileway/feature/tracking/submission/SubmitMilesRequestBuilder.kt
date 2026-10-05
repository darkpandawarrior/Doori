package com.mileway.feature.tracking.submission

import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.feature.tracking.checkin.RoundTripClassifier
import com.mileway.feature.tracking.export.RedactionDefaults
import com.mileway.feature.tracking.route.RoundTripGuard
import com.mileway.feature.tracking.viewmodel.SubmissionFormUi

/**
 * PLAN_V33 C5: pure (KMP-clean, no android.* / java.* deps) builder that fills in the
 * [SubmitMilesRequestK] fields `MileageSubmissionViewModel.submit()` previously left at their
 * defaults. Structure only — reimplemented against Mileway's own DTO/domain shapes (see
 * CLAUDE.md's "reference app" section: the field set/transforms are the blueprint, this code
 * and its DTO are Mileway's own).
 *
 * Fields the blueprint populates that have **no** Mileway DTO equivalent are deliberately left
 * unset rather than invented:
 * - a full GPS *polyline* — [SubmitMilesRequestK] only has single-point `origin`/`destination`
 *   (`CoordsV2`), not a coords-list field, so only the trip's first/last point is sent.
 * - `mjpId`/`mjpItemId` — those only exist on `LogMilesDraftEntity` (a different submission
 *   path); [SavedTrack] (the Track Miles trip record) has no MJP-linkage field to source from.
 */
object SubmitMilesRequestBuilder {
    private const val MetresPerKm = 1_000.0

    /** Audit-trail marker recorded when distance is sourced from GPS because the odometer wasn't usable. */
    const val ODOMETER_NOT_WORKING_REMARK = "ODOMETER_NOT_WORKING"

    fun build(
        routeId: String,
        vehicleKey: String,
        distanceKm: Double,
        startTime: Long,
        endTime: Long,
        submissionTime: Long,
        form: SubmissionFormUi,
        track: SavedTrack? = null,
        routePoints: List<LocationData> = emptyList(),
        recordedTracks: List<SavedTrack> = emptyList(),
        homes: List<SavedPlaceEntity> = emptyList(),
    ): SubmitMilesRequestK {
        val odometerFallbackActive = form.config.calculateExpenseViaOdometer && form.odometerNotWorking
        val odometerCaptured = form.simulatedStartOdo != null && form.simulatedEndOdo != null
        // A real odometer source only exists when a reading was captured AND we're not on the
        // GPS-fallback path (fallback means the odometer was declared unusable).
        val hasRealOdometerSource = odometerCaptured && !odometerFallbackActive

        return SubmitMilesRequestK(
            token = routeId,
            vehicleType = vehicleKey,
            distance = distanceKm,
            startTime = startTime,
            endTime = endTime,
            submissionTime = submissionTime,
            odometerNotWorking = odometerFallbackActive,
            // Reference builder appends the marker to violationRemarks, NOT notes.
            violationRemarks = if (odometerFallbackActive) ODOMETER_NOT_WORKING_REMARK else null,
            // A recorded closed loop or paired return is already in the mileage ledger.
            roundTrip = form.roundTrip && !returnAlreadyRecorded(track, recordedTracks, submissionTime),
            startLabel = odometerLabel(hasRealOdometerSource, form.isManualStartOdo),
            endLabel = odometerLabel(hasRealOdometerSource, form.isManualEndOdo),
            startReading = odometerReading(odometerFallbackActive, form.simulatedStartOdo),
            endReading = odometerReading(odometerFallbackActive, form.simulatedEndOdo),
            odometerDistance = if (hasRealOdometerSource) form.smartDistanceOdometerKm else null,
            // Reference semantics preserved as-is (parity, not re-derived): true exactly when the
            // GPS-fallback path was used, i.e. when the odometer itself couldn't be trusted.
            milesAmountByOdometer = odometerFallbackActive,
            origin = approverEndpoint(routePoints.firstOrNull(), homes),
            destination = approverEndpoint(routePoints.lastOrNull(), homes),
            tripId = track?.tripId,
            tripV2Id = track?.tripV2Id,
            // v1/v2 split: prefer the v2 id, fall back to v1 — Mileway's DTO has no separate
            // `itineraryV2Id` field, so the resolved value is written into `itineraryId`.
            itineraryId = track?.tripV2Id?.takeIf { it.isNotBlank() } ?: track?.itineraryId,
            petty = track?.pettyId?.takeIf { it >= 0 },
            officeId = track?.officeId,
            entityId = track?.entityId,
        )
    }

    private fun approverEndpoint(
        point: LocationData?,
        homes: List<SavedPlaceEntity>,
    ): CoordsV2? {
        if (point == null) return null
        val retained = homes.fold(listOf(point)) { points, home -> RedactionDefaults.locations(points, home) }
        return retained.singleOrNull()?.let { CoordsV2(lat = it.lat, lng = it.lng) }
    }

    private fun returnAlreadyRecorded(
        track: SavedTrack?,
        recordedTracks: List<SavedTrack>,
        submittedAtMillis: Long,
    ): Boolean {
        if (track == null) return false
        if (track.roundTrip ||
            RoundTripClassifier.isRoundTrip(
                track.startLatitude,
                track.startLongitude,
                track.endLatitude,
                track.endLongitude,
                track.distance / MetresPerKm,
            )
        ) {
            return true
        }
        return RoundTripGuard.hasRecordedReturnForTrip(track, submittedAtMillis, recordedTracks)
    }

    /** ocr -> "OCR", manual -> "MANUAL", na/no-reading -> null (dropped, not sent as literal "NA"). */
    private fun odometerLabel(
        hasRealOdometerSource: Boolean,
        isManual: Boolean,
    ): String? =
        if (!hasRealOdometerSource) {
            null
        } else if (isManual) {
            "MANUAL"
        } else {
            "OCR"
        }

    /** Zeroed while the odometer fallback is active (the reading is stale/unusable); passed through otherwise. */
    private fun odometerReading(
        fallbackActive: Boolean,
        reading: Int?,
    ): String? = if (fallbackActive) "0" else reading?.toString()
}
