package com.mileway.feature.tracking.route

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.util.haversineMeters
import com.mileway.core.network.routing.RoutePoint

/** A recorded return leg takes precedence over generating another return distance. No rows are merged or deleted. */
object RoundTripGuard {
    private const val PAIR_WINDOW_MS = 24L * 60 * 60 * 1_000
    private const val ENDPOINT_RADIUS_METRES = 200.0

    fun hasRecordedReturn(origin: RoutePoint, destination: RoutePoint, atMillis: Long, tracks: List<SavedTrack>): Boolean =
        tracks.any { track ->
            track.isCompleted && !track.isDraft && !track.isDiscarded && track.endTime <= atMillis &&
                track.endTime >= atMillis - PAIR_WINDOW_MS && track.distance.isFinite() && track.distance > 0.0 &&
                haversineMeters(destination.latitude, destination.longitude, track.startLatitude, track.startLongitude) <= ENDPOINT_RADIUS_METRES &&
                haversineMeters(origin.latitude, origin.longitude, track.endLatitude, track.endLongitude) <= ENDPOINT_RADIUS_METRES
        }
}
