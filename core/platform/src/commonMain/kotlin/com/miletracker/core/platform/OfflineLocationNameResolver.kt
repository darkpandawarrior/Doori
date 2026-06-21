package com.miletracker.core.platform

import kotlin.math.PI
import kotlin.math.cos

/**
 * A fully offline [LocationNameResolver] that maps coordinates to plausible named waypoints
 * **without any network**. This backs the offline-first demo: the simulated drive emits a
 * deterministic stream of coordinates around Pune, and this resolver turns each fix into a
 * real-looking place name so live tracking reads "Koregaon Park, Pune" instead of raw decimals.
 *
 * Resolution is purely geometric: the resolver holds a small gazetteer of named Pune waypoints
 * and returns the nearest one (by squared planar distance, longitude scaled by cos(lat) so the
 * comparison stays fair at this latitude). Identical coordinates always map to the same name, so
 * it is deterministic and reproducible in screenshot tests.
 *
 * Anything outside a sane radius of the gazetteer (≈ the Pune metro) falls back to formatted
 * coordinates — the resolver never invents a name for a coordinate it has no basis to label.
 */
class OfflineLocationNameResolver(
    private val waypoints: List<Waypoint> = PUNE_WAYPOINTS,
    /** Maximum great-circle-ish distance (degrees) to accept a named match before falling back. */
    private val maxMatchDegrees: Double = 0.18,
) : LocationNameResolver {
    /** A named place in the offline gazetteer. */
    data class Waypoint(
        val latitude: Double,
        val longitude: Double,
        val name: String,
    )

    override suspend fun resolve(
        latitude: Double,
        longitude: Double,
    ): PlaceName {
        val nearest =
            waypoints.minByOrNull { wp ->
                squaredDistance(latitude, longitude, wp.latitude, wp.longitude)
            } ?: return PlaceName.coordinatesOnly(latitude, longitude)

        val dist = kotlin.math.sqrt(squaredDistance(latitude, longitude, nearest.latitude, nearest.longitude))
        return if (dist <= maxMatchDegrees) {
            PlaceName(name = nearest.name, coordinates = PlaceName.formatCoordinates(latitude, longitude))
        } else {
            PlaceName.coordinatesOnly(latitude, longitude)
        }
    }

    private fun squaredDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val dLat = lat1 - lat2
        // Scale longitude by cos(latitude) so a degree of longitude is weighted like it is on the
        // ground at this latitude; otherwise east–west distances would be overstated near Pune.
        val dLng = (lng1 - lng2) * cos(((lat1 + lat2) / 2.0) * PI / 180.0)
        return dLat * dLat + dLng * dLng
    }

    companion object {
        /**
         * Real Pune localities laid out roughly where they sit on the map, so a simulated drive
         * heading north-east from the city centre passes through a believable sequence
         * (Shivajinagar → Koregaon Park → Kalyani Nagar → Viman Nagar → Kharadi …).
         */
        val PUNE_WAYPOINTS: List<Waypoint> =
            listOf(
                Waypoint(18.5204, 73.8567, "Pune City Centre"),
                Waypoint(18.5308, 73.8475, "Shivajinagar, Pune"),
                Waypoint(18.5167, 73.8563, "Camp, Pune"),
                Waypoint(18.5362, 73.8939, "Koregaon Park, Pune"),
                Waypoint(18.5479, 73.9010, "Kalyani Nagar, Pune"),
                Waypoint(18.5679, 73.9143, "Viman Nagar, Pune"),
                Waypoint(18.5515, 73.9436, "Kharadi, Pune"),
                Waypoint(18.5089, 73.9260, "Hadapsar, Pune"),
                Waypoint(18.5018, 73.9280, "Magarpatta City, Pune"),
                Waypoint(18.5645, 73.7769, "Hinjewadi, Pune"),
                Waypoint(18.5993, 73.7666, "Wakad, Pune"),
                Waypoint(18.5590, 73.7868, "Baner, Pune"),
                Waypoint(18.5626, 73.8077, "Aundh, Pune"),
                Waypoint(18.5074, 73.8077, "Kothrud, Pune"),
                Waypoint(18.5018, 73.8636, "Swargate, Pune"),
                Waypoint(18.5535, 73.8744, "Yerwada, Pune"),
            )
    }
}
