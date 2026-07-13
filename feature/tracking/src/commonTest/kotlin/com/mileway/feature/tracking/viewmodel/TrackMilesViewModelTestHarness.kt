package com.mileway.feature.tracking.viewmodel

import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.CurrentTrackData
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.MockAccountEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.core.data.model.network.AllTaggedExpenseResponse
import com.mileway.core.data.model.network.AllTypesResponseV2
import com.mileway.core.data.model.network.ApprovedVehiclePricingResponse
import com.mileway.core.data.model.network.BulkEventRequest
import com.mileway.core.data.model.network.BulkEventRequestV2
import com.mileway.core.data.model.network.BulkLocationRequest
import com.mileway.core.data.model.network.BulkLocationRequestV2
import com.mileway.core.data.model.network.CheckInDetailsResponseV2
import com.mileway.core.data.model.network.CheckInRequestV2
import com.mileway.core.data.model.network.DistanceRequestV2
import com.mileway.core.data.model.network.DistanceResponseV2
import com.mileway.core.data.model.network.EmptyRequest
import com.mileway.core.data.model.network.EventRequest
import com.mileway.core.data.model.network.EventRequestV2
import com.mileway.core.data.model.network.EventResponseV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LocationRequest
import com.mileway.core.data.model.network.LocationRequestV2
import com.mileway.core.data.model.network.LocationResponseV2
import com.mileway.core.data.model.network.LogMilesRequestV2
import com.mileway.core.data.model.network.LogMilesResponseV2
import com.mileway.core.data.model.network.LogMilesRoutesResponse
import com.mileway.core.data.model.network.LogMilesServicesResponse
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.MapResponse
import com.mileway.core.data.model.network.PolicyApprovedVehiclesResponse
import com.mileway.core.data.model.network.PostMileageEventRequestK
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.model.network.SubmittedCheckInResponseV2
import com.mileway.core.data.model.network.SuccessResponseV2
import com.mileway.core.data.model.network.TrackMileageStatusResponse
import com.mileway.core.data.model.state.LogMilesPluginConfig
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.config.ConfigProvider
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

// ── ConfigProvider fake ────────────────────────────────────────────────────────

private object FakeConfigProvider : ConfigProvider {
    override fun getTrackMilesConfig(): TrackMilesPluginConfig = TrackMilesPluginConfig()

    override fun getLogMilesConfig(): LogMilesPluginConfig = LogMilesPluginConfig()

    override fun isMilesEnabled(): Boolean = true

    override fun isLogMilesEnabled(): Boolean = true

    override fun getCurrency(): String = "USD"
}

// ── MilewayNetworkApi fake (only vehicles/pricing used by VehiclePricingRepository) ──

// Shared across tracking VM tests; `vehicles` is configurable so callers (e.g.
// TrackingSuccessViewModelTest) can seed approved-vehicle pricing for the PolicyRateEngine.
internal open class FakeNetworkApi(
    private val approvedVehicles: List<com.mileway.core.data.model.network.ApprovedVehicle> = emptyList(),
) : MilewayNetworkApi {
    override suspend fun vehicles(trackMiles: Boolean): PolicyApprovedVehiclesResponse = PolicyApprovedVehiclesResponse(vehicles = approvedVehicles)

    override suspend fun pricing(): ApprovedVehiclePricingResponse = ApprovedVehiclePricingResponse(data = emptyMap())

    override suspend fun submitMilesEvent(request: PostMileageEventRequestK) = error("unused")

    override suspend fun logMilesLimit(request: LogMilesRequestV2): LogMilesResponseV2 = error("unused")

    override suspend fun logMiles(request: LogMilesSubmitRequestV2): ExpenseSubmissionResponse = error("unused")

    override suspend fun fetchLogMilesServices(isInsideTrip: Boolean): LogMilesServicesResponse = error("unused")

    override suspend fun logMilesRoutes(): LogMilesRoutesResponse = error("unused")

    override suspend fun discardMiles(request: PostMileageEventRequestK) = error("unused")

    override suspend fun distance(request: DistanceRequestV2): DistanceResponseV2 = error("unused")

    override suspend fun submitMiles(request: SubmitMilesRequestK): ExpenseSubmissionResponse = error("unused")

    override suspend fun getTrackMileageStatus(trackingToken: String): TrackMileageStatusResponse = error("unused")

    override suspend fun postLocation(request: BulkLocationRequest) = error("unused")

    override suspend fun postLocationSingle(request: LocationRequest) = error("unused")

    override suspend fun postBulkEvents(request: BulkEventRequest) = error("unused")

    override suspend fun postEventSingle(request: EventRequest) = error("unused")

    override suspend fun postLocationV2Single(request: LocationRequestV2) = error("unused")

    override suspend fun postLocationV2Batch(request: BulkLocationRequestV2) = error("unused")

    override suspend fun postEventV2Single(request: EventRequestV2) = error("unused")

    override suspend fun postBulkEventsV2(request: BulkEventRequestV2) = error("unused")

    override suspend fun getLocationsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): LocationResponseV2 = error("unused")

    override suspend fun getEventsV2(
        token: String,
        startTime: Long,
        endTime: Long,
    ): EventResponseV2 = error("unused")

    override suspend fun fetchMap(
        lat: String,
        lng: String,
    ): MapResponse = error("unused")

    override suspend fun geoTypeById(typeId: Long): CheckInDetailsResponseV2 = error("unused")

    override suspend fun submittedCheckins(token: String): SubmittedCheckInResponseV2 = error("unused")

    override suspend fun geoTypes(): AllTypesResponseV2 = error("unused")

    override suspend fun updateCenterLocation(request: CheckInRequestV2): SuccessResponseV2 = error("unused")

    override suspend fun resetMilesLocation(
        contactId: Long,
        request: EmptyRequest,
    ): SuccessResponseV2 = error("unused")

    override suspend fun submitCheckIn(request: CheckInRequestV2): SuccessResponseV2 = error("unused")

    override suspend fun allTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse = error("unused")

    override suspend fun pendingTaggedExpenses(
        start: Long,
        end: Long,
    ): AllTaggedExpenseResponse = error("unused")
}

