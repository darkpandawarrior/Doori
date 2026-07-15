package com.mileway.feature.tracking.service

import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.db.TrackMetrics
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.data.outbox.TripDraftOutbox
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.siddharth.kmp.offlineoutbox.DraftEntry
import com.siddharth.kmp.offlineoutbox.DraftStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * PLAN_V33 A5: [MilesSubmitSyncer] — durable trip submission, mirroring [LocationDataSyncerTest]'s
 * retry-policy coverage for the submit outbox (one draft per routeId, no batching/paging needed).
 */
class MilesSubmitSyncerTest {
    private fun draft(routeId: String = "route-1") = TripDraft(routeId, SubmitMilesRequestK(token = routeId, distance = 10.0))

    @Test
    fun `a successful send marks the draft SUBMITTED and reconciles the SavedTrack`() =
        runTest {
            val outbox = FakeTripDraftOutbox()
            val dao = FakeMilesSubmitDao()
            val response = ExpenseSubmissionResponse(transId = "TXN-1", reimbursableAmount = 42.0)
            val syncer = MilesSubmitSyncer(outbox, SavedTrackRepository(dao), now = { 0L }, send = { SubmitOutcome.Success(response) })
            val d = draft()
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, d.routeId, d)

            val outcome = syncer.drain(d.routeId)

            assertIs<SubmitOutcome.Success>(outcome)
            assertEquals(DraftStatus.SUBMITTED, outbox.statusFor(d.routeId))
            assertEquals(listOf<Triple<String, String?, Double>>(Triple("route-1", "TXN-1", 42.0)), dao.completed)
        }

    @Test
    fun `a retryable failure leaves the draft queued and is attempted again on the next drain`() =
        runTest {
            val outbox = FakeTripDraftOutbox()
            val dao = FakeMilesSubmitDao()
            var attempts = 0
            val syncer =
                MilesSubmitSyncer(
                    outbox,
                    SavedTrackRepository(dao),
                    now = { 0L },
                    send = {
                        attempts++
                        SubmitOutcome.RetryableFailure
                    },
                )
            val d = draft()
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, d.routeId, d)

            syncer.drain(d.routeId)
            syncer.drain(d.routeId)

            assertEquals(DraftStatus.FAILED, outbox.statusFor(d.routeId))
            assertEquals(2, attempts, "a retryable failure must still be retried on the next drain")
            assertTrue(dao.completed.isEmpty())
        }

    @Test
    fun `a permanent failure (409-5xx-equivalent) is not retried on a later drain`() =
        runTest {
            val outbox = FakeTripDraftOutbox()
            val dao = FakeMilesSubmitDao()
            var attempts = 0
            val syncer =
                MilesSubmitSyncer(
                    outbox,
                    SavedTrackRepository(dao),
                    now = { 0L },
                    send = {
                        attempts++
                        SubmitOutcome.PermanentFailure
                    },
                )
            val d = draft()
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, d.routeId, d)

            syncer.drain(d.routeId)
            syncer.drain(d.routeId)

            assertEquals(1, attempts, "a permanently-failed draft must not be retried")
            assertEquals(DraftStatus.FAILED, outbox.statusFor(d.routeId))
            assertTrue(dao.completed.isEmpty())
        }

    @Test
    fun `drain with no routeId retries every outstanding draft - the offline-to-online trigger`() =
        runTest {
            val outbox = FakeTripDraftOutbox()
            val dao = FakeMilesSubmitDao()
            val sent = mutableListOf<String>()
            val syncer =
                MilesSubmitSyncer(
                    outbox,
                    SavedTrackRepository(dao),
                    now = { 0L },
                    send = { d ->
                        sent += d.routeId
                        SubmitOutcome.Success(ExpenseSubmissionResponse(transId = "TXN-${d.routeId}", reimbursableAmount = 1.0))
                    },
                )
            // Simulates two trips submitted while offline (enqueued, never drained yet).
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, "route-a", draft("route-a"))
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, "route-b", draft("route-b"))

            syncer.drain() // the connectivity offline->online edge trigger

            assertEquals(setOf("route-a", "route-b"), sent.toSet())
            assertEquals(DraftStatus.SUBMITTED, outbox.statusFor("route-a"))
            assertEquals(DraftStatus.SUBMITTED, outbox.statusFor("route-b"))
            assertEquals(2, dao.completed.size)
        }

    @Test
    fun `a policy-violation response is not reconciled into SavedTrack from an unattended drain`() =
        runTest {
            val outbox = FakeTripDraftOutbox()
            val dao = FakeMilesSubmitDao()
            val response = ExpenseSubmissionResponse(transId = "TXN-V", submissionStatus = SubmissionStatus.POLICY_VIOLATION)
            val syncer = MilesSubmitSyncer(outbox, SavedTrackRepository(dao), now = { 0L }, send = { SubmitOutcome.Success(response) })
            val d = draft()
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, d.routeId, d)

            syncer.drain(d.routeId)

            // The network call succeeded (the draft is done), but a violation needs a user's ack
            // that an unattended background drain can't provide — the SavedTrack row is left
            // un-submitted; a live MileageSubmissionViewModel's finalize()/ResolvePolicyAndFinalize
            // path remains the one source of truth for that decision when the user is present.
            assertEquals(DraftStatus.SUBMITTED, outbox.statusFor(d.routeId))
            assertTrue(dao.completed.isEmpty())
        }
}

private class FakeTripDraftOutbox : TripDraftOutbox {
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
private class FakeMilesSubmitDao : SavedTrackDao {
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
}
