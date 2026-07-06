package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.data.model.display.toDisplayData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class SavedTrackRepository(private val dao: SavedTrackDao) {
    /**
     * P2.2: when [accountId] is `null` (the default), every trip is returned — unchanged
     * behavior for existing call sites. When non-null, only trips started by that persona
     * (`started_by_account_id`) are returned, so the Journeys/Expenses tabs can be re-scoped to
     * the active account without breaking anywhere that still wants the unscoped list.
     */
    fun allTracksFlow(accountId: String? = null): Flow<List<TrackDisplayData>> =
        (if (accountId == null) dao.getAllSavedTracks() else dao.getAllSavedTracksByAccount(accountId))
            .map { list -> list.map { it.toDisplayData() } }

    fun completedTracksFlow(): Flow<List<TrackDisplayData>> = dao.getCompletedTracks().map { list -> list.map { it.toDisplayData() } }

    /**
     * Every raw [SavedTrack] row, unmapped — for local logic that needs more than
     * [TrackDisplayData] can give it (e.g. the multi-session restore gatherer).
     */
    fun rawTracksFlow(): Flow<List<SavedTrack>> = dao.getAllSavedTracks()

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

    // P3.3: already-claimed guard — stamps every trip selected into a just-submitted voucher with
    // that voucher's number so it's excluded from a subsequent Create Voucher selection list.
    suspend fun markClaimedByVoucher(
        routeIds: List<String>,
        voucherNumber: String,
    ) {
        routeIds.forEach { dao.markClaimedByVoucher(it, voucherNumber) }
    }

    // P6.1: persists the odometer-not-working fallback flag at submission time, so the existing
    // Room column stops being dead.
    suspend fun markOdometerNotWorking(routeId: String): Int = dao.markOdometerNotWorking(routeId)

    // Wave-2 SmartDistanceAnalysis: persists the user-approved final distance once reductions are applied.
    suspend fun updateSmartDistanceFinal(
        routeId: String,
        value: Double,
    ) = dao.updateSmartDistanceFinal(routeId, value)

    /**
     * Wave-4 §2.3: local-data resolution for a journey's GPS trail. [MileageMaintenanceTask]
     * purges the `locations` rows for old, already-uploaded trips and flips `has_local_data` off
     * (see its doc comment); this is the read-side counterpart that call sites (e.g. the route
     * detail screen) use instead of reaching into the DAO directly.
     *
     * Local points are the only data source today — there is no real backend yet (see the
     * project's "backend deferred" policy) — so a purged route resolves to
     * [LocalDataResolution.WouldFetchFromServer] as an honest, explicit stub rather than silently
     * returning an empty list.
     */
    suspend fun resolveLocalData(routeId: String): LocalDataResolution {
        val track = dao.getSavedTrackById(routeId) ?: return LocalDataResolution.NotFound
        return if (track.hasLocalData) {
            LocalDataResolution.Local(routeId)
        } else {
            LocalDataResolution.WouldFetchFromServer(routeId)
        }
    }
}

/** Result of [SavedTrackRepository.resolveLocalData]. */
sealed interface LocalDataResolution {
    data class Local(val routeId: String) : LocalDataResolution

    // ponytail: no real backend yet — this is the honest stub the "has_local_data" flag exists to
    // enable; the future server-fetch call slots in here without touching any caller.
    data class WouldFetchFromServer(val routeId: String) : LocalDataResolution

    data object NotFound : LocalDataResolution
}
