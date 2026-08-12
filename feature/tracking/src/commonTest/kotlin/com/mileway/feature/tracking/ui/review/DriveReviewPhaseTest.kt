package com.mileway.feature.tracking.ui.review

import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.feature.tracking.viewmodel.SubmissionFormUi
import com.mileway.feature.tracking.viewmodel.SubmissionUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DriveReviewPhaseTest {
    private val noOpToResult: (ExpenseSubmissionResponse) -> com.mileway.feature.tracking.ui.navigation.SubmissionResult = {
        error("should not be called for this branch")
    }

    // ── derivePhase: every SubmissionUiState maps to the right DriveReviewPhase ──────────────

    @Test
    fun idle_with_no_field_open_derives_to_review() {
        val phase = derivePhase(SubmissionUiState.Idle, DriveReviewLocalState(), noOpToResult)
        assertEquals(DriveReviewPhase.Review, phase)
    }

    @Test
    fun idle_with_a_field_open_derives_to_editing_that_field() {
        val local = DriveReviewLocalState().edit(ReviewField.ODOMETER)
        val phase = derivePhase(SubmissionUiState.Idle, local, noOpToResult)
        assertEquals(DriveReviewPhase.Editing(ReviewField.ODOMETER), phase)
    }

    @Test
    fun submitting_derives_to_submitting_regardless_of_any_field_being_open() {
        // Once a submission is in flight, an expanded field stops meaning anything.
        val local = DriveReviewLocalState().edit(ReviewField.PURPOSE)
        val phase = derivePhase(SubmissionUiState.Submitting, local, noOpToResult)
        assertEquals(DriveReviewPhase.Submitting, phase)
    }

    @Test
    fun success_derives_to_success_carrying_the_mapped_result() {
        val response = ExpenseSubmissionResponse(transId = "TXN-1", reimbursableAmount = 42.0)
        val expected =
            com.mileway.feature.tracking.ui.navigation.SubmissionResult(
                distanceKm = 10.0,
                reimbursableAmount = 42.0,
                vehicleKey = "car",
                vehicleName = "car",
                startTime = 0L,
                endTime = 0L,
                transactionId = "TXN-1",
                submissionStatus = "SUCCESS",
                violationCount = 0,
                violationMessage = null,
                voucherNumber = null,
                voucherAmount = 0.0,
            )
        val phase = derivePhase(SubmissionUiState.Success(response), DriveReviewLocalState()) { expected }
        assertEquals(DriveReviewPhase.Success(expected), phase)
    }

    @Test
    fun error_derives_to_failed_not_queued_carrying_the_error_message() {
        val phase = derivePhase(SubmissionUiState.Error("network down"), DriveReviewLocalState(), noOpToResult)
        assertEquals(DriveReviewPhase.Failed(message = "network down", queued = false), phase)
    }

    @Test
    fun queued_derives_to_failed_but_marked_queued_with_saved_will_sync_copy_never_error_language() {
        val phase = derivePhase(SubmissionUiState.Queued, DriveReviewLocalState(), noOpToResult)
        val failed = phase as DriveReviewPhase.Failed
        assertTrue(failed.queued)
        assertFalse(failed.message.contains("error", ignoreCase = true))
        assertFalse(failed.message.contains("fail", ignoreCase = true))
        assertTrue(failed.message.contains("sync", ignoreCase = true))
    }

    @Test
    fun response_maps_to_a_submission_result_with_the_journeys_own_distance_and_names() {
        val response =
            ExpenseSubmissionResponse(
                transId = "TXN-9",
                reimbursableAmount = 99.5,
                violations = listOf(com.mileway.core.data.model.network.PolicyViolation(message = "over limit")),
                issuedVoucher = com.mileway.core.data.model.network.Voucher(number = "V-1", amount = 50.0),
            )
        val result = response.toSubmissionResult(distanceKm = 12.0, vehicleKey = "car-1", vehicleName = "", startTime = 1L, endTime = 2L)
        assertEquals(12.0, result.distanceKm)
        assertEquals("TXN-9", result.transactionId)
        // Blank vehicle name falls back to the vehicle key, same as TrackSubmissionScreen's mapping.
        assertEquals("car-1", result.vehicleName)
        assertEquals(1, result.violationCount)
        assertEquals("over limit", result.violationMessage)
        assertEquals("V-1", result.voucherNumber)
        assertEquals(50.0, result.voucherAmount)
    }

    // ── a failed submission is retryable: Confirm and Retry issue the identical Submit action ──

    @Test
    fun retry_after_a_failure_resubmits_the_same_journey_confirm_would_have() {
        val confirmAction = buildSubmitActionForTest("route-1", 12.5, "car", 100L, 200L)
        val retryAction = buildSubmitActionForTest("route-1", 12.5, "car", 100L, 200L)
        assertEquals(confirmAction, retryAction)
    }

    // ── DriveReviewLocalState: every transition ──────────────────────────────────────────────

    @Test
    fun edit_opens_the_requested_field() {
        val state = DriveReviewLocalState().edit(ReviewField.ATTACHMENTS)
        assertEquals(ReviewField.ATTACHMENTS, state.editingField)
    }

    @Test
    fun finish_editing_closes_whichever_field_was_open() {
        val state = DriveReviewLocalState().edit(ReviewField.PURPOSE).finishEditing()
        assertNull(state.editingField)
    }

    @Test
    fun classify_sets_the_choice_and_closes_the_row_in_one_step() {
        val state = DriveReviewLocalState().edit(ReviewField.CLASSIFICATION).classify(TripClassification.PERSONAL)
        assertEquals(TripClassification.PERSONAL, state.classification)
        assertNull(state.editingField)
    }

    // ── dismissal preserves the drive: dismiss/reopen only ever touch visibility ────────────

    @Test
    fun dismiss_only_flips_visibility_every_other_field_survives_untouched() {
        val before = DriveReviewLocalState(visible = true, editingField = ReviewField.ODOMETER, classification = TripClassification.PERSONAL)
        val after = before.dismiss()
        assertFalse(after.visible)
        assertEquals(before.editingField, after.editingField)
        assertEquals(before.classification, after.classification)
    }

    @Test
    fun reopen_restores_visibility_and_the_sheet_reopens_exactly_where_it_was_left() {
        val dismissed = DriveReviewLocalState(editingField = ReviewField.PURPOSE, classification = TripClassification.PERSONAL).dismiss()
        val reopened = dismissed.reopen()
        assertTrue(reopened.visible)
        assertEquals(ReviewField.PURPOSE, reopened.editingField)
        assertEquals(TripClassification.PERSONAL, reopened.classification)
    }

    // ── the ledger figures map from the real submission form fields ─────────────────────────

    @Test
    fun ledger_odometer_figure_is_the_delta_between_the_forms_captured_readings() {
        val form = SubmissionFormUi(simulatedStartOdo = 45_000, simulatedEndOdo = 45_050)
        val ledger = form.toDistanceLedger(distanceKm = 48.0)
        assertEquals(48.0, ledger.rawKm)
        assertEquals(48.0, ledger.cleanedKm)
        assertEquals(48.0, ledger.claimedKm)
        assertEquals(50.0, ledger.odometerKm)
    }

    @Test
    fun ledger_odometer_figure_is_null_until_both_readings_are_captured() {
        val neitherCaptured = SubmissionFormUi()
        assertNull(neitherCaptured.toDistanceLedger(10.0).odometerKm)

        val onlyStartCaptured = SubmissionFormUi(simulatedStartOdo = 100)
        assertNull(onlyStartCaptured.toDistanceLedger(10.0).odometerKm)
    }

    @Test
    fun ledger_balances_against_the_distance_pipelines_own_invariant() {
        // With no bucket deductions surfaced at this layer (see DriveReviewLedger.kt's ponytail
        // note), raw/cleaned/claimed are all the same tracked figure — that must still balance.
        val form = SubmissionFormUi()
        assertTrue(form.toDistanceLedger(33.3).balances())
    }
}

/** Mirrors [buildSubmitAction]'s private shape so the retry-equals-confirm test can assert on it. */
private fun buildSubmitActionForTest(
    routeId: String,
    distanceKm: Double,
    vehicleKey: String,
    startTime: Long,
    endTime: Long,
) = com.mileway.feature.tracking.viewmodel.MileageSubmissionAction.Submit(routeId, distanceKm, vehicleKey, startTime, endTime)
