package com.mileway.core.data.favourite

import com.mileway.core.data.dao.FavouriteRouteDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.FavouriteRouteEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P12.8 — the favourite-routes store's real logic: pinnable candidates are the completed
 * trips minus the already-pinned ones (metres → km preserved), and a pin with a blank name falls
 * back to the trip name.
 */
class FavouriteRoutesRepositoryTest {
    private class FakeFavouriteDao : FavouriteRouteDao {
        val rows = MutableStateFlow<List<FavouriteRouteEntity>>(emptyList())

        override fun observeAll(): Flow<List<FavouriteRouteEntity>> = rows.map { it.sortedByDescending { r -> r.createdAtMs } }

        override suspend fun upsert(entity: FavouriteRouteEntity) {
            rows.value = rows.value.filterNot { it.id == entity.id } + entity
        }

        override suspend fun get(id: String): FavouriteRouteEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun rename(
            id: String,
            name: String,
        ) {
            rows.value = rows.value.map { if (it.id == id) it.copy(name = name) else it }
        }

        override suspend fun delete(id: String) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }

    @Test
    fun `pinnable excludes already-pinned tracks and pin falls back to the trip name`() =
        runTest {
            val favDao = FakeFavouriteDao()
            val trackDao =
                completedTracksDao(
                    completed("route-1", "Home to office", distanceMetres = 5_000.0),
                    completed("route-2", "Airport run", distanceMetres = 12_000.0),
                )
            val repo = FavouriteRoutesRepository(favDao, trackDao)

            // Both completed trips are pinnable initially, km-converted.
            val pinnableBefore = repo.observePinnableTracks().first()
            assertEquals(listOf("route-1", "route-2"), pinnableBefore.map { it.trackId })
            assertEquals(5.0, pinnableBefore.first { it.trackId == "route-1" }.distanceKm)

            // Pin route-1 with a blank name → falls back to the trip name.
            repo.pin(pinnableBefore.first { it.trackId == "route-1" }, name = "")
            val fav = repo.observeFavourites().first().single()
            assertEquals("Home to office", fav.name)

            // route-1 is no longer offered as pinnable.
            assertEquals(listOf("route-2"), repo.observePinnableTracks().first().map { it.trackId })
        }

    private fun completed(
        routeId: String,
        name: String,
        distanceMetres: Double,
    ) = SavedTrack(
        routeId = routeId,
        name = name,
        isCompleted = true,
        startLatitude = 0.0, startLongitude = 0.0,
        endLatitude = 0.0, endLongitude = 0.0,
        pausedLatitude = 0.0, pausedLongitude = 0.0,
        startTime = 0L, endTime = 1L,
        distance = distanceMetres, duration = 1_000L,
        service = "OWN CAR INTRA",
    )

    private fun completedTracksDao(vararg tracks: SavedTrack): SavedTrackDao =
        object : SavedTrackDao {
            override fun getCompletedTracks(): Flow<List<SavedTrack>> = flowOf(tracks.toList())

            override suspend fun insertSavedTrack(savedTrack: SavedTrack) = Unit

            override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

            override suspend fun deleteSavedTrack(track: SavedTrack) = Unit

            override suspend fun deleteSavedTrack(routeId: String) = Unit

            override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

            override fun getAllSavedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

            override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = flowOf(emptyList())

            override suspend fun count(): Long = 0

            override suspend fun getActiveTrack(): SavedTrack? = null

            override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null

            override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())

            override suspend fun getMostRecentActiveTrack(): SavedTrack? = null

            override suspend fun getLastCompletedTrack(): SavedTrack? = null

            override suspend fun getSavedTrackById(routeId: String): SavedTrack? = null

            override fun observeTrackById(routeId: String): Flow<SavedTrack?> = flowOf(null)

            override fun getRetainedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

            override fun getTracksInRange(
                start: Long,
                end: Long,
            ): Flow<List<SavedTrack>> = flowOf(emptyList())

            override fun getTracksInRangeExcludingRetained(
                start: Long,
                end: Long,
            ): Flow<List<SavedTrack>> = flowOf(emptyList())

            override suspend fun countInRangeExcludingRetained(
                start: Long,
                end: Long,
            ): Int = 0

            override suspend fun updateTrackName(
                routeId: String,
                name: String,
            ) = Unit

            override suspend fun updateSmartDistanceFinal(
                routeId: String,
                value: Double,
            ) = Unit

            override suspend fun updateTrackLiveData(
                routeId: String,
                distance: Double,
                duration: Long,
            ) = Unit

            override suspend fun markTrackDraft(
                routeId: String,
                draftSavedAt: Long,
            ): Int = 0

            override suspend fun updateSubmissionTime(
                routeId: String,
                submissionTime: Long,
            ): Int = 0

            override suspend fun finalizeTrack(
                routeId: String,
                endTime: Long,
                finalDistance: Double,
                avgSpeed: Double,
                maxSpeed: Double,
            ) = Unit

            override suspend fun markTrackCompleted(
                routeId: String,
                trackingActivity: String,
                currentTime: Long,
                newName: String,
                submittedAmount: Double,
                submittedAmountCurrency: String,
                transId: String?,
            ): Int = 0

            override suspend fun markTrackEndedLocally(
                routeId: String,
                trackingActivity: String,
                currentTime: Long,
                newName: String,
            ): Int = 0

            override suspend fun markRetained(routeIds: List<String>) = Unit

            override suspend fun markRetainedBefore(threshold: Long): Int = 0

            override suspend fun setRetained(
                routeId: String,
                retained: Boolean,
            ) = Unit

            override suspend fun deleteCorruptedTracks(): Int = 0

            override suspend fun getCorruptedTrackCount(): Int = 0

            override suspend fun deleteOlderThanExcludingRetained(threshold: Long): Int = 0

            override suspend fun getLastNRouteIdsFromRange(
                start: Long,
                end: Long,
                limit: Int,
            ): List<String> = emptyList()

            override suspend fun getAverageTrackMetrics(): TrackMetrics = TrackMetrics(0.0, 0L, 0f, 0)

            override suspend fun getPreviousSimilarTrack(routeId: String): SavedTrack? = null

            override suspend fun getSimilarTracks(routeId: String): List<SavedTrack> = emptyList()

            override suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String> = emptyList()

            override suspend fun markLocalDataPurged(routeId: String) = Unit

            override suspend fun markAppKilled(routeId: String): Int = 0

            override suspend fun markFgTerminated(routeId: String): Int = 0

            override suspend fun markPhoneShutDown(routeId: String): Int = 0

            override suspend fun markClaimedByVoucher(
                routeId: String,
                voucherNumber: String,
            ): Int = 0

            override suspend fun markOdometerNotWorking(routeId: String): Int = 0
        }
}
