package com.mileway.core.data.model.validator

import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.SavedTrack

/** Blocker vs advisory weight for a [JourneyValidationError]. */
enum class JourneySeverity { ERROR, WARNING, INFO }

/**
 * Taxonomy of journey-validation findings (parity spec §2.5 / gap-inventory). Not every code is
 * wired to a check yet — DUPLICATE_JOURNEY/PERFORMANCE_HINT/OPTIMIZATION_AVAILABLE are reserved
 * for checks a future task adds (route de-dupe, perf telemetry) — the enum is the stable contract
 * callers switch on.
 */
enum class JourneyErrorCode {
    ALREADY_SUBMITTED,
    ALREADY_COMPLETED,
    INVALID_TOKEN,
    MISSING_CRITICAL_DATA,
    INVALID_TIME_RANGE,
    INVALID_LOCATION,
    DUPLICATE_JOURNEY,
    DATA_DISCREPANCY,
    STALE_DATA,
    UNUSUAL_VALUES,
    INCONSISTENT_STATE,
    PERFORMANCE_HINT,
    OPTIMIZATION_AVAILABLE,
}

data class JourneyValidationError(
    val code: JourneyErrorCode,
    val message: String,
    val severity: JourneySeverity,
    val metadata: Map<String, String> = emptyMap(),
)

data class JourneyValidationResult(
    val isValid: Boolean,
    val errors: List<JourneyValidationError> = emptyList(),
) {
    fun hasBlockers(): Boolean = errors.any { it.severity == JourneySeverity.ERROR }

    fun hasWarnings(): Boolean = errors.any { it.severity == JourneySeverity.WARNING }

    companion object {
        fun valid() = JourneyValidationResult(isValid = true)

        fun of(errors: List<JourneyValidationError>) =
            JourneyValidationResult(
                isValid = errors.none { it.severity == JourneySeverity.ERROR },
                errors = errors,
            )
    }
}

/**
 * Pure commonMain journey-validation engine — no Android/Java imports. Enforces the invariants
 * [SavedTrack] and [CurrentTrackData] carry as plain fields but nothing else checks today:
 * submission blockers, restoration safety, and DataStore/DB ("ghost journey") consistency.
 */
object JourneyValidator {
    private const val MISSING_VEHICLE_TYPE = "NONE"

    /** ponytail: clock skew/timezone edge cases are common on real devices; don't block on them. */
    private const val FUTURE_CLOCK_DRIFT_BUFFER_MS = 24 * 60 * 60 * 1000L
    private const val STALE_RESTORATION_MS = 7 * 24 * 60 * 60 * 1000L

    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0

    private const val PERCENT_MULTIPLIER = 100.0
    private const val DISTANCE_DISCREPANCY_THRESHOLD_PERCENT = 10.0

    /** Fast gate for the common submission blockers — skips warnings/metadata entirely. */
    fun canSubmit(track: SavedTrack): Boolean =
        !track.serverUploaded &&
            track.routeId.isNotBlank() &&
            track.startTime > 0 &&
            track.selectedVehicleType.isNotBlank() &&
            track.selectedVehicleType != MISSING_VEHICLE_TYPE

