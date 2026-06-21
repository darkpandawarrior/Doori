package com.miletracker.feature.payables.model

/** The five payables document families surfaced in the unified history (PB.4). */
enum class PayablesDocType(val label: String) {
    INVOICE("Invoice"),
    PURCHASE_REQUEST("PR"),
    GIN("GIN"),
    PARK_IN_OUT("Park"),
    ASN("ASN"),
}

/** A unified payables-document lifecycle status used for the history StatusChip + filter chips (PB.4). */
enum class PayablesDocStatus(val label: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    COMPLETED("Completed"),
}

/**
 * One row in the unified payables history (PB.4) — flat enough to render every document family on the same
 * [com.miletracker.core.ui.components.scaffold.HistoryListScaffold] card and to feed the PB.5
 * `PayablesSearchProvider`.
 */
data class PayablesDoc(
    val id: String,
    val type: PayablesDocType,
    val title: String,
    val reference: String,
    val amount: Double?,
    val status: PayablesDocStatus,
    val dateMillis: Long,
)
