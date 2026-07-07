package com.mileway.feature.payables.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.scaffold.HistoryListScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.payables_doc_status_approved
import com.mileway.core.ui.resources.payables_doc_status_completed
import com.mileway.core.ui.resources.payables_doc_status_draft
import com.mileway.core.ui.resources.payables_doc_status_pending
import com.mileway.core.ui.resources.payables_doc_status_rejected
import com.mileway.core.ui.resources.payables_doc_type_asn
import com.mileway.core.ui.resources.payables_doc_type_gin
import com.mileway.core.ui.resources.payables_doc_type_invoice
import com.mileway.core.ui.resources.payables_doc_type_park
import com.mileway.core.ui.resources.payables_doc_type_pr
import com.mileway.core.ui.resources.payables_filter_all
import com.mileway.core.ui.resources.payables_history_empty_subtitle
import com.mileway.core.ui.resources.payables_history_empty_title
import com.mileway.core.ui.resources.payables_history_search_placeholder
import com.mileway.core.ui.resources.payables_history_title
import com.mileway.feature.payables.model.PayablesDoc
import com.mileway.feature.payables.model.PayablesDocStatus
import com.mileway.feature.payables.model.PayablesDocType
import com.mileway.feature.payables.viewmodel.PAYABLES_HISTORY_TABS
import com.mileway.feature.payables.viewmodel.PayablesHistoryAction
import com.mileway.feature.payables.viewmodel.PayablesHistoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PB.4: unified payables history (Invoice / PR / GIN / Park In-Out / ASN). Doc-type tabs + a status filter
 * chip row + search, all on the shared F0.4 [HistoryListScaffold] with F0.3 [StatusChip] tones.
 */
@Composable
fun PayablesHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayablesHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HistoryListScaffold(
        title = stringResource(Res.string.payables_history_title),
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(PayablesHistoryAction.Refresh) },
        modifier = modifier,
        tabs = PAYABLES_HISTORY_TABS.map { it?.localizedLabel() ?: stringResource(Res.string.payables_filter_all) },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(PayablesHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(PayablesHistoryAction.SetQuery(it)) },
        searchPlaceholder = stringResource(Res.string.payables_history_search_placeholder),
        emptyTitle = stringResource(Res.string.payables_history_empty_title),
        emptySubtitle = stringResource(Res.string.payables_history_empty_subtitle),
        filterChips = {
            StatusFilterChip(stringResource(Res.string.payables_filter_all), ui.statusFilter == null) {
                viewModel.onAction(PayablesHistoryAction.SetStatusFilter(null))
            }
            PayablesDocStatus.entries.forEach { status ->
                StatusFilterChip(status.localizedLabel(), ui.statusFilter == status) {
                    viewModel.onAction(PayablesHistoryAction.SetStatusFilter(status))
                }
            }
        },
        itemKey = { it.id },
    ) { doc ->
        PayablesDocCard(doc)
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun PayablesDocCard(doc: PayablesDoc) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${doc.type.localizedLabel()} · ${doc.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(label = doc.status.localizedLabel(), tone = toneFor(doc.status))
            }
            Text(
                "${doc.title} · ${doc.reference}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (doc.amount != null) {
                Text(
                    "₹${doc.amount.toLong()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun toneFor(status: PayablesDocStatus): StatusTone =
    when (status) {
        PayablesDocStatus.DRAFT -> StatusTone.Neutral
        PayablesDocStatus.PENDING -> StatusTone.Warning
        PayablesDocStatus.APPROVED -> StatusTone.Success
        PayablesDocStatus.REJECTED -> StatusTone.Error
        PayablesDocStatus.COMPLETED -> StatusTone.Info
    }

/**
 * Localized display label for a document status. The enum's own `label` stays
 * canonical English for the non-composable search provider.
 */
@Composable
private fun PayablesDocStatus.localizedLabel(): String =
    when (this) {
        PayablesDocStatus.DRAFT -> stringResource(Res.string.payables_doc_status_draft)
        PayablesDocStatus.PENDING -> stringResource(Res.string.payables_doc_status_pending)
        PayablesDocStatus.APPROVED -> stringResource(Res.string.payables_doc_status_approved)
        PayablesDocStatus.REJECTED -> stringResource(Res.string.payables_doc_status_rejected)
        PayablesDocStatus.COMPLETED -> stringResource(Res.string.payables_doc_status_completed)
    }

/** Localized display label for a document type (canonical `label` kept for search). */
@Composable
private fun PayablesDocType.localizedLabel(): String =
    when (this) {
        PayablesDocType.INVOICE -> stringResource(Res.string.payables_doc_type_invoice)
        PayablesDocType.PURCHASE_REQUEST -> stringResource(Res.string.payables_doc_type_pr)
        PayablesDocType.GIN -> stringResource(Res.string.payables_doc_type_gin)
        PayablesDocType.PARK_IN_OUT -> stringResource(Res.string.payables_doc_type_park)
        PayablesDocType.ASN -> stringResource(Res.string.payables_doc_type_asn)
    }
