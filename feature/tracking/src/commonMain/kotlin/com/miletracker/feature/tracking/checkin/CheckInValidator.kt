package com.miletracker.feature.tracking.checkin

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin (no Android imports) Haversine-based check-in validator.
 *
 * Given a user's current coordinates and a target check-in location with a configured
 * radius, computes whether the user is within range and, if not, how far outside they are.
 *
 * All validation is done locally; no network calls are made.
 */
object CheckInValidator {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Describes a named check-in location (e.g. supply center, job site).
     *
     * @param id          Unique identifier used for logging.
     * @param name        Human-readable display name.
     * @param lat         Latitude in decimal degrees.
     * @param lng         Longitude in decimal degrees.
     * @param type        Category label (e.g. "SUPPLY_CENTER", "JOB_SITE").
     * @param radiusMeters Per-location radius override; falls back to the default passed at
     *                    call-site if null.
     */
    data class CheckInLocation(
        val id: String,
        val name: String,
        val lat: Double,
        val lng: Double,
        val type: String,
        val radiusMeters: Double? = null,
    )

    /**
     * The result of a single validation call.
     *
     * @param withinRadius      True when [distanceMeters] <= effective radius.
     * @param distanceMeters    Haversine distance from [userLat]/[userLng] to the nearest
     *                         location's coordinates.
     * @param distanceOutside   How far outside the radius the user is, in meters.
     *                         Zero when [withinRadius] is true.
     * @param nearestLocation   The [CheckInLocation] that produced the smallest distance.
     * @param effectiveRadius   The radius (metres) that was used for this validation.
     * @param userLat           Echoed back for callers that need to build a log record.
     * @param userLng           Echoed back for callers that need to build a log record.
     */
    data class ValidationResult(
        val withinRadius: Boolean,
        val distanceMeters: Double,
        val distanceOutside: Double,
        val nearestLocation: CheckInLocation,
        val effectiveRadius: Double,
        val userLat: Double,
        val userLng: Double,
    )

    /**
     * Validates whether [userLat]/[userLng] is within the radius of the nearest location
     * in [candidates].
     *
     * Selects the candidate with the smallest Haversine distance, then tests it against
     * its own per-location radius if set, or [defaultRadiusMeters] otherwise.
     *
     * @param userLat             User's current latitude.
     * @param userLng             User's current longitude.
     * @param candidates          Non-empty list of potential check-in locations.
     * @param defaultRadiusMeters Fallback radius used when a location has no override.
     *
     * @throws IllegalArgumentException if [candidates] is empty.
     */
    fun validate(
        userLat: Double,
        userLng: Double,
        candidates: List<CheckInLocation>,
        defaultRadiusMeters: Double = 100.0,
    ): ValidationResult {
        require(candidates.isNotEmpty()) { "candidates must not be empty" }

        val nearest = candidates.minByOrNull { haversineMeters(userLat, userLng, it.lat, it.lng) }!!
        val distance = haversineMeters(userLat, userLng, nearest.lat, nearest.lng)
        val radius = nearest.radiusMeters ?: defaultRadiusMeters
        val within = distance <= radius
        val outside = if (within) 0.0 else distance - radius

        return ValidationResult(
            withinRadius = within,
            distanceMeters = distance,
            distanceOutside = outside,
            nearestLocation = nearest,
            effectiveRadius = radius,
            userLat = userLat,
            userLng = userLng,
        )
    }

    /**
     * Haversine formula, returns the great-circle distance in metres between two
     * geographic coordinates.
     */
    fun haversineMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLng = (lng2 - lng1) * PI / 180.0
        val a =
            sin(dLat / 2).pow(2) +
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLng / 2).pow(2)
        return EARTH_RADIUS_METERS * 2 * asin(sqrt(a))
    }

    /**
     * Builds the human-readable radius warning shown on the override sheet.
     */
    fun buildOutsideRadiusMessage(result: ValidationResult): String =
        "You are ${result.distanceOutside.toInt()} m outside the check-in radius of " +
            "${result.effectiveRadius.toInt()} m for \"${result.nearestLocation.name}\". " +
            "Move closer and try again."
}
