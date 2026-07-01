package com.mileway.feature.tracking.repository

import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.session.CurrentTrackDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CurrentTrackRepository(
    private val dataStore: CurrentTrackDataSource,
) {
    val currentTrackFlow: Flow<CurrentTrackData> = dataStore.currentTrackFlow

    suspend fun getCurrentTrackDataRawAsync(): Result<CurrentTrackData> =
        runCatching {
            dataStore.currentTrackFlow.first()
        }

    suspend fun getHardwareEventQueueSnapshot(): Result<List<Pair<String, Long>>> = Result.success(emptyList())

    suspend fun startSession(data: CurrentTrackData) = dataStore.saveSession(data)

    suspend fun updateDistance(
        token: String,
        distanceMeters: Double,
        speed: Double,
        avgSpeed: Double,
    ) = dataStore.updateDistance(token, distanceMeters, speed, avgSpeed)

    suspend fun updateLocationCount(
        token: String,
        total: Long,
        unsynced: Long,
    ) = dataStore.updateLocationCount(token, total, unsynced)

    suspend fun pauseSession(
        token: String,
        lat: Double,
        lng: Double,
    ) = dataStore.markPaused(token, lat, lng)

    suspend fun resumeSession(token: String) = dataStore.markResumed(token)

    suspend fun stopSession(
        token: String,
        endLat: Double,
        endLng: Double,
    ) = dataStore.markStopped(token, endLat, endLng)

    suspend fun clearSession() = dataStore.clearSession()

    suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    ) = dataStore.updateLastHardwareEvent(token, eventText)
}
