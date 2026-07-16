package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.core.data.outbox.LocationBatch
import com.mileway.core.data.outbox.LocationBatchOutbox
import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.data.outbox.TripDraftOutbox
import com.mileway.core.data.session.CurrentTrackDataSource
import com.siddharth.kmp.offlineoutbox.DraftEntry
import com.siddharth.kmp.offlineoutbox.DraftStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// PLAN_V34 P1: shared sync-test fixtures — promoted from private copies in LocationDataSyncerTest,
// MilesSubmitSyncerTest and SessionReconciliationPolicyTest so AppSyncTriggerTest (and future sync
// tests) reuse them instead of adding yet another per-file SavedTrackDao/LocationDao fake.

internal class FakeSyncLocationDao(unsynced: List<LocationData>) : LocationDao {
    val unsynced = unsynced.toMutableList()
    val markedSynced = mutableListOf<Long>()

    override fun getLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override suspend fun getLocationsByTokenOnce(token: String): List<LocationData> = emptyList()

    override suspend fun getLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = emptyList()

    override suspend fun countLocationsByToken(token: String): Int = 0

    override fun getAllLocations(): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByUploadStatus(uploaded: Boolean): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByActivity(activity: String): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getLocationsByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getCheckInLocationsByToken(token: String): Flow<List<LocationData>> = flowOf(emptyList())

    override fun getAllCheckInPoints(): Flow<List<LocationData>> = flowOf(emptyList())

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

    override suspend fun getLocationCount(): Int = unsynced.size

    override suspend fun getUnuploadedLocationCount(uploadedValue: Boolean): Int = unsynced.size

    override suspend fun getUnsyncedLocationsByToken(token: String): List<LocationData> = unsynced.toList()

    override suspend fun getUnsyncedLocationsByTokenPaged(
        token: String,
        limit: Int,
        offset: Int,
    ): List<LocationData> = unsynced.drop(offset).take(limit)

    override suspend fun getLocationsByIds(ids: List<Long>): List<LocationData> = unsynced.filter { it.id in ids }

    override suspend fun markLocationsAsSynced(locationIds: List<Long>) {
        markedSynced.addAll(locationIds)
        unsynced.removeAll { it.id in locationIds }
    }

    override suspend fun deleteOlderThan(timestamp: Long): Int = 0

    override suspend fun getFirstUnsyncedLocationByToken(token: String): LocationData? = unsynced.firstOrNull()

    override suspend fun getLastLocationByToken(token: String): LocationData? = unsynced.lastOrNull()
}

/** In-memory [LocationBatchOutbox] — mirrors FakeSubmitOutbox in LogMilesSubmitUseCaseTest. */
internal class FakeLocationBatchOutbox : LocationBatchOutbox {
    private val entries = MutableStateFlow<Map<String, DraftEntry<LocationBatch>>>(emptyMap())

    val enqueued: List<LocationBatch> get() = entries.value.values.map { it.payload }

    fun statusFor(batch: LocationBatch): DraftStatus = entries.value.values.first { it.payload == batch }.status

    override fun drafts(formKey: String): Flow<List<DraftEntry<LocationBatch>>> = entries.map { it.values.filter { e -> e.formKey == formKey } }

    override suspend fun enqueue(
        formKey: String,
        uniqueKey: String,
        payload: LocationBatch,
    ) {
        entries.value = entries.value + (uniqueKey to DraftEntry(formKey, uniqueKey, payload, DraftStatus.PENDING, null, 0L, 0L))
    }

    override suspend fun markSubmitted(
        formKey: String,
        uniqueKey: String,
    ) {
        entries.value = entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.SUBMITTED))
    }

    override suspend fun markFailed(
        formKey: String,
        uniqueKey: String,
        error: String,
    ) {
        entries.value =
            entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.FAILED, errorMessage = error))
    }

    override suspend fun clear(formKey: String) {
        entries.value = entries.value.filterValues { it.formKey != formKey }
    }
}

internal class FakeTripDraftOutbox : TripDraftOutbox {
    private val entries = MutableStateFlow<Map<String, DraftEntry<TripDraft>>>(emptyMap())