// ── SavedTrackDao fake (minimal in-memory) ─────────────────────────────────────

// `internal` (not `private`) so PLAN_V29 P29.S.1's MileageSearchProviderTest can reuse it instead
// of duplicating another full SavedTrackDao fake.
internal class FakeSavedTrackDao(seed: List<SavedTrack> = emptyList()) : SavedTrackDao {
    // P10.1: stale-fake catch-up — SavedTrackDao.updateSmartDistanceFinal was added by the
    // SmartDistance commit without updating these test fakes; no-op override so this test source
    // set compiles (pre-existing breakage, incidental to P10.1).
    override suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = Unit

    private val tracks = mutableListOf<SavedTrack>().apply { addAll(seed) }
    private val allFlow = MutableStateFlow<List<SavedTrack>>(tracks.toList())

    private fun publish() {
        allFlow.value = tracks.toList()
    }

    override suspend fun insertSavedTrack(savedTrack: SavedTrack) {
        tracks += savedTrack
        publish()
    }

    override suspend fun updateSavedTrack(savedTrack: SavedTrack): Int {
        val i = tracks.indexOfFirst { it.routeId == savedTrack.routeId }
        if (i >= 0) {
            tracks[i] = savedTrack
            publish()
            return 1
        }
        return 0
    }

    override suspend fun deleteSavedTrack(track: SavedTrack) {
        tracks.remove(track)
        publish()
    }

    override suspend fun deleteSavedTrack(routeId: String) {
        tracks.removeAll { it.routeId == routeId }
        publish()
    }

    override suspend fun deleteTracksByAccount(employeeCode: String): Int = 0

    override fun getAllSavedTracks(): Flow<List<SavedTrack>> = allFlow

    override fun getAllSavedTracksByAccount(accountId: String): Flow<List<SavedTrack>> = flowOf(tracks.filter { it.startedByAccountId == accountId })

    override fun getCompletedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun count(): Long = tracks.size.toLong()

    override suspend fun getActiveTrack(): SavedTrack? = tracks.firstOrNull { it.endTime < 0 }

    override suspend fun getActiveTrackByAccount(employeeCode: String): SavedTrack? = null

    override fun getPausedTracksByAccount(employeeCode: String): Flow<List<SavedTrack>> = flowOf(emptyList())

    override suspend fun getMostRecentActiveTrack(): SavedTrack? = tracks.lastOrNull()

    override suspend fun getLastCompletedTrack(): SavedTrack? = null

    override suspend fun getSavedTrackById(routeId: String): SavedTrack? = tracks.firstOrNull { it.routeId == routeId }

    override fun observeTrackById(routeId: String): Flow<SavedTrack?> = flowOf(null)

    override fun getRetainedTracks(): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getTracksInRange(
        startMs: Long,
        endMs: Long,
    ): Flow<List<SavedTrack>> = flowOf(emptyList())

