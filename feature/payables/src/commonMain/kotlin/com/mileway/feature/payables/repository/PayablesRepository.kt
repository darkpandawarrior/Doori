package com.mileway.feature.payables.repository

import com.mileway.feature.payables.model.Invoice
import com.mileway.feature.payables.model.InvoiceStatus
import com.mileway.feature.payables.model.PoLineItem
import com.mileway.feature.payables.model.PoStatus
import com.mileway.feature.payables.model.PurchaseOrder

class PayablesRepository {
    private val baseMs = 1_700_000_000_000L
    private val dayMs = 86_400_000L

    val purchaseOrders =
        listOf(
            PurchaseOrder(
                id = "PO-2024-001",
                vendorName = "OfficeMax Supplies Ltd.",
                deliveryDate = "2024-01-20",
                officeLocation = "Head Office – Pune",
                status = PoStatus.APPROVED,
                lineItems =
                    listOf(
                        PoLineItem("A4 Copy Paper (500 sheets × 5 reams)", 10, 450.0, 12),
                        PoLineItem("Ballpoint Pens Box (50 pcs)", 5, 220.0, 18),
                        PoLineItem("Stapler + Staple Pins Set", 8, 180.0, 18),
                    ),
                dateMs = baseMs - 20 * dayMs,
            ),
            PurchaseOrder(
                id = "PO-2024-002",
                vendorName = "TechVision Systems",
                deliveryDate = "2024-01-28",
                officeLocation = "North Branch – Mumbai",
                status = PoStatus.PENDING_APPROVAL,
                lineItems =
                    listOf(
                        PoLineItem("Laptop Stand – Adjustable", 4, 1800.0, 18),
                        PoLineItem("USB-C Hub 7-in-1", 4, 2200.0, 18),
                    ),
                dateMs = baseMs - 5 * dayMs,
            ),
            PurchaseOrder(
                id = "PO-2024-003",
                vendorName = "Greenline Pantry Co.",
                deliveryDate = "2024-02-05",
                officeLocation = "Head Office – Pune",
                status = PoStatus.DRAFT,
                lineItems =
                    listOf(
                        PoLineItem("Coffee Sachets – Premium (20 pcs)", 6, 350.0, 5),
                        PoLineItem("Green Tea Bags (100 pcs)", 4, 280.0, 5),
                        PoLineItem("Biscuit Assortment Pack", 10, 120.0, 5),
                    ),
                dateMs = baseMs - 1 * dayMs,
            ),
        )

    val invoices =
        listOf(
            Invoice(
                id = "INV-2024-0091",
                poId = "PO-2024-001",
                vendorName = "OfficeMax Supplies Ltd.",
                amountRupees = 62504.0,
                status = InvoiceStatus.MATCHED,
                dateMs = baseMs - 18 * dayMs,
            ),
            Invoice(
                id = "INV-2024-0102",
                poId = "PO-2024-002",
                vendorName = "TechVision Systems",
                amountRupees = 18880.0,
                status = InvoiceStatus.UNMATCHED,
                dateMs = baseMs - 3 * dayMs,
            ),
            Invoice(
                id = "INV-2023-0889",
                poId = "PO-2023-045",
                vendorName = "Rapid Print Works",
                amountRupees = 8430.0,
                status = InvoiceStatus.PAID,
                dateMs = baseMs - 45 * dayMs,
            ),
        )

    fun getPoById(id: String): PurchaseOrder? = purchaseOrders.find { it.id == id }
}
