package com.mileway.feature.logging.repository

import com.mileway.core.data.dao.VoucherDao
import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.db.VoucherEntity
import com.mileway.core.data.model.db.VoucherStatus
import com.mileway.feature.logging.ui.model.SubmittedVoucher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * P3.1: reads from the shared [VoucherDao] (`core/data`) instead of regenerating a hardcoded
 * 8-row spec on every call — the same store `feature/tracking`'s `VoucherRepository` writes to,
 * so a voucher submitted via Create Voucher is now observably visible here. On first run (empty
 * table) it seeds the original 8 demo rows so existing behavior is preserved (`AgentRepository`'s
 * `seedIfEmpty()` pattern from PLAN_V20 P1.2). [VoucherEntity] only carries the fields both
 * screens actually write (title/category/amount/notes/route ids/status) — the extra
 * display-only fields [SubmittedVoucher] has (`office`, `serviceTag`, `chips`,
 * `violationCount`) are deterministic display data derived here from the entity, exactly as the
 * old hardcoded spec derived them from an index, never persisted into unrelated columns.
 */
class VoucherHistoryRepository(private val dao: VoucherDao, private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L

    /** All vouchers (newest-first), or just those in [status] when non-null, as a live [Flow]. */
    fun observeVouchers(status: VoucherStatus? = null): Flow<List<SubmittedVoucher>> =
        dao.observeAll().map { rows ->
            rows.mapIndexed { index, row -> row.toSubmittedVoucher(index) }
                .filter { status == null || it.voucherState == status.label }
        }

    /** One-shot snapshot of all vouchers — used by search (SP.4), which needs a single suspend call, not a Flow. */
    suspend fun vouchers(): List<SubmittedVoucher> = observeVouchers().first()

    /** Seeds the original 8 demo vouchers if the shared table is empty (first run only). */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(demoSeed())
    }

    private fun demoSeed(): List<VoucherEntity> {
        val now = clock.now().toEpochMilliseconds()
        // (title, status, amount, daysAgo, violations) tuples → one voucher each. Real trip context
        // instead of a generic "Voucher 1000..1007" — the same list card and VoucherDetailsScreen
        // both render `title` verbatim, so a placeholder string there is a placeholder in two
        // screens at once. [VoucherEntity] has no office column (adding one is a Room schema
        // change, out of scope here) — `office` stays index-derived display data in
        // [toSubmittedVoucher], same as before, just now cycling PolicyMockData's four real
        // registered offices (1345/1347/1349/5356) instead of the unrelated "HQ_NORTH"/"HQ_WEST".
        val spec =
            listOf(
                Spec("Pune → Mumbai: Client Onsite", VoucherStatus.DRAFT, 22_629.0, 2L, 1),
                Spec("Baner → Hinjewadi: Vendor Review", VoucherStatus.DRAFT, 4_180.0, 4L, 0),
                Spec("Mumbai → Pune: Weekly Sync", VoucherStatus.PENDING, 14_850.0, 6L, 0),
                Spec("Bhosari MIDC Site Visit", VoucherStatus.PENDING, 9_240.0, 9L, 2),
                Spec("Andheri → BKC: Partner Meeting", VoucherStatus.APPROVED, 6_310.0, 12L, 0),
                Spec("Pune → Bengaluru: Annual Summit", VoucherStatus.APPROVED, 31_500.0, 16L, 0),
                Spec("Airport Pickup: Auditor Visit", VoucherStatus.REJECTED, 2_990.0, 20L, 3),
                Spec("Mumbai Distribution Hub Inspection", VoucherStatus.SETTLED, 18_400.0, 28L, 0),
            )
        return spec.mapIndexed { index, s ->
            VoucherEntity(
                voucherNumber = "VCH-${1000 + index}",
                title = s.title,
                category = VoucherCategory.MILEAGE,
                totalAmount = s.amount,
                notes = if (s.violations > 0) "$VIOLATIONS_PREFIX${s.violations}" else "",
                expenseRouteIdsJson = VoucherEntity.encodeExpenseRouteIds(listOf("EXP-${48700 + index}")),
                status = s.status.label,
                createdAtMs = now - s.daysAgo * dayMs,
            )
        }
    }

    private fun VoucherEntity.toSubmittedVoucher(index: Int): SubmittedVoucher {
        val expenseIds = VoucherEntity.decodeExpenseRouteIds(expenseRouteIdsJson)
        val violationCount = notes.removePrefix(VIOLATIONS_PREFIX).toIntOrNull() ?: 0
        return SubmittedVoucher(
            id = voucherNumber,
            voucherState = status,
            payment = "Self Paid",
            chips = if (violationCount > 0) listOf("Attachments", "Violations") else listOf("Attachments"),
            office = REAL_OFFICE_CODES[index % REAL_OFFICE_CODES.size],
            amount = totalAmount,
            serviceTag = if (index % 3 == 0) "Log Conveyance" else "log_trip",
            expenseDateMillis = createdAtMs - 2 * dayMs,
            // P3.1 (gap 17.7): a voucher can hold multiple linked trips/expenses — surface all of
            // them rather than truncating to one, aligning history's shape with creation's.
            expenseId = expenseIds.joinToString(",").ifEmpty { voucherNumber },
            submittedOnMillis = createdAtMs,
            violationCount = violationCount,
        )
    }

    private data class Spec(
        val title: String,
        val status: VoucherStatus,
        val amount: Double,
        val daysAgo: Long,
        val violations: Int,
    )

    private companion object {
        const val VIOLATIONS_PREFIX = "violations:"

        /** [com.mileway.stub.PolicyMockData.offices]' four real registered office codes. */
        val REAL_OFFICE_CODES = listOf("1345", "1347", "1349", "5356")
    }
}
