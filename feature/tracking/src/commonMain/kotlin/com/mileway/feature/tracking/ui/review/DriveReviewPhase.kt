@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.tracking.ui.review

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.feature.tracking.ui.navigation.SubmissionResult
import com.mileway.feature.tracking.viewmodel.SubmissionUiState

/** A [DriveReviewSheet] row that supports inline correction from the REVIEW phase. */
enum class ReviewField { CLASSIFICATION, PURPOSE, ATTACHMENTS, ODOMETER }

/**
 * Business vs personal is a structured choice (this enum), never free text — it changes
 * reimbursement, so it is persisted the same way [ReviewField.PURPOSE] is: through the existing
 * `SetFormValue` channel, keyed `"classification"`.
 */
enum class TripClassification { BUSINESS, PERSONAL }

/**
 * The five states [DriveReviewSheet] can be in, every one of them rendered without leaving the
 * sheet. SUBMITTING/SUCCESS/FAILED are a direct, lossless view of [SubmissionUiState] — this type
 * never invents a parallel copy of "is the network call in flight", it only adds the two states
 * [SubmissionUiState] has no concept of: [Review] (resting) and [Editing] (a field expanded
 * in place). See [derivePhase].
 */
sealed interface DriveReviewPhase {
    data object Review : DriveReviewPhase

    data class Editing(val field: ReviewField) : DriveReviewPhase

    data object Submitting : DriveReviewPhase

    data class Success(val result: SubmissionResult) : DriveReviewPhase

    /**
     * @param queued True when this is an offline submission durably queued for auto-sync
     *   ([SubmissionUiState.Queued]) — must read as "saved, will sync", never as an error. False
     *   for a real [SubmissionUiState.Error].
     */
    data class Failed(val message: String, val queued: Boolean) : DriveReviewPhase
}

/** Copy shown for a durably-queued offline submission — deliberately not error language. */
const val QUEUED_SAVE_MESSAGE = "Saved. This journey will sync automatically once you're back online."

/**
 * Sheet-local UI state that sits alongside (never inside) the ViewModel's [SubmissionUiState]:
 * which field is expanded for inline editing, the classification choice, and whether the sheet
 * itself is visible.
 *
 * Nothing in here can discard the tracked drive — there is no field, transition or action in this
 * type that touches drive/track data at all, so dismissing the sheet ([dismiss]) can never lose
 * it. [visible] only controls the sheet's own presentation.
 */
data class DriveReviewLocalState(
    val visible: Boolean = true,
    val editingField: ReviewField? = null,
    val classification: TripClassification = TripClassification.BUSINESS,
)

/** Expand [field] inline for correction. */
fun DriveReviewLocalState.edit(field: ReviewField): DriveReviewLocalState = copy(editingField = field)

/** Collapse whichever field is expanded, returning to the plain REVIEW row list. */
fun DriveReviewLocalState.finishEditing(): DriveReviewLocalState = copy(editingField = null)

/** Set the business/personal choice and collapse the row (a single tap resolves it). */
fun DriveReviewLocalState.classify(value: TripClassification): DriveReviewLocalState = copy(classification = value, editingField = null)

/**
 * Hide the sheet. Deliberately touches only [visible] — [editingField] and [classification]
 * survive untouched, so [reopen] restores exactly what the user left, and the drive underneath
 * (owned entirely by the ViewModel/repository, never by this type) is never in question.
 */
fun DriveReviewLocalState.dismiss(): DriveReviewLocalState = copy(visible = false)

/** Reopen a dismissed sheet with its prior edit-in-progress state intact. */
fun DriveReviewLocalState.reopen(): DriveReviewLocalState = copy(visible = true)

/**
 * Derives the sheet's [DriveReviewPhase] from the ViewModel's [submissionState] plus [local]'s
 * editingField — the one place the two are reconciled. [submissionState] wins the moment it
 * leaves [SubmissionUiState.Idle]: once a submission is in flight, expanding a field stops
 * meaning anything, so REVIEW/EDITING both collapse into SUBMITTING/SUCCESS/FAILED as soon as it
 * does.
 */
fun derivePhase(
    submissionState: SubmissionUiState,
    local: DriveReviewLocalState,
    toResult: (ExpenseSubmissionResponse) -> SubmissionResult,
): DriveReviewPhase =
    when (submissionState) {
        SubmissionUiState.Submitting -> DriveReviewPhase.Submitting
        is SubmissionUiState.Success -> DriveReviewPhase.Success(toResult(submissionState.response))
        SubmissionUiState.Queued -> DriveReviewPhase.Failed(message = QUEUED_SAVE_MESSAGE, queued = true)
        is SubmissionUiState.Error -> DriveReviewPhase.Failed(message = submissionState.message, queued = false)
        SubmissionUiState.Idle ->
            local.editingField?.let { DriveReviewPhase.Editing(it) } ?: DriveReviewPhase.Review
    }

/**
 * Maps a raw [ExpenseSubmissionResponse] into the platform-neutral [SubmissionResult] that
 * [TrackingSuccessScreen][com.mileway.feature.tracking.ui.screens.TrackingSuccessScreen] (reused
 * verbatim for the SUCCESS phase) renders. Mirrors the mapping `TrackSubmissionScreen` builds on
 * a successful submission, so the SUCCESS phase shows exactly what that screen showed.
 */
fun ExpenseSubmissionResponse.toSubmissionResult(
    distanceKm: Double,
    vehicleKey: String,
    vehicleName: String,
    startTime: Long,
    endTime: Long,
): SubmissionResult =
    SubmissionResult(
        distanceKm = distanceKm,
        reimbursableAmount = reimbursableAmount ?: 0.0,
        vehicleKey = vehicleKey,
        vehicleName = vehicleName.ifBlank { vehicleKey },
        startTime = startTime,
        endTime = endTime,
        transactionId = transId ?: transaction?.id,
        submissionStatus = submissionStatus.name,
        violationCount = violations.size,
        violationMessage = violations.firstOrNull()?.message,
        voucherNumber = issuedVoucher?.number,
        voucherAmount = issuedVoucher?.amount ?: 0.0,
    )
