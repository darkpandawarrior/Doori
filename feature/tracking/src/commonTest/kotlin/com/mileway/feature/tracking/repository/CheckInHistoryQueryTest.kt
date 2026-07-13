package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.model.db.LocationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CheckInHistoryScreen's data source: [LocationRepository.allCheckInPoints] must return only rows
 * where `wasCheckInPoint` is true, regardless of token, newest first.
 */
class CheckInHistoryQueryTest {
    private fun point(
        id: Long,
        token: String,
        wasCheckInPoint: Boolean,
        date: Long,
    ) = LocationData(
        id = id,
        activity = "STILL",
        speed = 0f,
        lat = 0.0,
        lng = 0.0,
        token = token,
        date = date,
        batteryPercentage = 100.0,
        wasCheckInPoint = wasCheckInPoint,
    )

    @Test
    fun `allCheckInPoints returns only wasCheckInPoint rows across tokens`() =
        runTest {
            val dao = FakeCheckInLocationDao()
            dao.rows +=
                listOf(
                    point(1, "trip-A", wasCheckInPoint = true, date = 100L),
                    point(2, "trip-A", wasCheckInPoint = false, date = 200L),
                    point(3, "trip-B", wasCheckInPoint = true, date = 300L),
                    point(4, "trip-B", wasCheckInPoint = false, date = 400L),
                )
            val repo = LocationRepository(dao)

            val result = repo.allCheckInPoints().first()

            assertEquals(listOf(3L, 1L), result.map { it.id })
            assertTrue(result.all { it.wasCheckInPoint })
        }
}

// ── Minimal fake (only the check-in query matters for this test) ────────────
// `internal` (not `private`) so PLAN_V29 P29.S.1's CheckInSearchProviderTest can reuse it instead
// of duplicating another full LocationDao fake.

internal class FakeCheckInLocationDao : LocationDao {
    val rows = mutableListOf<LocationData>()

    override fun getAllCheckInPoints(): Flow<List<LocationData>> {
        return MutableStateFlow(rows.filter { it.wasCheckInPoint }.sortedByDescending { it.date })
    }

    // ── Everything else unused ─────────────────────────────────────────────
    override fun getLocationsByToken(token: String): Flow<List<LocationData>> = MutableStateFlow(emptyList())

    override suspend fun getLocationsByTokenOnce(token: String): List<LocationData> = emptyList()

    override suspend fun getLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun countLocationsByToken(token: String): Int = 0

    override fun getAllLocations(): Flow<List<LocationData>> = MutableStateFlow(emptyList())

    override fun getLocationsByUploadStatus(uploaded: Boolean): Flow<List<LocationData>> = MutableStateFlow(emptyList())

    override fun getLocationsByActivity(activity: String): Flow<List<LocationData>> = MutableStateFlow(emptyList())

    override fun getLocationsByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<LocationData>> = MutableStateFlow(emptyList())

    override fun getCheckInLocationsByToken(token: String): Flow<List<LocationData>> = MutableStateFlow(emptyList())

    override suspend fun insertLocation(location: LocationData) {}

    override suspend fun insertLocations(locations: List<LocationData>) {}

    override suspend fun updateLocation(location: LocationData) {}

    override suspend fun updateUploadStatus(
        id: Long,
        uploaded: Boolean,
    ) {}

    override suspend fun updateUploadStatusByToken(
        token: String,
        uploaded: Boolean,
    ) {}

    override suspend fun deleteLocation(location: LocationData) {}

    override suspend fun deleteLocationById(id: Long) {}

    override suspend fun deleteLocationsByToken(token: String) {}

    override suspend fun deleteUploadedLocations(uploadedValue: Boolean) {}

    override suspend fun deleteAllLocations() {}

    override suspend fun getLocationCount(): Int = 0

    override suspend fun getUnuploadedLocationCount(uploadedValue: Boolean): Int = 0

    override suspend fun getUnsyncedLocationsByToken(token: String): List<LocationData> = emptyList()

    override suspend fun getUnsyncedLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun markLocationsAsSynced(locationIds: List<Long>) {}

    override suspend fun deleteOlderThan(timestamp: Long): Int = 0

    override suspend fun getFirstUnsyncedLocationByToken(token: String): LocationData? = null

    override suspend fun getLastLocationByToken(token: String): LocationData? = null
}
