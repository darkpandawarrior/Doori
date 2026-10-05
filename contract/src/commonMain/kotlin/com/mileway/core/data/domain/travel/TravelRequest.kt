package com.mileway.core.data.domain.travel

import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.ReportLifecycleStateMachine
import com.mileway.core.data.domain.policy.AnnualMileageDistance
import com.mileway.core.data.domain.policy.MileageDistanceUnit
import com.mileway.core.data.domain.policy.MileageRateMirror
import com.mileway.core.data.domain.policy.checkedRateProduct
import com.mileway.core.data.domain.policy.checkedRateSum
import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.time.Instant

private const val MILLIMETRES_PER_KM = 1_000_000L
private const val MILLIMETRES_PER_MILE = 1_609_344L
private const val RATE_SCALE = 1_000L
private const val ISO_DATE_LENGTH = 10
private const val HALF_MINOR_UNIT = 0.5

/** Frozen route and dated rate evidence. The estimate is authorization only, never a payable claim. */
@Serializable
data class TravelEstimate(
    val distanceKm: Double,
    val approximate: Boolean,
    val amountMinor: Long,
    val currency: String,
    val rateEffectiveFrom: String,
    val authority: String,
    val sourceUrl: String,
    val distanceUnit: MileageDistanceUnit,
    val annualDistanceBeforeUnits: Long,
) {
    init {
        require(distanceKm.isFinite() && distanceKm > 0 && amountMinor >= 0) { "Estimate must contain a valid distance and amount" }
        require(currency.isNotBlank() && authority.isNotBlank() && sourceUrl.startsWith("https://")) { "Dated rate citation is required" }
        travelDateMillis(rateEffectiveFrom)
        require(annualDistanceBeforeUnits >= 0) { "Annual distance must be nonnegative" }
    }
}

/** Strict travel date, independent of when the request is submitted. */
fun travelDateMillis(date: String): Long {
    require(date.length == ISO_DATE_LENGTH) { "Enter a travel date as YYYY-MM-DD" }
    val instant = Instant.parse("${date}T00:00:00Z")
    require(instant.toString().substringBefore('T') == date) { "Invalid travel date" }
    return instant.toEpochMilliseconds()
}

/**
 * Prices fractional route distance with the travel-date version of the existing rate mirror.
 * Rounds route distance to millimetres, converts units explicitly and splits annual bands.
 * Checked integer products round half-up once to minor units, without floating-point money math.
 */
fun estimateTravel(
    distanceKm: Double,
    approximate: Boolean,
    travelDate: String,
    mirror: MileageRateMirror,
    annualDistance: AnnualMileageDistance? = null,
): TravelEstimate {
    require(distanceKm.isFinite() && distanceKm > 0) { "Enter a positive finite distance" }
    val at = travelDateMillis(travelDate)
    val version = mirror.versionFor(at)
    val schedule = version.schedule
    val period = mirror.annualPeriod.startFor(at)
    annualDistance?.let {
        require(mirror.annualPeriod.startFor(travelDateMillis(it.periodStart)) == it.periodStart) { "Annual period is misaligned" }
        require(it.periodStart <= period) { "Annual distance belongs to a future period" }
    }
    val before = annualDistance?.takeIf { it.periodStart == period }?.distanceUnits ?: 0
    val scaledDistance = distanceKm * MILLIMETRES_PER_KM
    require(scaledDistance.isFinite() && scaledDistance < Long.MAX_VALUE.toDouble()) { "Distance is too large" }
    val distanceMm = floor(scaledDistance + HALF_MINOR_UNIT).toLong()
    require(distanceMm > 0) { "Distance must be at least one millimetre" }
    val mmPerUnit = if (schedule.distanceUnit == MileageDistanceUnit.MILE) MILLIMETRES_PER_MILE else MILLIMETRES_PER_KM
    val firstDistanceMm =
        schedule.firstBandDistanceUnits?.let {
            minOf(distanceMm, checkedRateProduct((it - before).coerceAtLeast(0), mmPerUnit))
        } ?: distanceMm
    val firstAmount = checkedRateProduct(firstDistanceMm, schedule.firstRateThousandthsMinor)
    val aboveAmount = checkedRateProduct(distanceMm - firstDistanceMm, schedule.aboveBandRateThousandthsMinor ?: schedule.firstRateThousandthsMinor)
    val total = checkedRateSum(firstAmount, aboveAmount)
    val denominator = mmPerUnit * RATE_SCALE
    val amountMinor = total / denominator + if (total % denominator >= denominator / 2) 1 else 0
    return TravelEstimate(
        distanceMm.toDouble() / MILLIMETRES_PER_KM,
        approximate,
        amountMinor,
        mirror.currency,
        version.effectiveFrom,
        version.authority,
        mirror.sourceUrl,
        schedule.distanceUnit,
        before,
    )
}

/** Session-local authorization with the same ordered approval history and transition table as a report. */
@Serializable
data class TravelRequest(
    val id: String,
    val employeeId: String,
    val purpose: String,
    val travelDate: String,
    val estimate: TravelEstimate,
    val state: ReportLifecycleState = ReportLifecycleState.DRAFT,
    val approvalChain: ApprovalChain = ApprovalChain(),
    val recordVersion: Long = 0,
) {
    init {
        require(id.isNotBlank() && employeeId.isNotBlank() && purpose.isNotBlank()) { "Request identity and purpose are required" }
        travelDateMillis(travelDate)
        require(recordVersion >= 0) { "Request version must be nonnegative" }
        require(state != ReportLifecycleState.PAID && state != ReportLifecycleState.APPROVED_FOR_PAYMENT) { "Travel authorization cannot be payable" }
    }

    /** Reuses report transitions but refuses every payment event for an authorization. */
    fun transition(event: ReportLifecycleEvent): TravelRequest {
        require(event != ReportLifecycleEvent.REIMBURSE && event != ReportLifecycleEvent.RELEASE_FOR_PAYMENT) { "Travel authorization cannot be paid" }
        require(event != ReportLifecycleEvent.RECALL || approvalChain.steps.isEmpty()) { "A reviewed request cannot be recalled" }
        require(recordVersion < Long.MAX_VALUE) { "Request version exhausted" }
        return copy(state = ReportLifecycleStateMachine.transition(state, event), recordVersion = recordVersion + 1)
    }

    /** Empty-line projection for shared review routing and notifications, never saved to ReportRepository. */
    fun lifecycleReport(): Report = Report(id, employeeId, state = state, approvalChain = approvalChain, recordVersion = recordVersion)
}
