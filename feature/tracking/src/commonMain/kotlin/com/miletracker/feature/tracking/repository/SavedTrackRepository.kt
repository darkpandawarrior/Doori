package com.miletracker.feature.tracking.repository

import com.miletracker.core.data.dao.SavedTrackDao
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.data.model.display.TrackDisplayData
import com.miletracker.core.data.model.display.toDisplayData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class SavedTrackRepository(private val dao: SavedTrackDao) {
    fun allTracksFlow(): Flow<List<TrackDisplayData>> = dao.getAllSavedTracks().map { list -> list.map { it.toDisplayData() } }

    fun completedTracksFlow(): Flow<List<TrackDisplayData>> = dao.getCompletedTracks().map { list -> list.map { it.toDisplayData() } }

    suspend fun getByRouteId(routeId: String): SavedTrack? = dao.getSavedTrackById(routeId)

    fun observeByRouteId(routeId: String): Flow<SavedTrack?> = dao.observeTrackById(routeId)

    suspend fun insert(track: SavedTrack) = dao.insertSavedTrack(track)

    suspend fun update(track: SavedTrack) = dao.updateSavedTrack(track)

    suspend fun markSubmitted(
        routeId: String,
        transId: String,
        amount: Double,
    ) = dao.markTrackCompleted(
        routeId = routeId,
        trackingActivity = "Submitted",
        currentTime = Clock.System.now().toEpochMilliseconds(),
        newName = "Submitted Journey",
        submittedAmount = amount,
        submittedAmountCurrency = "INR",
        transId = transId,
    )

    suspend fun count(): Long = dao.count()

    suspend fun getActiveTrack(): SavedTrack? = dao.getActiveTrack()

    // P-C.1: write wasAppKilled=true + increment appKilledCount; returns rows updated (0 if track not found).
    suspend fun markAppKilled(routeId: String): Int = dao.markAppKilled(routeId)

    // P-C.2: write foregroundServiceTerminated=true + increment count; returns rows updated.
    suspend fun markFgTerminated(routeId: String): Int = dao.markFgTerminated(routeId)

    // P-C.3: write wasPhoneShutDown=true; returns rows updated.
    suspend fun markPhoneShutDown(routeId: String): Int = dao.markPhoneShutDown(routeId)
}