    /**
     * Full pre-submission validation, cross-checking the live DataStore session ([currentTrack])
     * against the persisted row ([savedTrack]) when one exists.
     */
    fun validateBeforeSubmission(
        currentTrack: CurrentTrackData,
        savedTrack: SavedTrack? = null,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): JourneyValidationResult {
        val errors = mutableListOf<JourneyValidationError>()

        if (currentTrack.token.isBlank()) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INVALID_TOKEN,
                    message = "Invalid or missing journey token",
                    severity = JourneySeverity.ERROR,
                )
        }

        if (savedTrack != null) {
            if (savedTrack.serverUploaded) {
                errors +=
                    JourneyValidationError(
                        code = JourneyErrorCode.ALREADY_SUBMITTED,
                        message = "Journey already submitted to server",
                        severity = JourneySeverity.ERROR,
                        metadata = mapOf("routeId" to savedTrack.routeId),
                    )
            }

            if (savedTrack.isCompleted && !savedTrack.transId.isNullOrEmpty()) {
                errors +=
                    JourneyValidationError(
                        code = JourneyErrorCode.ALREADY_COMPLETED,
                        message = "Journey already completed in database. Txn: ${savedTrack.transId}",
                        severity = JourneySeverity.ERROR,
                        metadata = mapOf("routeId" to savedTrack.routeId, "transId" to savedTrack.transId.orEmpty()),
                    )
            }

            errors += validateTimeRange(savedTrack, nowMs)
            errors += validateLocation(savedTrack)

            if (savedTrack.distance < 0) {
                errors +=
                    JourneyValidationError(
                        code = JourneyErrorCode.UNUSUAL_VALUES,
                        message = "Negative distance detected: ${savedTrack.distance}m",
                        severity = JourneySeverity.WARNING,
                        metadata = mapOf("distance" to savedTrack.distance.toString()),
                    )
            }

            errors += validateDistanceDiscrepancy(currentTrack, savedTrack)

            if (savedTrack.lastSyncedTime > currentTrack.lastSyncedTime) {
                errors +=
                    JourneyValidationError(
                        code = JourneyErrorCode.STALE_DATA,
                        message = "DataStore has stale data. SavedTrack was synced more recently.",
                        severity = JourneySeverity.WARNING,
                        metadata =
                            mapOf(
                                "dataStoreSync" to currentTrack.lastSyncedTime.toString(),
                                "savedTrackSync" to savedTrack.lastSyncedTime.toString(),
                            ),
                    )
            }

            if (savedTrack.selectedVehicleType.isBlank() || savedTrack.selectedVehicleType == MISSING_VEHICLE_TYPE) {
                errors +=
                    JourneyValidationError(
                        code = JourneyErrorCode.MISSING_CRITICAL_DATA,
                        message = "Vehicle type not selected",
                        severity = JourneySeverity.ERROR,
                    )
            }
        }

        return JourneyValidationResult.of(errors)
    }

    private fun validateTimeRange(
        savedTrack: SavedTrack,
        nowMs: Long,
    ): List<JourneyValidationError> {
        val errors = mutableListOf<JourneyValidationError>()

        if (savedTrack.startTime <= 0) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INVALID_TIME_RANGE,
                    message = "Invalid start time: ${savedTrack.startTime}",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("startTime" to savedTrack.startTime.toString()),
                )
        }

        if (savedTrack.startTime > nowMs + FUTURE_CLOCK_DRIFT_BUFFER_MS) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INVALID_TIME_RANGE,
                    message = "Start time is significantly in the future (> 24h)",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("startTime" to savedTrack.startTime.toString(), "now" to nowMs.toString()),
                )
        } else if (savedTrack.startTime > nowMs) {
            // In the future but within the clock-drift buffer — allow it, just flag it.
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.STALE_DATA,
                    message = "Start time is slightly in the future. Please check your device clock.",
                    severity = JourneySeverity.WARNING,
                    metadata = mapOf("startTime" to savedTrack.startTime.toString(), "now" to nowMs.toString()),
                )
        }

        val endTime = if (savedTrack.endTime > 0) savedTrack.endTime else nowMs
        if (endTime < savedTrack.startTime) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INVALID_TIME_RANGE,
                    message = "End time is before start time",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("startTime" to savedTrack.startTime.toString(), "endTime" to endTime.toString()),
                )
        }

        return errors
    }

    private fun validateLocation(savedTrack: SavedTrack): List<JourneyValidationError> {
        val outOfBounds =
            savedTrack.startLatitude < MIN_LATITUDE || savedTrack.startLatitude > MAX_LATITUDE ||
                savedTrack.startLongitude < MIN_LONGITUDE || savedTrack.startLongitude > MAX_LONGITUDE
        if (!outOfBounds) return emptyList()

        return listOf(
            JourneyValidationError(
                code = JourneyErrorCode.INVALID_LOCATION,
                message = "Invalid start location coordinates",
                severity = JourneySeverity.ERROR,
                metadata =
                    mapOf(
                        "lat" to savedTrack.startLatitude.toString(),
                        "lng" to savedTrack.startLongitude.toString(),
                    ),
            ),
        )
    }

    private fun validateDistanceDiscrepancy(
        currentTrack: CurrentTrackData,
        savedTrack: SavedTrack,
    ): List<JourneyValidationError> {
        val discrepancy = kotlin.math.abs(currentTrack.distance - savedTrack.distance)
        val discrepancyPercent = if (savedTrack.distance > 0) (discrepancy / savedTrack.distance) * PERCENT_MULTIPLIER else 0.0
        if (discrepancyPercent <= DISTANCE_DISCREPANCY_THRESHOLD_PERCENT) return emptyList()

        return listOf(
            JourneyValidationError(
                code = JourneyErrorCode.DATA_DISCREPANCY,
                message = "Distance discrepancy between DataStore (${currentTrack.distance}m) and SavedTrack (${savedTrack.distance}m)",
                severity = JourneySeverity.WARNING,
                metadata =
                    mapOf(
                        "dataStoreDistance" to currentTrack.distance.toString(),
                        "savedTrackDistance" to savedTrack.distance.toString(),
                        "discrepancyPercent" to discrepancyPercent.toString(),
                    ),
            ),
        )
    }

    /** Guards restoring a persisted [savedTrack] back into the live tracking session. */
    fun validateForRestoration(
        savedTrack: SavedTrack,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): JourneyValidationResult {
        val errors = mutableListOf<JourneyValidationError>()

        if (savedTrack.isCompleted) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.ALREADY_COMPLETED,
                    message = "Cannot restore completed journey: ${savedTrack.routeId}",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("routeId" to savedTrack.routeId),
                )
        }

        if (savedTrack.serverUploaded) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.ALREADY_SUBMITTED,
                    message = "Cannot restore server-uploaded journey: ${savedTrack.routeId}",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("routeId" to savedTrack.routeId),
                )
        }

        if (savedTrack.routeId.isBlank()) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INVALID_TOKEN,
                    message = "Invalid token for restoration",
                    severity = JourneySeverity.ERROR,
                )
        }

        if (savedTrack.startTime <= 0) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INVALID_TIME_RANGE,
                    message = "Invalid start time: ${savedTrack.startTime}",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("startTime" to savedTrack.startTime.toString()),
                )
        }

        val ageMs = nowMs - savedTrack.startTime
        if (ageMs > STALE_RESTORATION_MS) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.STALE_DATA,
                    message = "Journey is stale (older than 7 days). Consider discarding.",
                    severity = JourneySeverity.WARNING,
                    metadata = mapOf("ageMs" to ageMs.toString(), "startTime" to savedTrack.startTime.toString()),
                )
        }

        return JourneyValidationResult.of(errors)
    }

    /**
     * A ghost journey is one the live session shows as active while the persisted row disagrees.
     * Two independent tells: the row is already marked completed, or it already carries a
     * transaction id (submitted) — either means tracking should not still be "on".
     */
    fun detectGhostJourney(
        currentTrack: CurrentTrackData?,
        savedTrack: SavedTrack?,
    ): JourneyValidationResult {
        if (currentTrack == null || !currentTrack.isTracking || savedTrack == null) return JourneyValidationResult.valid()

        val errors = mutableListOf<JourneyValidationError>()

        if (savedTrack.isCompleted) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INCONSISTENT_STATE,
                    message = "Ghost journey detected: DataStore shows tracking but SavedTrack shows completed",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("token" to currentTrack.token, "routeId" to savedTrack.routeId),
                )
        }

        if (!savedTrack.transId.isNullOrEmpty()) {
            errors +=
                JourneyValidationError(
                    code = JourneyErrorCode.INCONSISTENT_STATE,
                    message = "Inconsistent state: tracking active but journey already has a transaction id",
                    severity = JourneySeverity.ERROR,
                    metadata = mapOf("token" to currentTrack.token, "transId" to savedTrack.transId.orEmpty()),
                )
        }

        return JourneyValidationResult.of(errors)
    }
}
