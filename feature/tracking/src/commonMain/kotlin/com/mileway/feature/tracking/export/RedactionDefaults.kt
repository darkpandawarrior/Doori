package com.mileway.feature.tracking.export

import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.util.haversineMeters

/** Privacy defaults shared by all track export formats. Saved data itself is never modified. */
object RedactionDefaults {
    const val HOME_RADIUS_METERS = 200.0
    const val HOME_TYPE = "HOME"

    /** Removes points at or within the Home radius. Address-only Home rows retain all points. */
    fun locations(
        points: List<LocationData>,
        home: SavedPlaceEntity?,
        enabled: Boolean = true,
    ): List<LocationData> =
        if (!enabled || home == null) {
            points
        } else {
            points.filterNot { nearHome(it.lat, it.lng, home) }.map { point ->
                point.copy(reason = point.reason?.let { address(it, home) })
            }
        }

    /** Home address text becomes the Home label, including addresses embedded in event text. */
    fun address(
        text: String,
        home: SavedPlaceEntity?,
        enabled: Boolean = true,
    ): String {
        if (!enabled || home?.type != HOME_TYPE) return text
        return if (home.address.isNotBlank()) text.replace(home.address, "Home") else text
    }

    /** Scrubs event coordinates too, since JSON/CSV can export them independently of track points. */
    fun events(
        events: List<HardwareEvent>,
        home: SavedPlaceEntity?,
        enabled: Boolean = true,
    ): List<HardwareEvent> =
        if (!enabled || home == null) {
            events
        } else {
            events.map { event ->
                event.copy(
                    event = address(event.event, home),
                    lat = event.lat.takeUnless { nearHome(event.lat, event.lng, home) },
                    lng = event.lng.takeUnless { nearHome(event.lat, event.lng, home) },
                )
            }
        }

    /** JSON exports endpoints from track metadata; replace hidden endpoints with retained points. */
    fun track(
        track: SavedTrack,
        points: List<LocationData>,
        home: SavedPlaceEntity?,
        enabled: Boolean = true,
    ): SavedTrack {
        if (!enabled || home == null) return track
        val hideStart = nearHome(track.startLatitude, track.startLongitude, home)
        val hideEnd = nearHome(track.endLatitude, track.endLongitude, home)
        return track.copy(
            name = address(track.name, home),
            startLatitude = if (hideStart) points.firstOrNull()?.lat ?: 0.0 else track.startLatitude,
            startLongitude = if (hideStart) points.firstOrNull()?.lng ?: 0.0 else track.startLongitude,
            endLatitude = if (hideEnd) points.lastOrNull()?.lat ?: 0.0 else track.endLatitude,
            endLongitude = if (hideEnd) points.lastOrNull()?.lng ?: 0.0 else track.endLongitude,
        )
    }

    private fun nearHome(
        lat: Double?,
        lng: Double?,
        home: SavedPlaceEntity,
    ): Boolean {
        if (home.type != HOME_TYPE) return false
        val homeLat = home.latitude ?: return false
        val homeLng = home.longitude ?: return false
        if (lat == null || lng == null) return false
        return haversineMeters(lat, lng, homeLat, homeLng) <= HOME_RADIUS_METERS
    }
}
