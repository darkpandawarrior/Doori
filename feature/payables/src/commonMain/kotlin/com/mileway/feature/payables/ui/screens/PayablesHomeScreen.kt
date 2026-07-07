@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

package com.mileway.feature.payables.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.mvi.DefaultEmptyState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.payables_empty_subtitle
import com.mileway.core.ui.resources.payables_empty_title
import com.mileway.core.ui.resources.payables_invoice_status_matched
import com.mileway.core.ui.resources.payables_invoice_status_paid
import com.mileway.core.ui.resources.payables_invoice_status_unmatched
import com.mileway.core.ui.resources.payables_metric_open_pos
import com.mileway.core.ui.resources.payables_metric_pending_invoices
import com.mileway.core.ui.resources.payables_metric_unmatched_gins
import com.mileway.core.ui.resources.payables_new_request
import com.mileway.core.ui.resources.payables_po_status_approved
import com.mileway.core.ui.resources.payables_po_status_draft
import com.mileway.core.ui.resources.payables_po_status_pending_approval
import com.mileway.core.ui.resources.payables_po_status_rejected
import com.mileway.core.ui.resources.payables_section_purchase_requests
import com.mileway.core.ui.resources.payables_section_recent_invoices
import com.mileway.core.ui.resources.payables_subtitle
import com.mileway.core.ui.resources.payables_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.feature.payables.model.Invoice
import com.mileway.feature.payables.model.InvoiceStatus
import com.mileway.feature.payables.model.PoStatus
import com.mileway.feature.payables.model.PurchaseOrder
import com.mileway.feature.payables.viewmodel.PayablesAction
import com.mileway.feature.payables.viewmodel.PayablesHomeData
import com.mileway.feature.payables.viewmodel.PayablesViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PayablesHomeScreen(
    onNewRequest: () -> Unit,
    onOpenPo: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayablesViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val state = ui.homeState.dataOrNull ?: PayablesHomeData()

    val openPoCount = state.purchaseOrders.count { it.status == PoStatus.APPROVED || it.status == PoStatus.PENDING_APPROVAL }
    val pendingInvoiceCount = state.invoices.count { it.status == InvoiceStatus.UNMATCHED || it.status == InvoiceStatus.MATCHED }
    val unmatchedCount = state.invoices.count { it.status == InvoiceStatus.UNMATCHED }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewRequest,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.payables_new_request)) },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Gradient header
            Box(
                modifier =
                    Modifier
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF00695C), Color(0xFF26A69A))),
                        )
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.payables_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(Res.string.payables_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.l))
                    // Summary row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    ) {
                        SummaryMetric(stringResource(Res.string.payables_metric_open_pos), "$openPoCount", modifier = Modifier.weight(1f))
                        SummaryMetric(stringResource(Res.string.payables_metric_pending_invoices), "$pendingInvoiceCount", modifier = Modifier.weight(1f))
                        SummaryMetric(stringResource(Res.string.payables_metric_unmatched_gins), "$unmatchedCount", modifier = Modifier.weight(1f))
                    }
                }
            }

            ScreenStateContent(
                state = ui.homeState,
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                onRetry = { viewModel.onAction(PayablesAction.Refresh) },
                empty = {
                    DefaultEmptyState(
                        title = stringResource(Res.string.payables_empty_title),
                        subtitle = stringResource(Res.string.payables_empty_subtitle),
                        ctaLabel = stringResource(Res.string.payables_new_request),
                        onCta = onNewRequest,
                    )
                },
            ) { data ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Spacer(Modifier.height(DesignTokens.Spacing.l))
                    Text(
                        text = stringResource(Res.string.payables_section_purchase_requests),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    data.purchaseOrders.forEach { po ->
                        PoCard(po = po, onClick = { onOpenPo(po.id) })
                    }

                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    Text(
                        text = stringResource(Res.string.payables_section_recent_invoices),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    data.invoices.forEach { inv ->
                        InvoiceCard(invoice = inv)
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PoCard(
    po: PurchaseOrder,
    onClick: () -> Unit,
) {
    val (statusLabel, statusColor) =
        when (po.status) {
            PoStatus.DRAFT -> stringResource(Res.string.payables_po_status_draft) to StatusColors.neutral
            PoStatus.PENDING_APPROVAL -> stringResource(Res.string.payables_po_status_pending_approval) to StatusColors.warning
            PoStatus.APPROVED -> stringResource(Res.string.payables_po_status_approved) to StatusColors.success
            PoStatus.REJECTED -> stringResource(Res.string.payables_po_status_rejected) to StatusColors.error
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(DesignTokens.Shape.roundedMd)
                .clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(po.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    po.vendorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "₹${po.totalAmount.toLong()} · ${po.lineItems.size} item${if (po.lineItems.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice) {
    val (icon, statusLabel, statusColor) =
        when (invoice.status) {
            InvoiceStatus.UNMATCHED -> Triple(Icons.Filled.Warning, stringResource(Res.string.payables_invoice_status_unmatched), StatusColors.warning)
            InvoiceStatus.MATCHED -> Triple(Icons.Filled.Receipt, stringResource(Res.string.payables_invoice_status_matched), StatusColors.info)
            InvoiceStatus.PAID -> Triple(Icons.Filled.CheckCircle, stringResource(Res.string.payables_invoice_status_paid), StatusColors.success)
        }
    val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(invoice.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    invoice.vendorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    Instant.fromEpochMilliseconds(invoice.dateMs).toLocalDateTime(TimeZone.currentSystemDefault()).let {
                            ldt ->
                        "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${invoice.amountRupees.toLong()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}
