package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P12.8: a favourite route — a completed track the user has pinned and named, with a
 * quick-start classification default (`purpose`) carried over from the source trip. Multi-row.
 * [sourceTrackId] is the `SavedTrack.routeId` it was pinned from; [distanceKm] is a display cache.
 */
@Entity(tableName = "favourite_routes")
data class FavouriteRouteEntity(
    @PrimaryKey
    val id: String,
    val sourceTrackId: String,
    val name: String,
    val purpose: String,
    val distanceKm: Double,
    val createdAtMs: Long,
)
