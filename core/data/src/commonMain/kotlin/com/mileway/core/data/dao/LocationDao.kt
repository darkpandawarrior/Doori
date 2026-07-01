package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mileway.core.data.model.db.LocationData
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE token = :token ORDER BY date ASC")
    fun getLocationsByToken(token: String): Flow<List<LocationData>>

    @Query("SELECT * FROM locations WHERE token = :token ORDER BY date ASC")
    suspend fun getLocationsByTokenOnce(token: String): List<LocationData>

    @Query("SELECT * FROM locations WHERE token = :token ORDER BY date ASC LIMIT :limit OFFSET :offset")
    suspend fun getLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData>

    @Query("SELECT COUNT(*) FROM locations WHERE token = :token")
    suspend fun countLocationsByToken(token: String): Int

    @Query("SELECT * FROM locations ORDER BY date DESC")
    fun getAllLocations(): Flow<List<LocationData>>

    @Query("SELECT * FROM locations WHERE uploaded = :uploaded ORDER BY date ASC")
    fun getLocationsByUploadStatus(uploaded: Boolean): Flow<List<LocationData>>

    @Query("SELECT * FROM locations WHERE activity = :activity ORDER BY date ASC")
    fun getLocationsByActivity(activity: String): Flow<List<LocationData>>

    @Query("SELECT * FROM locations WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getLocationsByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<LocationData>>

    @Query("SELECT * FROM locations WHERE token = :token AND checkInType != 'NONE' ORDER BY date ASC")
    fun getCheckInLocationsByToken(token: String): Flow<List<LocationData>>

    @Insert
    suspend fun insertLocation(location: LocationData)

    @Insert
    suspend fun insertLocations(locations: List<LocationData>)

    @Update
    suspend fun updateLocation(location: LocationData)

    @Query("UPDATE locations SET uploaded = :uploaded WHERE id = :id")
    suspend fun updateUploadStatus(
        id: Long,
        uploaded: Boolean,
    )

    @Query("UPDATE locations SET uploaded = :uploaded WHERE token = :token")
    suspend fun updateUploadStatusByToken(
        token: String,
        uploaded: Boolean,
    )

    @Delete
    suspend fun deleteLocation(location: LocationData)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)

    @Query("DELETE FROM locations WHERE token = :token")
    suspend fun deleteLocationsByToken(token: String)

    @Query("DELETE FROM locations WHERE uploaded = :uploadedValue")
    suspend fun deleteUploadedLocations(uploadedValue: Boolean = true)

    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations()

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationCount(): Int

    @Query("SELECT COUNT(*) FROM locations WHERE uploaded = :uploadedValue")
    suspend fun getUnuploadedLocationCount(uploadedValue: Boolean = false): Int

    @Query("SELECT * FROM locations WHERE token = :token AND uploaded = 0 ORDER BY date ASC")
    suspend fun getUnsyncedLocationsByToken(token: String): List<LocationData>

    @Query("SELECT * FROM locations WHERE token = :token AND uploaded = 0 ORDER BY date ASC LIMIT :limit OFFSET :offset")
    suspend fun getUnsyncedLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData>

    @Query("UPDATE locations SET uploaded = 1 WHERE id IN (:locationIds)")
    suspend fun markLocationsAsSynced(locationIds: List<Long>)

    @Query("DELETE FROM locations WHERE date < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int

    @Query("SELECT * FROM locations WHERE token = :token AND uploaded = 0 ORDER BY date ASC LIMIT 1")
    suspend fun getFirstUnsyncedLocationByToken(token: String): LocationData?

    @Query("SELECT * FROM locations WHERE token = :token ORDER BY date DESC LIMIT 1")
    suspend fun getLastLocationByToken(token: String): LocationData?
}
