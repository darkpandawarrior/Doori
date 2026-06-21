package com.miletracker.stub

import com.miletracker.core.data.model.network.ExpenseSubmissionResponse
import com.miletracker.core.network.model.BusinessEntity
import com.miletracker.core.network.model.Office
import com.miletracker.core.network.model.PolicyViolation
import com.miletracker.core.network.model.SubmissionStatus
import com.miletracker.core.network.model.TransactionRef
import com.miletracker.core.network.model.ViolationSeverity
import com.miletracker.core.network.model.Voucher
import com.miletracker.core.network.model.VoucherDeclaration
import com.miletracker.core.network.model.VoucherStatus
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Deterministic policy-engine mock for the offline demo.
 *
 * Submission outcome buckets (keyed off the submitted distance, never time or randomness):
 *
 * | distanceKm        | outcome               |
 * |-------------------|-----------------------|
 * | d < 5             | SUCCESS               |
 * | 5 <= d < 10       | REIMBURSABLE_ADJUSTED |
 * | 10 <= d < 15      | POLICY_VIOLATION      |
 * | 15 <= d <= 40     | NEEDS_APPROVAL        |
 * | d > 40            | HARD_STOP             |
 *
 * All ids and timestamps are derived from a stable seed of (distance, token) so the same
 * request always produces the same voucher / transaction. HARD_STOP submissions are blocked
 * by the policy engine, so no voucher or transaction is created for them.
 */
object PolicyMockData {
    /** Reimbursement rate per km — matches [DemoMockData.submissionResponse]. */
    const val RATE_PER_KM = 10.0

    /** Daily reimbursable distance ceiling enforced by the demo policy. */
    const val MAX_REIMBURSABLE_KM_PER_DAY = 10.0

    /** Distances above this cannot be submitted at all (HARD_STOP). */
    const val HARD_STOP_KM = 40.0

    /** Fixed demo epoch (2025-01-01T00:00:00Z) used to derive deterministic timestamps. */
    const val DEMO_EPOCH_MILLIS = 1_735_689_600_000L

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val DEFAULT_TOKEN = "demo-token"
    private const val SERVICE_TAG_MILEAGE = "MILEAGE"

    // ── Offices & entities ────────────────────────────────────────────────────

    /** Four registered offices across two cities (codes 1345 / 1347 / 1349 / 5356). */
    fun offices(): List<Office> =
        listOf(
            Office(
                code = "1345",
                name = "Pune Head Office",
                address = "Tower A, Baner Road, Pune, Maharashtra 411045",
                gstin = "27AAACM4521K1Z3",
            ),
            Office(
                code = "1347",
                name = "Pune Supply Center",
                address = "Plot 12, MIDC Bhosari, Pune, Maharashtra 411026",
                gstin = "27AAACM4521K2Z1",
            ),
            Office(
                code = "1349",
                name = "Mumbai Regional Office",
                address = "4th Floor, Bandra Kurla Complex, Mumbai, Maharashtra 400051",
                gstin = "27AAACM4521K3ZX",
            ),
            Office(
                code = "5356",
                name = "Mumbai Distribution Hub",
                address = "Unit 7, Andheri East, Mumbai, Maharashtra 400069",
                gstin = "27AAACM4521K4Z8",
            ),
        )

    /** Business entities the demo workspace can bill against, with mixed currencies. */
    fun businessEntities(): List<BusinessEntity> =
        listOf(
            BusinessEntity(name = "Mileway India Pvt Ltd", country = "India", currencySymbol = "₹"),
            BusinessEntity(name = "Mileway USA Inc", country = "United States", currencySymbol = "$"),
            BusinessEntity(name = "Mileway UK Ltd", country = "United Kingdom", currencySymbol = "£"),
            BusinessEntity(name = "Mileway Europe GmbH", country = "Germany", currencySymbol = "€"),
            BusinessEntity(name = "Mileway Japan KK", country = "Japan", currencySymbol = "¥"),
            BusinessEntity(name = "Mileway Singapore Pte Ltd", country = "Singapore", currencySymbol = "S$"),
            BusinessEntity(name = "Mileway Middle East FZE", country = "United Arab Emirates", currencySymbol = "AED"),
        )

    // ── Outcome rotation ──────────────────────────────────────────────────────

    /** Maps a submitted distance to a [SubmissionStatus] using the buckets documented above. */
    fun outcomeFor(distanceKm: Double): SubmissionStatus =
        when {
            distanceKm < 5.0 -> SubmissionStatus.SUCCESS
            distanceKm < 10.0 -> SubmissionStatus.REIMBURSABLE_ADJUSTED
            distanceKm < 15.0 -> SubmissionStatus.POLICY_VIOLATION
            distanceKm > HARD_STOP_KM -> SubmissionStatus.HARD_STOP
            else -> SubmissionStatus.NEEDS_APPROVAL
        }

