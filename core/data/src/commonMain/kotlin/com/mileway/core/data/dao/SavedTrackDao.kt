@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedTrack(savedTrack: SavedTrack)

    @Update
    suspend fun updateSavedTrack(savedTrack: SavedTrack): Int

    @Delete
    suspend fun deleteSavedTrack(track: SavedTrack)

    @Query("DELETE FROM saved_tracks WHERE routeId = :routeId")
    suspend fun deleteSavedTrack(routeId: String)

    @Query("DELETE FROM saved_tracks WHERE started_by_employee_code = :employeeCode")
    suspend fun deleteTracksByAccount(employeeCode: String): Int

    @Query("SELECT * FROM saved_tracks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllSavedTracks(): Flow<List<SavedTrack>>

    // P2.2: same ordering as [getAllSavedTracks], scoped to one persona's `started_by_account_id`
    // so Journeys/Expenses tabs re-query when the active account switches. Additive — the unscoped
    // overload above is unchanged for existing call sites that don't filter by account.
    @Query("SELECT * FROM saved_tracks WHERE started_by_account_id = :accountId ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>>

    @Query("SELECT * FROM saved_tracks WHERE isCompleted = 1 ORDER BY createdAt DESC")
    fun getCompletedTracks(): Flow<List<SavedTrack>>

    @Query("SELECT COUNT(*) FROM saved_tracks")
    suspend fun count(): Long

    @Query("SELECT * FROM saved_tracks WHERE isCompleted = 0 LIMIT 1")
    suspend fun getActiveTrack(): SavedTrack?

    @Query(
        "SELECT * FROM saved_tracks WHERE started_by_employee_code = :employeeCode AND isCompleted = 0 AND isDiscarded = 0 AND isDraft = 0 ORDER BY started_at_timestamp DESC LIMIT 1",
    )
    suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack?

    @Query("SELECT * FROM saved_tracks WHERE started_by_employee_code = :employeeCode AND isCompleted = 0 AND isDraft = 0 ORDER BY started_at_timestamp DESC")
    fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>>

    @Query("SELECT * FROM saved_tracks WHERE isCompleted = 0 ORDER BY started_at_timestamp DESC LIMIT 1")
    suspend fun getMostRecentActiveTrack(): SavedTrack?

    @Query("SELECT * FROM saved_tracks WHERE isCompleted = 1 AND isDiscarded = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastCompletedTrack(): SavedTrack?

    @Query("SELECT * FROM saved_tracks WHERE routeId = :routeId")
    suspend fun getSavedTrackById(routeId: String): SavedTrack?

    @Query("SELECT * FROM saved_tracks WHERE routeId = :routeId LIMIT 1")
    fun observeTrackById(routeId: String): Flow<SavedTrack?>

    @Query("SELECT * FROM saved_tracks WHERE isRetained = 1 OR isDiscarded = 1 ORDER BY createdAt DESC")
    fun getRetainedTracks(): Flow<List<SavedTrack>>

    @Query("SELECT * FROM saved_tracks WHERE createdAt >= :start AND createdAt < :end ORDER BY createdAt DESC")
    fun getTracksInRange(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>>

    @Query("SELECT * FROM saved_tracks WHERE createdAt >= :start AND createdAt < :end AND isRetained = 0 ORDER BY createdAt DESC")
    fun getTracksInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Flow<List<SavedTrack>>

    @Query("SELECT COUNT(*) FROM saved_tracks WHERE createdAt >= :start AND createdAt < :end AND isRetained = 0")
    suspend fun countInRangeExcludingRetained(
        start: Long,
        end: Long,
    ): Int

    @Query("UPDATE saved_tracks SET name = :name WHERE routeId = :routeId")
    suspend fun updateTrackName(
        routeId: String,
        name: String,
    )

    @Query("UPDATE saved_tracks SET smartDistanceFinal = :value WHERE routeId = :routeId")
    suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    )

    @Query("UPDATE saved_tracks SET distance = :distance, duration = :duration WHERE routeId = :routeId AND isCompleted = 0")
    suspend fun updateTrackLiveData(
        routeId: String,
        distance: Double,
        duration: Long,
    )

    @Query("UPDATE saved_tracks SET isDraft = 1, draftSavedAt = :draftSavedAt, isDiscarded = 0 WHERE routeId = :routeId")
    suspend fun markTrackDraft(
        routeId: String,
        draftSavedAt: Long,
    ): Int

    @Query("UPDATE saved_tracks SET submissionTime = :submissionTime WHERE routeId = :routeId")
    suspend fun updateSubmissionTime(
        routeId: String,
        submissionTime: Long,
    ): Int

    @Query(
        "UPDATE saved_tracks SET isCompleted = 1, endTime = :endTime, distance = :finalDistance, avgSpeed = :avgSpeed, maxSpeed = :maxSpeed WHERE routeId = :routeId",
    )
    suspend fun finalizeTrack(
        routeId: String,
        endTime: Long,
        finalDistance: Double,
        avgSpeed: Double,
        maxSpeed: Double,
    )

    @Query(
        """
        UPDATE saved_tracks
        SET isCompleted = 1, serverUploaded = 1, isDraft = 0, isDiscarded = 0, isRetained = 0,
            draftSavedAt = 0, submissionTime = :currentTime, trackingActivity = :trackingActivity,
            endTime = CASE WHEN endTime > 0 THEN endTime ELSE :currentTime END,
            name = :newName, submittedAmount = :submittedAmount,
            submittedAmountCurrency = :submittedAmountCurrency, transId = :transId
        WHERE routeId = :routeId
    """,
    )
    suspend fun markTrackCompleted(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
        submittedAmount: Double = 0.0,
        submittedAmountCurrency: String = "INR",
        transId: String? = null,
    ): Int

    @Query(
        """
        UPDATE saved_tracks
        SET isCompleted = 1, isDraft = 1, isDiscarded = 0, isRetained = 0,
            draftSavedAt = :currentTime, submissionTime = :currentTime,
            trackingActivity = :trackingActivity,
            endTime = CASE WHEN endTime > 0 THEN endTime ELSE :currentTime END,
            name = :newName
        WHERE routeId = :routeId
    """,
    )
    suspend fun markTrackEndedLocally(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
    ): Int

    @Query("UPDATE saved_tracks SET isRetained = 1 WHERE routeId IN (:routeIds)")
    suspend fun markRetained(routeIds: List<String>)

    @Query("UPDATE saved_tracks SET isRetained = 1 WHERE createdAt < :threshold AND isRetained = 0")
    suspend fun markRetainedBefore(threshold: Long): Int

    @Query("UPDATE saved_tracks SET isRetained = :retained WHERE routeId = :routeId")
    suspend fun setRetained(
        routeId: String,
        retained: Boolean,
    )

    @Query("DELETE FROM saved_tracks WHERE routeId = '' OR routeId IS NULL")
    suspend fun deleteCorruptedTracks(): Int

    @Query("SELECT COUNT(*) FROM saved_tracks WHERE routeId = '' OR routeId IS NULL")
    suspend fun getCorruptedTrackCount(): Int

    @Query("DELETE FROM saved_tracks WHERE createdAt < :threshold AND isRetained = 0 AND ((isCompleted = 1 AND serverUploaded = 1) OR isDiscarded = 1)")
    suspend fun deleteOlderThanExcludingRetained(threshold: Long): Int

    @Query("SELECT routeId FROM saved_tracks WHERE createdAt >= :start AND createdAt < :end AND isRetained = 0 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLastNRouteIdsFromRange(
        start: Long,
        end: Long,
        limit: Int,
    ): List<String>

    @Query("SELECT AVG(distance) as avgDistance, AVG(duration) as avgDuration, AVG(avgSpeed) as avgSpeed, COUNT(*) as totalTracks FROM saved_tracks")
    suspend fun getAverageTrackMetrics(): TrackMetrics

    @Query("SELECT * FROM saved_tracks WHERE routeId != :routeId ORDER BY ABS(distance - (SELECT distance FROM saved_tracks WHERE routeId = :routeId)) LIMIT 1")
    suspend fun getPreviousSimilarTrack(routeId: String): SavedTrack?

    @Query(
        "SELECT * FROM saved_tracks WHERE routeId != :routeId AND (SELECT distance FROM saved_tracks WHERE routeId = :routeId) IS NOT NULL AND ABS(distance - (SELECT distance FROM saved_tracks WHERE routeId = :routeId)) / NULLIF((SELECT distance FROM saved_tracks WHERE routeId = :routeId), 0) < 0.2 ORDER BY ABS(distance - (SELECT distance FROM saved_tracks WHERE routeId = :routeId)) LIMIT 10",
    )
    suspend fun getSimilarTracks(routeId: String): List<SavedTrack>

    @Query("SELECT routeId FROM saved_tracks WHERE serverUploaded = 1 AND isCompleted = 1 AND createdAt < :cutoffMillis AND has_local_data = 1")
    suspend fun getRouteIdsEligibleForCleanup(cutoffMillis: Long): List<String>

    @Query("UPDATE saved_tracks SET has_local_data = 0 WHERE routeId = :routeId")
    suspend fun markLocalDataPurged(routeId: String)

    // P-C.1: L1 flag — set when the user swipes the app from recents while tracking.
    @Query("UPDATE saved_tracks SET wasAppKilled = 1, appKilledCount = appKilledCount + 1 WHERE routeId = :routeId")
    suspend fun markAppKilled(routeId: String): Int

    // P-C.2: L2 flag — set when the OS terminates the FGS and relaunches it (sticky restart / bg relaunch).
    @Query(
        "UPDATE saved_tracks SET foregroundServiceTerminated = 1, foregroundServiceTerminatedCount = foregroundServiceTerminatedCount + 1 WHERE routeId = :routeId",
    )
    suspend fun markFgTerminated(routeId: String): Int

    // P-C.3: L4 flag — set when the device shuts down while the session is active.
    @Query("UPDATE saved_tracks SET wasPhoneShutDown = 1 WHERE routeId = :routeId")
    suspend fun markPhoneShutDown(routeId: String): Int

    // P3.3: already-claimed guard — a completed trip can only fund one voucher. Called once per
    // selected trip when a voucher is submitted (CreateVoucherViewModel.submit()).
    @Query("UPDATE saved_tracks SET claimedByVoucherNumber = :voucherNumber WHERE routeId = :routeId")
    suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int

    // P6.1: odometer-not-working fallback — records that this trip's expense was rate-sourced
    // from GPS distance rather than an odometer-reading delta, at submission time.
    @Query("UPDATE saved_tracks SET odometerNotWorking = 1 WHERE routeId = :routeId")
    suspend fun markOdometerNotWorking(routeId: String): Int

    // PLAN_V33 gap-fix: persists the office/entity the user picked on the submission screen, so
    // SubmitMilesRequestBuilder (which reads officeId/entityId off this row) stops always seeing null.
    @Query("UPDATE saved_tracks SET officeId = :officeId, entityId = :entityId WHERE routeId = :routeId")
    suspend fun setOfficeAndEntity(
        routeId: String,
        officeId: Long?,
        entityId: Long?,
    ): Int
}
