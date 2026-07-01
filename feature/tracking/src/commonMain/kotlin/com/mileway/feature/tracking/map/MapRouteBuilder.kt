package com.mileway.feature.tracking.map

import com.mileway.core.data.model.db.LocationData

/**
 * Pure-Kotlin helper that derives everything the map needs from a raw
 * [List<LocationData>].  No Android or osmdroid imports, fully unit-testable
 * on the JVM.
 */
object MapRouteBuilder {
    // -------------------------------------------------------------------
    // Output types
    // -------------------------------------------------------------------

    /** A geographic coordinate without any Android or osmdroid dependency. */
    data class LatLng(val lat: Double, val lng: Double)

    /**
     * Axis-aligned bounding box for the route.
     *
     * All fields are NaN when the route is empty (use [isEmpty] to check).
     */
    data class RouteBounds(
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double,
    ) {
        val isEmpty: Boolean
            get() = minLat.isNaN()

        companion object {
            val EMPTY = RouteBounds(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
        }
    }

    /**
     * All map-layer data derived from a list of [LocationData].
     *
     * @param routeCoords    Ordered coordinates for the main polyline (non-abnormal, non-mock points).
     * @param startCoord     First coordinate in chronological order, or null if route is empty.
     * @param endCoord       Last coordinate in chronological order, or null if route is empty.
     * @param filteredCoords Coordinates classified as filtered (mock / paused) points.
     * @param abnormalCoords Coordinates classified as abnormal (GPS spike) points.
     * @param bounds         Bounding box that encompasses *all* points (route + filtered + abnormal).
     */
    data class RouteMapData(
        val routeCoords: List<LatLng>,
        val startCoord: LatLng?,
        val endCoord: LatLng?,
        val filteredCoords: List<LatLng>,
        val abnormalCoords: List<LatLng>,
        val bounds: RouteBounds,
    )

    // -------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------

    /**
     * Build [RouteMapData] from a raw [List<LocationData>].
     *
     * Classification rules:
     * - **abnormal**, [LocationData.isAbnormal] is true → drawn in red
     * - **filtered**, [LocationData.isMock] or [LocationData.isPaused] → drawn in orange
     * - **normal**, everything else → drawn on the main blue polyline
     *
     * Ordering: points are used in list order (which is insertion/timestamp order
     * from the repository). Start = first normal point; end = last normal point.
     * If there are no normal points, start/end fall back to the first/last point
     * overall.
     */
    fun build(points: List<LocationData>): RouteMapData {
        if (points.isEmpty()) {
            return RouteMapData(
                routeCoords = emptyList(),
                startCoord = null,
                endCoord = null,
                filteredCoords = emptyList(),
                abnormalCoords = emptyList(),
                bounds = RouteBounds.EMPTY,
            )
        }

        val routeCoords = mutableListOf<LatLng>()
        val filteredCoords = mutableListOf<LatLng>()
        val abnormalCoords = mutableListOf<LatLng>()

        for (p in points) {
            val coord = LatLng(p.lat, p.lng)
            when {
                p.isAbnormal -> abnormalCoords.add(coord)
                p.isMock || p.isPaused -> filteredCoords.add(coord)
                else -> routeCoords.add(coord)
            }
        }

        // Start/end from the main route; fall back to full list if route is empty.
        val startCoord =
            routeCoords.firstOrNull()
                ?: points.first().let { LatLng(it.lat, it.lng) }
        val endCoord =
            routeCoords.lastOrNull()
                ?: points.last().let { LatLng(it.lat, it.lng) }

        // Bounding box across ALL points so the viewport always covers everything.
        val allCoords: List<LatLng> =
            buildList {
                addAll(routeCoords)
                addAll(filteredCoords)
                addAll(abnormalCoords)
            }
        val bounds = computeBounds(allCoords)

        return RouteMapData(
            routeCoords = routeCoords,
            startCoord = startCoord,
            endCoord = endCoord,
            filteredCoords = filteredCoords,
            abnormalCoords = abnormalCoords,
            bounds = bounds,
        )
    }

    // -------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------

    /** Compute the min/max bounding box for [coords]. Returns [RouteBounds.EMPTY] when empty. */
    fun computeBounds(coords: List<LatLng>): RouteBounds {
        if (coords.isEmpty()) return RouteBounds.EMPTY
        var minLat = coords[0].lat
        var maxLat = coords[0].lat
        var minLng = coords[0].lng
        var maxLng = coords[0].lng
        for (c in coords) {
            if (c.lat < minLat) minLat = c.lat
            if (c.lat > maxLat) maxLat = c.lat
            if (c.lng < minLng) minLng = c.lng
            if (c.lng > maxLng) maxLng = c.lng
        }
        return RouteBounds(minLat = minLat, maxLat = maxLat, minLng = minLng, maxLng = maxLng)
    }
}
