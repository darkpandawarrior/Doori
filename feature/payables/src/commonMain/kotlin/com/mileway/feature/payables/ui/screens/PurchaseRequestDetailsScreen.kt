@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.payables.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.common.asString
import com.mileway.core.common.formatDecimal
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.payables_back
import com.mileway.core.ui.resources.payables_col_description
import com.mileway.core.ui.resources.payables_col_qty
import com.mileway.core.ui.resources.payables_col_total
import com.mileway.core.ui.resources.payables_detail_delivery_date
import com.mileway.core.ui.resources.payables_detail_office_location
import com.mileway.core.ui.resources.payables_detail_po_number
import com.mileway.core.ui.resources.payables_detail_total_amount
import com.mileway.core.ui.resources.payables_download_pdf
import com.mileway.core.ui.resources.payables_line_items
import com.mileway.core.ui.resources.payables_po_not_found
import com.mileway.core.ui.resources.payables_po_status_approved
import com.mileway.core.ui.resources.payables_po_status_draft
import com.mileway.core.ui.resources.payables_po_status_pending_approval
import com.mileway.core.ui.resources.payables_po_status_rejected
import com.mileway.core.ui.resources.payables_timeline_approved_note
import com.mileway.core.ui.resources.payables_timeline_pending
import com.mileway.core.ui.resources.payables_timeline_rejected_note
import com.mileway.core.ui.resources.payables_timeline_submitted
import com.mileway.core.ui.resources.payables_timeline_submitted_note
import com.mileway.core.ui.resources.payables_timeline_title
import com.mileway.core.ui.resources.payables_timeline_under_review
import com.mileway.core.ui.resources.payables_timeline_under_review_note
import com.mileway.core.ui.resources.payables_total_incl_gst
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.feature.payables.model.PoLineItem
import com.mileway.feature.payables.model.PoStatus
import com.mileway.feature.payables.model.PurchaseOrder
import com.mileway.feature.payables.viewmodel.PayablesAction
import com.mileway.feature.payables.viewmodel.PayablesEffect
import com.mileway.feature.payables.viewmodel.PayablesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseRequestDetailsScreen(
    poId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayablesViewModel = koinViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(poId) { viewModel.onAction(PayablesAction.OpenDetail(poId)) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PayablesEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message.asString())
                else -> Unit
            }
        }
    }

    val po = ui.detailState.dataOrNull

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = poId,
                subtitle = po?.vendorName,
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payables_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (po == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(Res.string.payables_po_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DesignTokens.Spacing.l)
                        .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))

                // Header card
                PoHeaderCard(po = po)

                // Line items
                PoLineItemsCard(lineItems = po.lineItems, total = po.totalAmount)

                // Approval timeline
                PoTimelineCard(status = po.status)

                OutlinedButton(
                    onClick = { viewModel.onAction(PayablesAction.ShowMessage("Downloading ${po.id}.pdf…")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Text(stringResource(Res.string.payables_download_pdf))
                }

                Spacer(Modifier.height(DesignTokens.Spacing.l))
            }
        }
    }
}

@Composable
private fun PoHeaderCard(po: PurchaseOrder) {
    val (statusLabel, statusColor) =
        when (po.status) {
            PoStatus.DRAFT -> stringResource(Res.string.payables_po_status_draft) to StatusColors.neutral
            PoStatus.PENDING_APPROVAL -> stringResource(Res.string.payables_po_status_pending_approval) to StatusColors.warning
            PoStatus.APPROVED -> stringResource(Res.string.payables_po_status_approved) to StatusColors.success
            PoStatus.REJECTED -> stringResource(Res.string.payables_po_status_rejected) to StatusColors.error
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = po.vendorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            DetailRow(stringResource(Res.string.payables_detail_po_number), po.id)
            DetailRow(stringResource(Res.string.payables_detail_delivery_date), po.deliveryDate)
            DetailRow(stringResource(Res.string.payables_detail_office_location), po.officeLocation)
            DetailRow(stringResource(Res.string.payables_detail_total_amount), "₹${po.totalAmount.formatDecimal(2)}")
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PoLineItemsCard(
    lineItems: List<PoLineItem>,
    total: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(
                stringResource(Res.string.payables_line_items),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(Res.string.payables_col_description),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(Res.string.payables_col_qty),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(DesignTokens.Spacing.l))
                Text(
                    stringResource(Res.string.payables_col_total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            lineItems.forEach { item ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.description, style = MaterialTheme.typography.bodySmall)
                        Text("GST ${item.gstPercent}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${item.qty}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(DesignTokens.Spacing.l))
                    Text("₹${item.lineTotal.toLong()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.payables_total_incl_gst), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("₹${total.formatDecimal(2)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PoTimelineCard(status: PoStatus) {
    data class TStep(val label: String, val icon: ImageVector, val color: Color, val active: Boolean, val note: String = "")

    val steps =
        listOf(
            TStep(
                stringResource(Res.string.payables_timeline_submitted),
                Icons.Filled.Receipt,
                StatusColors.info,
                active = true,
                note = stringResource(Res.string.payables_timeline_submitted_note),
            ),
            TStep(
                stringResource(Res.string.payables_timeline_under_review),
                Icons.Filled.HourglassBottom,
                StatusColors.warning,
                active = status != PoStatus.DRAFT,
                note = if (status != PoStatus.DRAFT) stringResource(Res.string.payables_timeline_under_review_note) else "",
            ),
            when (status) {
                PoStatus.APPROVED ->
                    TStep(
                        stringResource(Res.string.payables_po_status_approved),
                        Icons.Filled.CheckCircle,
                        StatusColors.success,
                        active = true,
                        note = stringResource(Res.string.payables_timeline_approved_note),
                    )
                PoStatus.REJECTED ->
                    TStep(
                        stringResource(Res.string.payables_po_status_rejected),
                        Icons.Filled.CheckCircle,
                        StatusColors.error,
                        active = true,
                        note = stringResource(Res.string.payables_timeline_rejected_note),
                    )
                else -> TStep(stringResource(Res.string.payables_timeline_pending), Icons.Filled.HourglassBottom, StatusColors.neutral, active = false)
            },
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                stringResource(Res.string.payables_timeline_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            steps.forEach { step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .background(
                                    if (step.active) {
                                        step.color.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = if (step.active) step.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Column {
                        Text(
                            text = step.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (step.active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (step.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        if (step.note.isNotBlank()) {
                            Text(step.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
