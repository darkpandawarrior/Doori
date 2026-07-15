package com.mileway.feature.tracking.service

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.data.outbox.TripDraftOutbox
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.siddharth.kmp.offlineoutbox.DraftStatus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/** Outcome of one submit attempt — mirrors [SendOutcome], but SUCCESS carries the server response
 * so the online-submit path can read amount/transId/voucher/violations straight off it. */
sealed interface SubmitOutcome {
    data class Success(val response: ExpenseSubmissionResponse) : SubmitOutcome

    data object RetryableFailure : SubmitOutcome

    data object PermanentFailure : SubmitOutcome

    /** No eligible draft found for the requested routeId — already submitted, or nothing queued. */
    data object NotQueued : SubmitOutcome
}

/**
 * PLAN_V33 A5: durable trip *submission*, mirroring [LocationDataSyncer]'s outbox-drain shape for
 * the submit outbox. One [TripDraft] per trip (keyed by routeId, latest-write-wins — see
 * [TripDraftOutbox]), so unlike location batches there's no paging: [drain] with a [routeId]
 * resolves that one trip's outstanding draft (the online-submit path calls this right after
 * [enqueue] so it can show the real response immediately); [drain] with no routeId retries every
 * still-outstanding draft (the connectivity offline->online edge / background trigger).
 *
 * On a successful send, the draft is always marked SUBMITTED (the network call itself already
 * succeeded). [SavedTrackRepository.markSubmitted] only fires when the response needs no further
 * user resolution — a POLICY_VIOLATION/HARD_STOP response reaching this from an unattended
 * background drain has no interactive violation-ack flow to run, so the SavedTrack row is
 * deliberately left un-marked-submitted; a live MileageSubmissionViewModel's own
 * finalize()/ResolvePolicyAndFinalize path (unchanged by this class) remains the one source of
 * truth for that decision when the user is actually present.
 */
class MilesSubmitSyncer(
    private val outbox: TripDraftOutbox,
    private val trackRepository: SavedTrackRepository,
    private val now: () -> Long,
    private val send: suspend (TripDraft) -> SubmitOutcome = { SubmitOutcome.Success(ExpenseSubmissionResponse()) },
) {
    // ponytail: same single-dispatcher assumption as LocationDataSyncer's isDraining guard — every
    // current caller (ViewModel submit, SyncStatusViewModel's connectivity trigger) runs on the same
    // main/viewModelScope dispatcher, so a plain Boolean is enough; upgrade if that changes.
    private var isDraining = false

    suspend fun enqueue(draft: TripDraft) {
        outbox.enqueue(FORM_KEY, draft.routeId, draft)
    }

    /**
     * [routeId] `null` retries every still-outstanding draft (the background/connectivity trigger).
     * A non-null [routeId] resolves just that trip's draft and returns its real outcome so the
     * caller can react immediately (used right after [enqueue] on the online-submit path).
     */
    suspend fun drain(routeId: String? = null): SubmitOutcome {
        if (isDraining) return SubmitOutcome.NotQueued
        isDraining = true
        try {
            val drafts = outbox.drafts(FORM_KEY).first().filter { routeId == null || it.uniqueKey == routeId }
            var result: SubmitOutcome = SubmitOutcome.NotQueued
            for (entry in drafts) {
                if (!isRetryEligible(entry.status, entry.errorMessage)) continue
                val outcome = send(entry.payload)
                when (outcome) {
                    is SubmitOutcome.Success -> {
                        outbox.markSubmitted(FORM_KEY, entry.uniqueKey)
                        reconcile(entry.payload, outcome.response)
                    }
                    SubmitOutcome.PermanentFailure -> outbox.markFailed(FORM_KEY, entry.uniqueKey, PERMANENT_ERROR)
                    SubmitOutcome.RetryableFailure -> outbox.markFailed(FORM_KEY, entry.uniqueKey, RETRYABLE_ERROR)
                    SubmitOutcome.NotQueued -> Unit
                }
                if (entry.uniqueKey == routeId) result = outcome
            }
            return result
        } finally {
            isDraining = false
        }
    }

    private suspend fun reconcile(
        draft: TripDraft,
        response: ExpenseSubmissionResponse,
    ) {
        if (response.distance > 0.0 && abs(response.distance - draft.request.distance) > DISTANCE_RECONCILE_EPSILON_KM) {
            Napier.w {
                "MilesSubmitSyncer: server distance ${response.distance}km differs from tracked " +
                    "${draft.request.distance}km for route ${draft.routeId}"
            }
        }
        val needsResolution =
            response.submissionStatus == SubmissionStatus.POLICY_VIOLATION ||
                response.submissionStatus == SubmissionStatus.HARD_STOP
        if (!needsResolution) {
            trackRepository.markSubmitted(
                routeId = draft.routeId,
                transId = response.transId ?: "DEMO-${now()}",
                amount = response.reimbursableAmount ?: response.amount ?: 0.0,
            )
        }
    }

    private fun isRetryEligible(
        status: DraftStatus,
        errorMessage: String?,
    ) = status == DraftStatus.PENDING || (status == DraftStatus.FAILED && errorMessage == RETRYABLE_ERROR)

    companion object {
        const val FORM_KEY = "trip_draft"
        const val RETRYABLE_ERROR = "retryable"
        const val PERMANENT_ERROR = "permanent"
        const val DISTANCE_RECONCILE_EPSILON_KM = 0.05
    }
}
