package com.miletracker.feature.payables.repository

import com.miletracker.feature.payables.model.PayablesDoc
import com.miletracker.feature.payables.model.PayablesDocStatus
import com.miletracker.feature.payables.model.PayablesDocType
import kotlin.time.Clock

/**
 * Offline fake payables-history store (PB.4) — a deterministic spread of [PayablesDoc]s across all five
 * [PayablesDocType] families and all [PayablesDocStatus]es so the type-tabbed history + status filter chips
 * exercise every segment. Built relative to a [Clock]-supplied `now` (no `Math.random`), in the SP.1/SP.2
 * `VoucherHistoryRepository` style. Also the single source the PB.5 `PayablesSearchProvider` searches over.
 */
class PayablesHistoryRepository(private val clock: Clock = Clock.System) {
    private val dayMs = 86_400_000L

    private fun all(): List<PayablesDoc> {
        val now = clock.now().toEpochMilliseconds()
        // (type, title, reference, amount?, status, daysAgo) → one document each.
        val spec =
            listOf(
                Spec(PayablesDocType.INVOICE, "Sunrise Traders", "PO-4821", 124_500.0, PayablesDocStatus.PENDING, 1L),
                Spec(PayablesDocType.INVOICE, "Apex Logistics", "PO-4810", 58_900.0, PayablesDocStatus.COMPLETED, 9L),
                Spec(PayablesDocType.INVOICE, "BlueOak Supplies", "PO-4799", 211_300.0, PayablesDocStatus.REJECTED, 17L),
                Spec(PayablesDocType.PURCHASE_REQUEST, "Office laptops x10", "REQ-2210", 845_000.0, PayablesDocStatus.PENDING, 2L),
                Spec(PayablesDocType.PURCHASE_REQUEST, "Warehouse racking", "REQ-2204", 320_000.0, PayablesDocStatus.APPROVED, 11L),
                Spec(PayablesDocType.PURCHASE_REQUEST, "Pantry restock", "REQ-2198", 12_400.0, PayablesDocStatus.DRAFT, 4L),
                Spec(PayablesDocType.GIN, "Sunrise Traders", "PO-4821", null, PayablesDocStatus.COMPLETED, 1L),
                Spec(PayablesDocType.GIN, "Apex Logistics", "PO-4810", null, PayablesDocStatus.PENDING, 6L),
                Spec(PayablesDocType.PARK_IN_OUT, "MH12 AB 1234 · Park In", "Dock 3", null, PayablesDocStatus.COMPLETED, 0L),
                Spec(PayablesDocType.PARK_IN_OUT, "MH14 CD 9087 · Park Out", "Gate 1", null, PayablesDocStatus.PENDING, 3L),
                Spec(PayablesDocType.ASN, "BlueOak Supplies", "ASN-7741", 211_300.0, PayablesDocStatus.APPROVED, 5L),
                Spec(PayablesDocType.ASN, "Apex Logistics", "ASN-7720", 58_900.0, PayablesDocStatus.DRAFT, 13L),
            )
        return spec.mapIndexed { index, sp ->
            PayablesDoc(
                id = idFor(sp.type, index),
                type = sp.type,
                title = sp.title,
                reference = sp.reference,
                amount = sp.amount,
                status = sp.status,
                dateMillis = now - sp.daysAgo * dayMs,
            )
        }
    }

    private fun idFor(
        type: PayablesDocType,
        index: Int,
    ): String {
        val prefix =
            when (type) {
                PayablesDocType.INVOICE -> "INV"
                PayablesDocType.PURCHASE_REQUEST -> "REQ"
                PayablesDocType.GIN -> "GIN"
                PayablesDocType.PARK_IN_OUT -> "PRK"
                PayablesDocType.ASN -> "ASN"
            }
        return "$prefix-${9000 + index}"
    }

    /** All documents, optionally narrowed to [type] and/or [status], newest first. */
    fun documents(
        type: PayablesDocType? = null,
        status: PayablesDocStatus? = null,
    ): List<PayablesDoc> =
        all()
            .filter { (type == null || it.type == type) && (status == null || it.status == status) }
            .sortedByDescending { it.dateMillis }

    private data class Spec(
        val type: PayablesDocType,
        val title: String,
        val reference: String,
        val amount: Double?,
        val status: PayablesDocStatus,
        val daysAgo: Long,
    )
}
