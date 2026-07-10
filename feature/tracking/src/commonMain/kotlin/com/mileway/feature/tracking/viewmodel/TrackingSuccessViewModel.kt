package com.mileway.feature.tracking.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.ledger.PolicyRateEngine
import com.mileway.core.data.ledger.PolicyRateTable
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.repository.VoucherRecord
import com.mileway.feature.tracking.repository.VoucherRepository
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The raw submission facts the success screen was navigated with. Everything except the
 * reimbursement is display-only; the reimbursement is (re)computed here from [PolicyRateEngine] so
 * the screen shows a real policy-rate figure rather than the mock API's `reimbursableAmount`.
 */
data class TrackingSuccessArgs(
    val distanceKm: Double,
    val vehicleKey: String,
    val vehicleName: String,
    val startTime: Long,
    val endTime: Long,
    val transactionId: String?,
    val submissionStatus: String,
    val violationCount: Int,
    val violationMessage: String?,
)

@Stable
data class TrackingSuccessUiState(
    val distanceKm: Double = 0.0,
    val ratePerKm: Double = 0.0,
    val reimbursableAmount: Double = 0.0,
    val vehicleName: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val transactionId: String? = null,
    val submissionStatus: String = "SUCCESS",
    val violationCount: Int = 0,
    val violationMessage: String? = null,
    // Set once onCreateVoucher persists a DRAFT voucher; the screen then reveals the voucher card.
    val voucherNumber: String? = null,
    val voucherAmount: Double = 0.0,
)

sealed interface TrackingSuccessAction {
    data object TrackNewJourney : TrackingSuccessAction

    data object ViewExpense : TrackingSuccessAction

    data object CreateVoucher : TrackingSuccessAction

    /** P27.E.5: "Add Expense" CTA — logs a fresh expense against this just-completed trip. */
    data object AddExpense : TrackingSuccessAction
}

sealed interface TrackingSuccessEffect {
    /** Return to the tracking hub (saved-tracks list). */
    data object NavigateToHub : TrackingSuccessEffect

    /** Open the expense/voucher list — Mileway has no per-transaction detail screen yet. */
    data object NavigateToExpenseList : TrackingSuccessEffect

    /**
     * P27.E.5: open the expense-entry flow carrying this trip's [ExpenseSourceContext.Trip]. Built
     * here (not by the nav layer) so feature:tracking never depends on feature:logging — see
     * [ExpenseSourceContext]'s kdoc for why the context type itself lives in core:data.
     */
    data class NavigateToAddExpense(val context: ExpenseSourceContext) : TrackingSuccessEffect
}

/**
 * Wires the stateless `TrackingSuccessScreen` to real data: reimbursement comes from
 * [PolicyRateEngine] (rate table built from [VehiclePricingRepository]'s approved vehicles), and
 * the "Create Voucher" CTA persists a real DRAFT voucher via [VoucherRepository] instead of a
 * navigation-only stub. Navigation intents leave as [TrackingSuccessEffect]s.
 */
class TrackingSuccessViewModel(
    private val args: TrackingSuccessArgs,
    private val vehiclePricingRepository: VehiclePricingRepository,
    private val voucherRepository: VoucherRepository,
) : BaseViewModel<TrackingSuccessUiState, TrackingSuccessEffect, TrackingSuccessAction>(
        TrackingSuccessUiState(
            distanceKm = args.distanceKm,
            vehicleName = args.vehicleName,
            startTime = args.startTime,
            endTime = args.endTime,
            transactionId = args.transactionId,
            submissionStatus = args.submissionStatus,
            violationCount = args.violationCount,
            violationMessage = args.violationMessage,
        ),
    ) {
    init {
        computeReimbursement()
    }

    override fun onAction(action: TrackingSuccessAction) {
        when (action) {
            TrackingSuccessAction.TrackNewJourney -> emitEffect(TrackingSuccessEffect.NavigateToHub)
            TrackingSuccessAction.ViewExpense -> emitEffect(TrackingSuccessEffect.NavigateToExpenseList)
            TrackingSuccessAction.CreateVoucher -> createVoucher()
            TrackingSuccessAction.AddExpense -> addExpense()
        }
    }

    /**
     * P27.E.5: this screen carries no dedicated trip/route id (see [TrackingSuccessArgs]) — the
     * ledger [TrackingSuccessUiState.transactionId] is the only stable per-trip identifier already
     * threaded here, so it doubles as [ExpenseSourceContext.Trip.tripId]. A no-op when no
     * transaction was issued (e.g. a hard-stop submission), mirroring the existing
     * `hasTransaction` gate the screen already uses for its "View Expense" CTA.
     * ponytail: reusing transactionId keeps this a same-screen change; thread a real routeId
     * through TrackingSuccessArgs instead once a per-trip detail screen (V29) needs one.
     */
    private fun addExpense() {
        val tripId = currentState.transactionId ?: return
        emitEffect(
            TrackingSuccessEffect.NavigateToAddExpense(
                ExpenseSourceContext.Trip(tripId = tripId, tripLabel = currentState.vehicleName),
            ),
        )
    }

    private fun computeReimbursement() {
        viewModelScope.launch {
            val vehicles = runCatching { vehiclePricingRepository.getVehicles() }.getOrElse { emptyList() }
            val table =
                PolicyRateTable.fromApprovedVehicles(
                    vehicles = vehicles,
                    // ponytail: fixed fallback rate for vehicle keys with no approved-list pricing.
                    // Server-configurable per-tenant rates are a later (backend) phase; a constant is
                    // fine for offline/mock today.
                    defaultRatePerKm = DEFAULT_RATE_PER_KM,
                    // No global min/max caps in the offline dataset yet; the engine handles nulls.
                    minReimbursement = null,
                    maxReimbursement = null,
                )
            val result = PolicyRateEngine(table).reimbursement(args.vehicleKey, args.distanceKm)
            setState {
                copy(
                    ratePerKm = result.ratePerKm,
                    reimbursableAmount = result.cappedAmount.toDouble(),
                )
            }
        }
    }

    private fun createVoucher() {
        // Guard against a double-tap creating two vouchers for the same trip.
        if (currentState.voucherNumber != null) return
        val amount = currentState.reimbursableAmount
        viewModelScope.launch {
            val number = "V-${Clock.System.now().toEpochMilliseconds() % 10_000}"
            voucherRepository.save(
                VoucherRecord(
                    voucherNumber = number,
                    title = "Mileage – ${currentState.vehicleName}",
                    category = VoucherCategory.MILEAGE,
                    totalAmount = amount,
                    notes = "",
                    // Link the voucher to this trip's ledger transaction when one was issued.
                    expenseRouteIds = listOfNotNull(currentState.transactionId),
                    createdAtMs = Clock.System.now().toEpochMilliseconds(),
                    // Starts DRAFT (VoucherRecord's default); moving to PENDING is the Create Voucher
                    // flow's job, not this instant-CTA's.
                ),
            )
            setState { copy(voucherNumber = number, voucherAmount = amount) }
        }
    }

    companion object {
        // ponytail: single flat fallback ₹/km until per-tenant server config lands.
        const val DEFAULT_RATE_PER_KM = 8.0
    }
}
