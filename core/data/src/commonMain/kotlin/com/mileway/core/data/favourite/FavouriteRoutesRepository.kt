package com.mileway.core.data.favourite

import com.mileway.core.data.dao.FavouriteRouteDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.FavouriteRouteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** A favourite route the user pinned from a completed trip. */
data class FavouriteRoute(
    val id: String,
    val sourceTrackId: String,
    val name: String,
    val purpose: String,
    val distanceKm: Double,
)

/** A completed trip that can be pinned as a favourite route (not yet pinned). */
data class PinnableTrack(
    val trackId: String,
    val name: String,
    val purpose: String,
    val distanceKm: Double,
)

/**
 * PLAN_V24 P12.8: the favourite-routes store. Lives in core:data (reads [SavedTrackDao] directly,
 * like [com.mileway.core.data.engagement.BadgeRepository]) so the profile hub pins routes without
 * depending on feature:tracking. Pure/offline over Room. `SavedTrack.distance` is metres → km, the
 * same conversion the badge board uses (tracking units preserved).
 *
 * ponytail: favourite ROUTES only — the reference "blocked zones" half is skipped because it needs
 * real geofence wiring into the live auto-track engine (cross-module, no cheap seam today); noted in
 * PROGRESS. The stored [FavouriteRoute.purpose] is the quick-start classification default; consuming
 * it to actually pre-seed a new trip is a cross-module hook that lands when tracking exposes one.
 */
class FavouriteRoutesRepository(
    private val favouriteDao: FavouriteRouteDao,
    private val savedTrackDao: SavedTrackDao,
    private val clock: Clock = Clock.System,
) {
    fun observeFavourites(): Flow<List<FavouriteRoute>> = favouriteDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** Completed trips not already pinned, offered as pin candidates (newest first). */
    fun observePinnableTracks(): Flow<List<PinnableTrack>> =
        combine(savedTrackDao.getCompletedTracks(), favouriteDao.observeAll()) { tracks, favourites ->
            val pinnedTrackIds = favourites.map { it.sourceTrackId }.toSet()
            tracks
                .filter { it.routeId.isNotBlank() && it.routeId !in pinnedTrackIds }
                .map {
                    PinnableTrack(
                        trackId = it.routeId,
                        name = it.name,
                        purpose = it.service,
                        distanceKm = it.distance / 1000.0,
                    )
                }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun pin(
        track: PinnableTrack,
        name: String,
    ) {
        favouriteDao.upsert(
            FavouriteRouteEntity(
                id = "fav_${Uuid.random()}",
                sourceTrackId = track.trackId,
                name = name.ifBlank { track.name.ifBlank { "Favourite route" } },
                purpose = track.purpose,
                distanceKm = track.distanceKm,
                createdAtMs = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun rename(
        id: String,
        name: String,
    ) {
        if (name.isBlank()) return
        favouriteDao.rename(id, name.trim())
    }

    suspend fun remove(id: String) = favouriteDao.delete(id)

    private fun FavouriteRouteEntity.toDomain(): FavouriteRoute =
        FavouriteRoute(id = id, sourceTrackId = sourceTrackId, name = name, purpose = purpose, distanceKm = distanceKm)
}
