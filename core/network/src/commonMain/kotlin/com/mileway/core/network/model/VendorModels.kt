package com.mileway.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Vendor directory ──────────────────────────────────────────────────────────

/**
 * A vendor / partner center that journeys can start from, end at, or check in against.
 * Coordinates are paired with a geofence radius for local proximity validation.
 */
@Serializable
data class VendorCenter(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("address") val address: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lng") val lng: Double = 0.0,
    @SerialName("radiusMeters") val radiusMeters: Double = 100.0,
)

// ── Frequent routes ───────────────────────────────────────────────────────────

/**
 * A frequently travelled route surfaced as a one-tap suggestion when logging miles.
 */
@Serializable
data class FrequentRoute(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String,
    @SerialName("fromName") val fromName: String,
    @SerialName("toName") val toName: String,
    @SerialName("distanceKm") val distanceKm: Double = 0.0,
    @SerialName("timesUsed") val timesUsed: Int = 0,
)
