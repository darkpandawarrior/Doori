package com.mileway.feature.tracking.repository

import com.mileway.core.data.dao.VoucherDao
import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.db.VoucherEntity
import com.mileway.core.data.model.db.VoucherStatus

data class VoucherRecord(
    val voucherNumber: String,
    val title: String,
    val category: VoucherCategory,
    val totalAmount: Double,
    val notes: String,
    val expenseRouteIds: List<String>,
    val createdAtMs: Long,
    val status: String = VoucherStatus.DRAFT.label,
)

/**
 * P3.1/P3.2: delegates to the shared [VoucherDao] (`core/data`) instead of a private in-memory
 * list, so a voucher created here is immediately visible to `feature/logging`'s
 * `VoucherHistoryRepository` — the two used to be entirely disconnected stores. P3.2 adds the
 * only writer of the `status` column past creation: legal transitions gated by
 * [VoucherTransitions], since `VoucherStatus` is a real enum but nothing ever changed it before.
 */
class VoucherRepository(
    private val dao: VoucherDao,
) {
    suspend fun save(record: VoucherRecord) {
        dao.insert(
            VoucherEntity(
                voucherNumber = record.voucherNumber,
                title = record.title,
                category = record.category,
                totalAmount = record.totalAmount,
                notes = record.notes,
                expenseRouteIdsJson = VoucherEntity.encodeExpenseRouteIds(record.expenseRouteIds),
                status = record.status,
                createdAtMs = record.createdAtMs,
            ),
        )
    }

    suspend fun getAll(): List<VoucherRecord> = dao.getAll().map { it.toRecord() }

    /**
     * Moves a just-created voucher out of [VoucherStatus.DRAFT] into [VoucherStatus.PENDING] —
     * a voucher isn't useful sitting in DRAFT forever, so `CreateVoucherViewModel.submit()` calls
     * this as part of the same submit flow. A no-op if the voucher is missing or not DRAFT
     * (defensive; submit() always inserts fresh at DRAFT so this should never fire in practice).
     */
    suspend fun moveToApproval(voucherNumber: String) {
        transition(voucherNumber, VoucherStatus.PENDING)
    }

    /**
     * Demo-realism "advance" action (P3.2): deterministically rotates a [VoucherStatus.PENDING]
     * voucher toward [VoucherStatus.APPROVED]/[VoucherStatus.REJECTED]/[VoucherStatus.SETTLED],
     * mirroring how `PolicyMockData` deterministically rotates mileage-submission outcomes,
     * without building a real approver UI/workflow. No-op for any voucher not currently PENDING.
     */
    suspend fun advance(voucherNumber: String) {
        val current = dao.getByNumber(voucherNumber) ?: return
        val from = VoucherStatus.entries.firstOrNull { it.label == current.status } ?: return
        if (from != VoucherStatus.PENDING) return
        val candidates = VoucherTransitions.allowed(from).sortedBy { it.name }
        val next = candidates[voucherNumber.hashCode().mod(candidates.size)]
        transition(voucherNumber, next)
    }

    /** Applies [to] only if [VoucherTransitions] allows it from the voucher's current status. */
    private suspend fun transition(
        voucherNumber: String,
        to: VoucherStatus,
    ) {
        val current = dao.getByNumber(voucherNumber) ?: return
        val from = VoucherStatus.entries.firstOrNull { it.label == current.status } ?: return
        if (!VoucherTransitions.isAllowed(from, to)) return
        dao.updateStatus(voucherNumber, to.label)
    }

    /**
     * P3.6: withdraws a submitted voucher — a deliberate simplification of the reference app's
     * two-factor `draft OR pending-approval` + config-flag gate, since Mileway has no separate
     * "pending approval permission" concept to gate on. Only [VoucherStatus.DRAFT] vouchers may
     * be withdrawn here; anything past DRAFT (already moved to PENDING/decided/settled) is a
     * no-op — the caller is expected to disable the action for those rows.
     */
    suspend fun withdraw(voucherNumber: String) {
        val current = dao.getByNumber(voucherNumber) ?: return
        val from = VoucherStatus.entries.firstOrNull { it.label == current.status } ?: return
        if (from != VoucherStatus.DRAFT) return
        dao.deleteByNumber(voucherNumber)
    }
}

private fun VoucherEntity.toRecord() =
    VoucherRecord(
        voucherNumber = voucherNumber,
        title = title,
        category = category,
        totalAmount = totalAmount,
        notes = notes,
        expenseRouteIds = VoucherEntity.decodeExpenseRouteIds(expenseRouteIdsJson),
        createdAtMs = createdAtMs,
        status = status,
    )
