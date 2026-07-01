package com.mileway.feature.tracking.repository

data class VoucherRecord(
    val voucherNumber: String,
    val title: String,
    val category: String,
    val totalAmount: Double,
    val notes: String,
    val expenseRouteIds: List<String>,
    val createdAtMs: Long,
)

class VoucherRepository {
    private val _vouchers = mutableListOf<VoucherRecord>()
    val vouchers: List<VoucherRecord> get() = _vouchers.toList()

    fun save(record: VoucherRecord) {
        _vouchers.add(record)
    }
}
