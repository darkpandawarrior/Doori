package com.miletracker.feature.logging.repository

import com.miletracker.feature.logging.ui.model.SubmittedVoucher
import kotlin.time.Clock

/** The voucher lifecycle states used by [VoucherHistoryRepository] tabs (SP.1). */
enum class VoucherStatus(val label: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    SETTLED("Settled"),
}

/**
 * Offline fake voucher store (SP.1) — a deterministic spread of [SubmittedVoucher]s across all five
 * [VoucherStatus]es so the tabbed history exercises every segment. Built relative to a [Clock]-supplied
 * `now` (no `Math.random`), in the `CardsMockData` style.
 */
class VoucherHistoryRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L

    private fun all(): List<SubmittedVoucher> {
        val now = clock.now().toEpochMilliseconds()
        // (status, amount, daysAgo, violations) tuples → one voucher each.
        val spec =
            listOf(
                Quad(VoucherStatus.DRAFT, 22_629.0, 2L, 1),
                Quad(VoucherStatus.DRAFT, 4_180.0, 4L, 0),
                Quad(VoucherStatus.PENDING, 14_850.0, 6L, 0),
                Quad(VoucherStatus.PENDING, 9_240.0, 9L, 2),
                Quad(VoucherStatus.APPROVED, 6_310.0, 12L, 0),
                Quad(VoucherStatus.APPROVED, 31_500.0, 16L, 0),
                Quad(VoucherStatus.REJECTED, 2_990.0, 20L, 3),
                Quad(VoucherStatus.SETTLED, 18_400.0, 28L, 0),
            )
        return spec.mapIndexed { index, q ->
            SubmittedVoucher(
                id = "VCH-${1000 + index}",
                voucherState = q.status.label,
                payment = "Self Paid",
                chips = if (q.violations > 0) listOf("Attachments", "Violations") else listOf("Attachments"),
                office = if (index % 2 == 0) "HQ_NORTH" else "HQ_WEST",
                amount = q.amount,
                serviceTag = if (index % 3 == 0) "Log Conveyance" else "log_trip",
                expenseDateMillis = now - (q.daysAgo + 2) * dayMs,
                expenseId = "EXP-${48700 + index}",
                submittedOnMillis = now - q.daysAgo * dayMs,
                violationCount = q.violations,
            )
        }
    }

    /** All vouchers, or just those in [status] when non-null. */
    fun vouchers(status: VoucherStatus? = null): List<SubmittedVoucher> =
        all().filter { status == null || it.voucherState == status.label }.sortedByDescending { it.submittedOnMillis }

    private data class Quad(
        val status: VoucherStatus,
        val amount: Double,
        val daysAgo: Long,
        val violations: Int,
    )
}
