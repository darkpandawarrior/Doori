package com.mileway.feature.payables.model

enum class PoStatus { DRAFT, PENDING_APPROVAL, APPROVED, REJECTED }

enum class InvoiceStatus { UNMATCHED, MATCHED, PAID }

data class PoLineItem(
    val description: String,
    val qty: Int,
    val unitPrice: Double,
    val gstPercent: Int,
) {
    val lineTotal: Double get() = qty * unitPrice * (1 + gstPercent / 100.0)
}

data class PurchaseOrder(
    val id: String,
    val vendorName: String,
    val deliveryDate: String,
    val officeLocation: String,
    val status: PoStatus,
    val lineItems: List<PoLineItem>,
    val dateMs: Long,
) {
    val totalAmount: Double get() = lineItems.sumOf { it.lineTotal }
}

data class Invoice(
    val id: String,
    val poId: String,
    val vendorName: String,
    val amountRupees: Double,
    val status: InvoiceStatus,
    val dateMs: Long,
)

data class NewLineItemDraft(
    val description: String = "",
    val qty: Int = 1,
    val unitPrice: String = "",
    val gstPercent: Int = 18,
)
