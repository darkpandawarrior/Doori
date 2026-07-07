package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.components.EmptyState
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_back_cd
import com.mileway.core.ui.resources.logging_delete_draft_cd
import com.mileway.core.ui.resources.logging_drafts_count
import com.mileway.core.ui.resources.logging_expense_date
import com.mileway.core.ui.resources.logging_expense_id
import com.mileway.core.ui.resources.logging_history_subtitle
import com.mileway.core.ui.resources.logging_history_title
import com.mileway.core.ui.resources.logging_no_drafts_subtitle
import com.mileway.core.ui.resources.logging_no_drafts_title
import com.mileway.core.ui.resources.logging_no_submissions_subtitle
import com.mileway.core.ui.resources.logging_no_submissions_title
import com.mileway.core.ui.resources.logging_none
import com.mileway.core.ui.resources.logging_office
import com.mileway.core.ui.resources.logging_plural_violations
import com.mileway.core.ui.resources.logging_policy_violation
import com.mileway.core.ui.resources.logging_submitted_count
import com.mileway.core.ui.resources.logging_submitted_on
import com.mileway.core.ui.resources.logging_updated
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.ui.model.SubmittedVoucher
import com.mileway.feature.logging.viewmodel.LogMilesAction
import com.mileway.feature.logging.viewmodel.LogMilesDraftUi
import com.mileway.feature.logging.viewmodel.LogMilesViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The two history tabs. */
private enum class HistoryTab { DRAFTS, SUBMITTED }

/**
 * Log Miles History screen.
 *
 * A gradient header ("Log Miles History / Drafts and submitted log miles"), a
 * Drafts(N)/Submitted(N) segmented control, and the selected tab's content:
 * - Drafts: VM-held drafts with open/delete.
 * - Submitted: date-grouped voucher-style cards (#id, "Voucher Not Created", Self
 *   Paid, chips, ₹ + service tag, expense date/id).
 *
 * Full-screen flow, so the header owns its status-bar inset and content scrolls.
 *
 * @param viewModel   shared flow ViewModel
 * @param onBack      pop back
 * @param onOpenDraft open a draft (returns the draft id)
 */
@Composable
fun LogMilesHistoryScreen(
    viewModel: LogMilesViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onOpenDraft: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(HistoryTab.DRAFTS) }

    Column(modifier = Modifier.fillMaxSize()) {
        HistoryHeader(onBack = onBack)

        SegmentedControl(
            selected = tab,
            draftCount = uiState.drafts.size,
            submittedCount = uiState.submitted.size,
            onSelect = { tab = it },
            modifier = Modifier.padding(DesignTokens.Spacing.l),
        )

        when (tab) {
            HistoryTab.DRAFTS ->
                DraftsTab(
                    drafts = uiState.drafts,
                    onOpen = onOpenDraft,
                    onDelete = { viewModel.onAction(LogMilesAction.DeleteDraft(it)) },
                )

            HistoryTab.SUBMITTED -> SubmittedTab(vouchers = uiState.submitted)
        }
    }
}