    override fun getTracksInRangeExcludingRetained(
        startMs: Long,
        endMs: Long,
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

    override suspend fun markAppKilled(routeId: String): Int = 0

    override suspend fun markFgTerminated(routeId: String): Int = 0

    override suspend fun markPhoneShutDown(routeId: String): Int = 0

    override suspend fun markClaimedByVoucher(
        routeId: String,
        voucherNumber: String,
    ): Int = 0

    override suspend fun markOdometerNotWorking(routeId: String): Int = 0
}

// ── LocationDao fake ─────────────────────────────────────────────────────────

private class FakeLocationDao : LocationDao {
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
        startMs: Long,
        endMs: Long,
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

// ── CurrentTrackDataSource fake ───────────────────────────────────────────────

private object FakeCurrentTrackDataSource : CurrentTrackDataSource {
    private val _flow = MutableStateFlow(CurrentTrackData(token = ""))
    override val currentTrackFlow: Flow<CurrentTrackData> = _flow

    override val syncSessionOverrideFlow: Flow<com.mileway.core.data.session.SyncSessionOverride?> =
        MutableStateFlow(null)

    override suspend fun setSyncSessionOverride(override: com.mileway.core.data.session.SyncSessionOverride?) {}

    override suspend fun saveSession(data: CurrentTrackData) {
        _flow.value = data
    }

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

    override suspend fun clearSession() {
        _flow.value = CurrentTrackData(token = "")
    }

    override suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    ) {}
}

// ── ActiveAccountSource / MockAccountDao / SessionSource fakes (P3.5) ─────────

internal class FakeActiveAccountSource(activeAccountId: String? = null) : com.mileway.core.data.session.ActiveAccountSource {
    override val activeAccountId: Flow<String?> = flowOf(activeAccountId)

    override suspend fun setActiveAccountId(accountId: String) = Unit
}

internal class FakeSessionSource(sessionState: com.mileway.core.data.session.SessionState) : com.mileway.core.data.session.SessionSource {
    override val sessionState: Flow<com.mileway.core.data.session.SessionState> = flowOf(sessionState)
}

private class FakeMockAccountDao(accounts: List<MockAccountEntity> = emptyList()) : MockAccountDao {
    private val byId = accounts.associateBy { it.accountId }.toMutableMap()

    override fun observeAll(): Flow<List<MockAccountEntity>> = flowOf(byId.values.toList())

    override suspend fun count(): Int = byId.size

    override suspend fun getById(accountId: String): MockAccountEntity? = byId[accountId]

    override suspend fun upsert(account: MockAccountEntity) {
        byId[account.accountId] = account
    }

    override suspend fun upsertAll(accounts: List<MockAccountEntity>) {
        accounts.forEach { byId[it.accountId] = it }
    }

    override suspend fun delete(accountId: String) {
        byId.remove(accountId)
    }

    override suspend fun clearActive() {
        byId.keys.toList().forEach { key -> byId[key] = byId.getValue(key).copy(isActive = false) }
    }

    override suspend fun markActive(accountId: String) {
        byId[accountId]?.let { byId[accountId] = it.copy(isActive = true) }
    }
}

// ── Harness factory ───────────────────────────────────────────────────────────

internal object TrackMilesViewModelTestHarness {
    fun build(
        controller: TrackingController = NoOpTrackingController,
        reconciliationHolder: com.mileway.feature.tracking.service.ReconciliationResultHolder? = null,
        seedTracks: List<SavedTrack> = emptyList(),
        sessionSource: com.mileway.core.data.session.SessionSource = NoSessionSource,
        activeAccountSource: com.mileway.core.data.session.ActiveAccountSource = FakeActiveAccountSource(),
        mockAccounts: List<MockAccountEntity> = emptyList(),
    ): TrackMilesViewModel =
        TrackMilesViewModel(
            configManager = TrackingConfigManager(FakeConfigProvider),
            vehicleRepo = VehiclePricingRepository(FakeNetworkApi()),
            trackRepo = SavedTrackRepository(FakeSavedTrackDao(seedTracks)),
            trackingController = controller,
            currentTrackRepo = CurrentTrackRepository(FakeCurrentTrackDataSource),
            locationRepo = LocationRepository(FakeLocationDao()),
            reconciliationHolder = reconciliationHolder,
            sessionSource = sessionSource,
            activeAccountSource = activeAccountSource,
            mockAccountDao = FakeMockAccountDao(mockAccounts),
        )
}

private object NoOpTrackingController : TrackingController {
    override fun start(token: String) {}

    override fun pause(token: String) {}

    override fun resume(token: String) {}

    override fun stop(token: String) {}
}
