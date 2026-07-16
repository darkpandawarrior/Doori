package com.mileway.feature.tracking.service

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.data.outbox.TripDraft
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.siddharth.kmp.offlineoutbox.DraftStatus
import kotlinx.coroutines.flow.first
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
    fun `backlogCount reflects only PENDING and RETRYING drafts, not SUBMITTED`() =
        runTest {
            val outbox = FakeTripDraftOutbox()
            val syncer = MilesSubmitSyncer(outbox, SavedTrackRepository(FakeMilesSubmitDao()), now = { 0L })
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, "route-a", draft("route-a"))
            outbox.enqueue(MilesSubmitSyncer.FORM_KEY, "route-b", draft("route-b"))
            outbox.markSubmitted(MilesSubmitSyncer.FORM_KEY, "route-b")

            assertEquals(1, syncer.backlogCount.first())
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
