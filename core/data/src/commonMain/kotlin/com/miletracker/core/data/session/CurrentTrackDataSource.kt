package com.miletracker.core.data.session

import com.miletracker.core.data.model.db.CurrentTrackData
import kotlinx.coroutines.flow.Flow

interface CurrentTrackDataSource {
    val currentTrackFlow: Flow<CurrentTrackData>

    suspend fun saveSession(data: CurrentTrackData)

    suspend fun updateDistance(
        token: String,
        distanceMeters: Double,
        speed: Double,
        avgSpeed: Double,
    )

    suspend fun updateLocationCount(
        token: String,
        total: Long,
        unsynced: Long,
    )

    suspend fun markPaused(
        token: String,
        lat: Double,
        lng: Double,
    )

    suspend fun markResumed(token: String)

    suspend fun markStopped(
        token: String,
        endLat: Double,
        endLng: Double,
    )

    suspend fun clearSession()

    suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    )
}
