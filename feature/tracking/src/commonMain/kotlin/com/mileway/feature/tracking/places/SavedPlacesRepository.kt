package com.mileway.feature.tracking.places

import com.mileway.core.data.dao.FavouriteRouteDao
import com.mileway.core.data.dao.SavedPlaceDao
import com.mileway.core.data.model.db.FavouriteRouteEntity
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.util.haversineMeters
import com.mileway.core.network.routing.RoutePoint
import com.mileway.feature.tracking.export.RedactionDefaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** HOME is a protected system place derived from existing schema data, never a new Room column. */
val SavedPlaceEntity.isProtected: Boolean get() = type == "HOME"

/** Existing saved_places and favourite_routes persistence, with a privacy boundary for external use. */
class SavedPlacesRepository(
    private val places: SavedPlaceDao,
    private val routes: FavouriteRouteDao,
) {
    val savedPlaces = places.observeAll()
    val favouriteRoutes = routes.observeAll()
    val approverPlaces = savedPlaces.map { rows -> rows.filterNot { it.isProtected } }

    suspend fun save(place: SavedPlaceEntity) {
        require(place.id.isNotBlank() && place.label.isNotBlank())
        require(place.type in setOf("HOME", "WORK", "OTHER"))
        require((place.latitude == null) == (place.longitude == null))
        val latitude = place.latitude
        val longitude = place.longitude
        if (latitude != null && longitude != null) RoutePoint(latitude, longitude)
        val old = savedPlaces.first().firstOrNull { it.id == place.id }
        require(old?.isProtected != true || place.type == "HOME") { "Home cannot be retyped" }
        val home = savedPlaces.first().firstOrNull { it.isProtected }
        require(place.type != "HOME" || home == null || home.id == place.id) { "Update the existing Home system place" }
        places.upsert(place)
    }

    suspend fun delete(id: String) {
        require(savedPlaces.first().none { it.id == id && it.isProtected }) { "Home is a protected system place" }
        places.delete(id)
    }

    /** Raw coordinate entry near Home receives the same protection as selecting the Home row. */
    suspend fun routePoint(place: SavedPlaceEntity): RoutePoint? {
        val lat = place.latitude ?: return null
        val lng = place.longitude ?: return null
        val nearHome =
            savedPlaces.first().any {
                it.isProtected &&
                    it.latitude != null &&
                    it.longitude != null &&
                    haversineMeters(lat, lng, it.latitude, it.longitude) <= RedactionDefaults.HOME_RADIUS_METERS
            }
        return RoutePoint(lat, lng, place.isProtected || nearHome)
    }

    suspend fun pin(route: FavouriteRouteEntity) {
        require(route.id.isNotBlank() && route.sourceTrackId.isNotBlank() && route.name.isNotBlank())
        require(route.distanceKm.isFinite() && route.distanceKm > 0.0)
        routes.upsert(route)
    }

    suspend fun unpin(id: String) = routes.delete(id)
}
