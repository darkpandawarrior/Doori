package com.mileway.core.data.model.db

/**
 * The voucher lifecycle states (P3.1/P3.2). Lives in `core:data` (alongside [VoucherEntity])
 * rather than either feature module, since both `feature:tracking` (creates vouchers, starting at
 * [DRAFT]) and `feature:logging` (renders history tabs per status) read/write it, and feature
 * modules never depend on each other — only on shared `core:*` modules.
 */
enum class VoucherStatus(
    val label: String,
) {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    SETTLED("Settled"),
}
