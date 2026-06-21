@file:Suppress("ktlint:standard:function-naming")

package com.miletracker.feature.payables.ui.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miletracker.core.common.formatDecimal
import com.miletracker.core.ui.previews.PreviewLightDark
import com.miletracker.core.ui.previews.PreviewMatrix
import com.miletracker.core.ui.previews.PreviewSurface
import com.miletracker.feature.payables.model.PoLineItem
import com.miletracker.feature.payables.model.PoStatus
import com.miletracker.feature.payables.model.PurchaseOrder

// ---------------------------------------------------------------------------
// Phase 9.1, Payables feature preview matrix.
//
// PayablesHomeScreen / PurchaseRequestDetailsScreen both default to koinViewModel().
// These previews use model types directly to render representative UI without DI.
// ---------------------------------------------------------------------------

// ── Sample data ──────────────────────────────────────────────────────────────

private val sampleLineItems =
    listOf(
        PoLineItem(description = "Laptop Stand", qty = 2, unitPrice = 1500.0, gstPercent = 18),
        PoLineItem(description = "USB-C Hub", qty = 3, unitPrice = 850.0, gstPercent = 18),
        PoLineItem(description = "Ergonomic Mouse", qty = 5, unitPrice = 650.0, gstPercent = 12),
    )

private val samplePoApproved =
    PurchaseOrder(
        id = "PO-2026-0042",
        vendorName = "Acme Office Supplies",
        deliveryDate = "2026-07-10",
        officeLocation = "Pune HQ",
        status = PoStatus.APPROVED,
        lineItems = sampleLineItems,
        dateMs = 1_750_348_800_000L,
    )

private val samplePoPending =
    PurchaseOrder(
        id = "PO-2026-0055",
        vendorName = "TechWorld Distributors",
        deliveryDate = "2026-07-20",
        officeLocation = "Mumbai Office",
        status = PoStatus.PENDING_APPROVAL,
        lineItems = sampleLineItems.take(2),
        dateMs = 1_750_348_800_000L,
    )

// ── PO summary card ──────────────────────────────────────────────────────────

@Composable
private fun PoSummaryCard(po: PurchaseOrder) {
    val (statusColor, statusLabel) =
        when (po.status) {
            PoStatus.APPROVED -> Color(0xFF22C55E) to "Approved"
            PoStatus.PENDING_APPROVAL -> Color(0xFFF59E0B) to "Pending Approval"
            PoStatus.REJECTED -> Color(0xFFEF4444) to "Rejected"
            PoStatus.DRAFT -> Color(0xFF94A3B8) to "Draft"
        }
    val statusIcon =
        when (po.status) {
            PoStatus.APPROVED -> Icons.Filled.CheckCircle
            PoStatus.PENDING_APPROVAL -> Icons.Filled.HourglassBottom
            else -> Icons.Filled.Receipt
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = po.id,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Text(text = statusLabel, style = MaterialTheme.typography.labelMedium, color = statusColor)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = po.vendorName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Deliver by ${po.deliveryDate}  •  ${po.officeLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Total: ₹${po.totalAmount.formatDecimal(2)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${po.lineItems.size} line item(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Approved PO card ─────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewPoCardApproved() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            PoSummaryCard(po = samplePoApproved)
        }
    }
}

// ── Pending PO card ──────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
fun PreviewPoCardPendingApproval() {
    PreviewSurface {
        Column(modifier = Modifier.padding(16.dp)) {
            PoSummaryCard(po = samplePoPending)
        }
    }
}

// ── Full matrix, PO list ────────────────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewPoListMatrix() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PoSummaryCard(po = samplePoApproved)
            PoSummaryCard(po = samplePoPending)
        }
    }
}

// ── Line items breakdown card ────────────────────────────────────────────────

@PreviewMatrix
@Composable
fun PreviewPoLineItemsMatrix() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Line Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            sampleLineItems.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = "Qty ${item.qty}  •  GST ${item.gstPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "₹${item.lineTotal.formatDecimal(2)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