    fun statusFor(uniqueKey: String): DraftStatus = entries.value.getValue(uniqueKey).status

    override fun drafts(formKey: String): Flow<List<DraftEntry<TripDraft>>> = entries.map { it.values.filter { e -> e.formKey == formKey } }

    override suspend fun enqueue(
        formKey: String,
        uniqueKey: String,
        payload: TripDraft,
    ) {
        entries.value = entries.value + (uniqueKey to DraftEntry(formKey, uniqueKey, payload, DraftStatus.PENDING, null, 0L, 0L))
    }

    override suspend fun markSubmitted(
        formKey: String,
        uniqueKey: String,
    ) {
        entries.value = entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.SUBMITTED))
    }

    override suspend fun markFailed(
        formKey: String,
        uniqueKey: String,
        error: String,
    ) {
        entries.value =
            entries.value + (uniqueKey to entries.value.getValue(uniqueKey).copy(status = DraftStatus.FAILED, errorMessage = error))
    }

    override suspend fun clear(formKey: String) {
        entries.value = entries.value.filterValues { it.formKey != formKey }
    }
}

/** Minimal [SavedTrackDao] fake — records [markTrackCompleted] calls, everything else is unused stubbing. */
internal class FakeMilesSubmitDao : SavedTrackDao {
    val completed = mutableListOf<Triple<String, String?, Double>>()

    override suspend fun markTrackCompleted(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
        submittedAmount: Double,
        submittedAmountCurrency: String,
        transId: String?,
    ): Int {
        completed += Triple(routeId, transId, submittedAmount)
        return 1
    }

    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = null

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {}

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int = 0

    override suspend fun deleteSavedTrack(track: SavedTrack) {}

    override suspend fun deleteSavedTrack(routeId: String) {}

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun count(): Long = 0

    override suspend fun getActiveTrack(): SavedTrack? = null

    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null

    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun getMostRecentActiveTrack(): SavedTrack? = null

    override suspend fun getLastCompletedTrack(): SavedTrack? = null

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
    ) {}

    override suspend fun updateTrackLiveData(
        routeId: String,
        distance: Double,
        duration: Long,
    ) {}

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
    ) {}

    override suspend fun markTrackEndedLocally(
        routeId: String,
        trackingActivity: String,
        currentTime: Long,
        newName: String,
    ): Int = 0

    override suspend fun markRetained(routeIds: List<String>) {}

    override suspend fun markRetainedBefore(threshold: Long): Int = 0

    override suspend fun setRetained(
        routeId: String,
        retained: Boolean,
    ) {}

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

    override suspend fun markLocalDataPurged(routeId: String) {}

    override suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int = 0

    override suspend fun markOdometerNotWorking(routeId: String): Int = 0

    override suspend fun setOfficeAndEntity(
        routeId: String,
        officeId: Long?,
        entityId: Long?,
    ): Int = 0
}

internal class FakeCurrentTrackSource(private val initial: CurrentTrackData) : CurrentTrackDataSource {
    override val currentTrackFlow: Flow<CurrentTrackData> = MutableStateFlow(initial)

    override suspend fun saveSession(data: CurrentTrackData) {}

    override suspend fun updateDistance(
        token: String,
        distanceMeters: Double,
        speed: Double,
        avgSpeed: Double,
    ) {}

    override suspend fun updateLocationCount(
        token: String,
        total: Long,
        unsynced: Long,
    ) {}

    override suspend fun markPaused(
        token: String,
        lat: Double,
        lng: Double,
    ) {}

    override suspend fun markResumed(token: String) {}

    override suspend fun markStopped(
        token: String,
        endLat: Double,
        endLng: Double,
    ) {}

    override suspend fun clearSession() {}

    // P10.2: sync-settings current-journey override (no-op fake).
    override val syncSessionOverrideFlow: Flow<com.mileway.core.data.session.SyncSessionOverride?> =
        MutableStateFlow(null)

    override suspend fun setSyncSessionOverride(override: com.mileway.core.data.session.SyncSessionOverride?) {}

    override suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    ) {}
}
