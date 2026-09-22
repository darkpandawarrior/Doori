package com.mileway.core.data.model.db

/**
 * P3.4: the voucher expense category as a typed, exhaustive enum — replacing the free-text
 * `CreateVoucherUiState.category: String` that was bound to a hardcoded
 * `CATEGORIES = listOf("Travel", "Fuel", "Maintenance", "Other")` list in `CreateVoucherScreen.kt`.
 * Lives in `core:data` (alongside [VoucherStatus]/[VoucherEntity]) for the same reason
 * [VoucherStatus] does: both `feature:tracking` (creates vouchers) and `feature:logging` (renders
 * history) need it, and feature modules never depend on each other.
 *
 * This is Mileway's own closed set, scoped to mileage trips only — it intentionally does NOT
 * model the reference app's `VoucherContext` sealed class (Advance/Card/QrPayment/Event contexts),
 * since Mileway's voucher feature isn't a general expense wallet.
 *
 * [label] preserves the exact strings the 4 original hardcoded categories used, so existing
 * behavior/labels are unchanged.
 */
enum class VoucherCategory(
    val label: String,
) {
    MILEAGE("Travel"),
    FUEL("Fuel"),
    MAINTENANCE("Maintenance"),
    OTHER("Other"),
}