    /**
     * Violations attached to a submission. Only the violation statuses
     * (POLICY_VIOLATION, HARD_STOP) carry violations; every other bucket returns an
     * empty list.
     */
    fun violationsFor(distanceKm: Double): List<PolicyViolation> =
        when (outcomeFor(distanceKm)) {
            SubmissionStatus.POLICY_VIOLATION ->
                listOf(
                    PolicyViolation(
                        id = "max-distance-per-day",
                        title = "Daily distance limit exceeded",
                        message = "Only the first 10 km per day are reimbursable under the mileage policy.",
                        severity = ViolationSeverity.VIOLATION,
                    ),
                )
            SubmissionStatus.HARD_STOP ->
                listOf(
                    PolicyViolation(
                        id = "max-trip-distance-hard-stop",
                        title = "Trip distance hard limit exceeded",
                        message = "Trips above 40 km cannot be submitted for reimbursement.",
                        severity = ViolationSeverity.HARDSTOP,
                    ),
                )
            else -> emptyList()
        }

    // ── Voucher / transaction factories ───────────────────────────────────────

    /**
     * Voucher created for the submission, or null for HARD_STOP (the submission is blocked).
     * The voucher amount is capped at the [MAX_REIMBURSABLE_KM_PER_DAY] policy ceiling.
     */
    fun voucherFor(
        distanceKm: Double,
        token: String? = null,
    ): Voucher? {
        if (outcomeFor(distanceKm) == SubmissionStatus.HARD_STOP) return null
        val seed = seedFor(distanceKm, token)
        return Voucher(
            id = (seed % 9_000_000L) + 1_000_000L,
            number = "VCH-" + (seed % 1_000_000L).toString().padStart(6, '0'),
            amount = reimbursableAmountFor(distanceKm),
            status = VoucherStatus.entries[(seed % 3).toInt()],
        )
    }

    /**
     * Ledger transaction created for the submission, or null for HARD_STOP.
     * The id format mirrors a real ledger reference, e.g. `O-INDIAN-000048769`.
     */
    fun transactionFor(
        distanceKm: Double,
        token: String? = null,
    ): TransactionRef? {
        if (outcomeFor(distanceKm) == SubmissionStatus.HARD_STOP) return null
        val seed = seedFor(distanceKm, token)
        return TransactionRef(
            id = "O-INDIAN-" + (seed % 1_000_000_000L).toString().padStart(9, '0'),
            createdAtMillis = DEMO_EPOCH_MILLIS + (seed % MILLIS_PER_DAY),
            amount = reimbursableAmountFor(distanceKm),
            serviceTag = SERVICE_TAG_MILEAGE,
        )
    }

    /** Declaration every voucher filing must acknowledge. */
    fun voucherDeclaration(): VoucherDeclaration =
        VoucherDeclaration(
            text =
                "I declare that the distance claimed was travelled for business purposes " +
                    "and complies with the mileage reimbursement policy.",
            requiresAcknowledgement = true,
        )

    // ── Response enrichment ───────────────────────────────────────────────────

    /**
     * Returns a copy of [base] with the policy-outcome fields populated. All pre-existing
     * fields of [base] are left untouched, so legacy consumers see identical behaviour.
     */
    fun enrich(
        base: ExpenseSubmissionResponse,
        distanceKm: Double,
        token: String? = null,
    ): ExpenseSubmissionResponse =
        base.copy(
            submissionStatus = outcomeFor(distanceKm),
            violations = violationsFor(distanceKm),
            issuedVoucher = voucherFor(distanceKm, token),
            transaction = transactionFor(distanceKm, token),
        )

    // ── Deterministic seed derivation ─────────────────────────────────────────

    /** Reimbursable amount with the daily-distance policy cap applied. */
    private fun reimbursableAmountFor(distanceKm: Double): Double = min(distanceKm, MAX_REIMBURSABLE_KM_PER_DAY) * RATE_PER_KM

    /**
     * Stable non-negative seed derived from the request's distance (in metres) and token.
     * Same inputs always produce the same seed on every platform.
     */
    private fun seedFor(
        distanceKm: Double,
        token: String?,
    ): Long {
        val metres = (distanceKm * 1000).roundToLong()
        val tokenHash = stableHash(token?.takeIf { it.isNotBlank() } ?: DEFAULT_TOKEN)
        return (tokenHash * 31 + metres) and 0x7FFF_FFFF_FFFF_FFFFL
    }

    /** Platform-independent string hash (FNV-style polynomial over char codes). */
    private fun stableHash(value: String): Long {
        var h = 1_125_899_906_842_597L
        for (ch in value) h = 31 * h + ch.code
        return h
    }
}
