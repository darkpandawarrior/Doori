package com.miletracker.feature.tracking.repository

import com.miletracker.core.data.dao.LocationDao
import com.miletracker.core.data.model.db.LocationData
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val dao: LocationDao) {
    fun locationsForToken(token: String): Flow<List<LocationData>> = dao.getLocationsByToken(token)

    suspend fun insertBatch(locations: List<LocationData>) = dao.insertLocations(locations)

    suspend fun insert(location: LocationData) = dao.insertLocation(location)

    suspend fun markUploaded(ids: List<Long>) = dao.markLocationsAsSynced(ids)

    suspend fun countForToken(token: String): Int = dao.countLocationsByToken(token)

    suspend fun getForToken(token: String): List<LocationData> = dao.getLocationsByTokenPaged(token, limit = Int.MAX_VALUE, offset = 0)

    /** G1: one page of a track's GPS trail (chronological), for [LocationPagingSource]. */
    suspend fun pageForToken(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = dao.getLocationsByTokenPaged(token, limit = limit, offset = offset)

    suspend fun deleteForToken(token: String) = dao.deleteLocationsByToken(token)
}