@Composable
private fun HistoryHeader(onBack: () -> Unit) {
    // Gradient banner that owns the status-bar inset exactly once.
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(DesignTokens.topBarGradientBrush())
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.Spacing.s,
                        vertical = DesignTokens.Spacing.m,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.logging_back_cd),
                    tint = Color.White,
                )
            }
            Icon(
                Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(DesignTokens.IconSize.header),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.m))
            Column {
                Text(
                    stringResource(Res.string.logging_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    stringResource(Res.string.logging_history_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    selected: HistoryTab,
    draftCount: Int,
    submittedCount: Int,
    onSelect: (HistoryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.button,
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SegmentButton(
                label = stringResource(Res.string.logging_drafts_count, draftCount),
                selected = selected == HistoryTab.DRAFTS,
                showCheck = selected == HistoryTab.DRAFTS,
                onClick = { onSelect(HistoryTab.DRAFTS) },
                modifier = Modifier.weight(1f),
            )
            SegmentButton(
                label = stringResource(Res.string.logging_submitted_count, submittedCount),
                selected = selected == HistoryTab.SUBMITTED,
                showCheck = selected == HistoryTab.SUBMITTED,
                onClick = { onSelect(HistoryTab.SUBMITTED) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    showCheck: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = container, shape = DesignTokens.Shape.button, modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(vertical = DesignTokens.Spacing.m),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCheck) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
            }
            Text(label, style = MaterialTheme.typography.titleSmall, color = content, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DraftsTab(
    drafts: List<LogMilesDraftUi>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (drafts.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.logging_no_drafts_title),
            subtitle = stringResource(Res.string.logging_no_drafts_subtitle),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = DesignTokens.Spacing.l,
                end = DesignTokens.Spacing.l,
                bottom = DesignTokens.Spacing.xxl,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        items(drafts, key = { it.id }) { draft ->
            DraftCard(draft = draft, onOpen = { onOpen(draft.id) }, onDelete = { onDelete(draft.id) })
        }
    }
}

@Composable
private fun DraftCard(
    draft: LogMilesDraftUi,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        onClick = onOpen,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DesignTokens.IconSize.header),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    draft.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${draft.stopCount} stops · ${draft.distanceKm.formatDecimal(1)} km" +
                        (draft.vehicleName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.logging_updated, DateUtils.epochToDisplayDate(draft.updatedAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(Res.string.logging_delete_draft_cd),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SubmittedTab(vouchers: List<SubmittedVoucher>) {
    if (vouchers.isEmpty()) {
        EmptyState(title = stringResource(Res.string.logging_no_submissions_title), subtitle = stringResource(Res.string.logging_no_submissions_subtitle))
        return
    }
    // Group by expense day, newest first, with a date header per group.
    val grouped =
        vouchers
            .sortedByDescending { it.expenseDateMillis }
            .groupBy { dayKey(it.expenseDateMillis) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = DesignTokens.Spacing.l,
                end = DesignTokens.Spacing.l,
                bottom = DesignTokens.Spacing.xxl,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        grouped.forEach { (_, group) ->
            item(key = "header-${group.first().id}") {
                Text(
                    DateUtils.epochToDisplayDate(group.first().expenseDateMillis),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.s),
                )
            }
            items(group, key = { it.id }) { voucher -> VoucherCard(voucher = voucher) }
        }
    }
}

@Composable
private fun VoucherCard(voucher: SubmittedVoucher) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (voucher.violationCount > 0) {
                    DesignTokens.StatusColors.warning.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
            ),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.header),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "#${voucher.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        voucher.voucherState,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        voucher.payment,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            // Chips row.
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                voucher.chips.forEach { chip -> Pill(text = chip) }
                if (voucher.violationCount > 0) {
                    Pill(
                        text = pluralStringResource(Res.plurals.logging_plural_violations, voucher.violationCount, voucher.violationCount),
                        container = DesignTokens.StatusColors.warning.copy(alpha = 0.15f),
                        content = DesignTokens.StatusColors.warning,
                        leadingWarning = true,
                    )
                }
            }
            Spacer(Modifier.size(DesignTokens.Spacing.xs))
            Pill(text = stringResource(Res.string.logging_office, voucher.office))

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₹ ${voucher.amount.formatDecimal(2)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Pill(
                    text = voucher.serviceTag,
                    container = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    content = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.m))
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )
            Spacer(Modifier.size(DesignTokens.Spacing.m))

            Row(modifier = Modifier.fillMaxWidth()) {
                LabelValue(
                    label = stringResource(Res.string.logging_expense_date),
                    value = DateUtils.epochToDisplayDate(voucher.expenseDateMillis),
                    modifier = Modifier.weight(1f),
                )
                LabelValue(
                    label = stringResource(Res.string.logging_expense_id),
                    value = voucher.expenseId,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Row(modifier = Modifier.fillMaxWidth()) {
                LabelValue(
                    label = stringResource(Res.string.logging_submitted_on),
                    value = DateUtils.epochToDisplayDate(voucher.submittedOnMillis),
                    modifier = Modifier.weight(1f),
                )
                LabelValue(
                    label = stringResource(Res.string.logging_policy_violation),
                    value = if (voucher.violationCount > 0) "${voucher.violationCount}" else stringResource(Res.string.logging_none),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Pill(
    text: String,
    container: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leadingWarning: Boolean = false,
) {
    Surface(shape = DesignTokens.Shape.chip, color = container) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingWarning) {
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.xs))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, color = content)
        }
    }
}

@Composable
private fun LabelValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Bucket key for grouping vouchers by calendar day. */
private fun dayKey(millis: Long): Long = millis / 86_400_000L
