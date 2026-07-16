package com.mileway.feature.advances.viewmodel

import com.mileway.feature.advances.model.AdvanceTransaction

/** Transactions-tab filter chips shared by [PettyCardDetailViewModel] and [QrCardDetailViewModel]. */
enum class VoucherFilter { ALL, NO_VOUCHER, HAS_VOUCHER }

/** Applies the voucher-status chip, then a free-text title search, to a transaction list. */
fun List<AdvanceTransaction>.filterTransactions(
    voucherFilter: VoucherFilter,
    query: String,
): List<AdvanceTransaction> {
    val byVoucher =
        when (voucherFilter) {
            VoucherFilter.ALL -> this
            VoucherFilter.NO_VOUCHER -> filter { !it.voucherCreated }
            VoucherFilter.HAS_VOUCHER -> filter { it.voucherCreated }
        }
    val q = query.trim()
    return if (q.isEmpty()) byVoucher else byVoucher.filter { it.title.contains(q, ignoreCase = true) }
}
