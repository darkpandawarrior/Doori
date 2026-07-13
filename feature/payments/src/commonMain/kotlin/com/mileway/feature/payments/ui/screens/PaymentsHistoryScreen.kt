package com.mileway.feature.payments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
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
import com.mileway.core.ui.resources.payments_direction_pay
import com.mileway.core.ui.resources.payments_direction_request
import com.mileway.core.ui.resources.payments_empty_subtitle
import com.mileway.core.ui.resources.payments_empty_title
import com.mileway.core.ui.resources.payments_history_subtitle
import com.mileway.core.ui.resources.payments_history_title
import com.mileway.core.ui.resources.payments_search_placeholder
import com.mileway.core.ui.resources.payments_status_active
import com.mileway.core.ui.resources.payments_status_completed
import com.mileway.core.ui.resources.payments_status_expired
import com.mileway.core.ui.resources.payments_status_failed
import com.mileway.core.ui.resources.payments_status_pending
import com.mileway.core.ui.resources.payments_tab_all
import com.mileway.feature.payments.model.PaymentDirection
import com.mileway.feature.payments.model.PaymentRecord
import com.mileway.feature.payments.model.PaymentStatus
import com.mileway.feature.payments.viewmodel.PAYMENTS_HISTORY_TABS
import com.mileway.feature.payments.viewmodel.PaymentsHistoryAction
import com.mileway.feature.payments.viewmodel.PaymentsHistoryViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** PM: payments history on the shared F0.4 HistoryListScaffold + F0.3 StatusChip. */
@Composable
fun PaymentsHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentsHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    val allLabel = stringResource(Res.string.payments_tab_all)
    HistoryListScaffold(
        title = stringResource(Res.string.payments_history_title),
        subtitle = stringResource(Res.string.payments_history_subtitle),
        titleIcon = Icons.Filled.Payments,
        onBack = onBack,
        state = ui.list,
        onRetry = { viewModel.onAction(PaymentsHistoryAction.Refresh) },
        modifier = modifier,
        tabs = PAYMENTS_HISTORY_TABS.map { it?.localizedLabel() ?: allLabel },
        selectedTab = ui.tabIndex,
        onSelectTab = { viewModel.onAction(PaymentsHistoryAction.SelectTab(it)) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(PaymentsHistoryAction.SetQuery(it)) },
        searchPlaceholder = stringResource(Res.string.payments_search_placeholder),
        emptyTitle = stringResource(Res.string.payments_empty_title),
        emptySubtitle = stringResource(Res.string.payments_empty_subtitle),
        itemKey = { it.id },
    ) { payment ->
        PaymentCard(payment)
    }
}

@Composable
private fun PaymentCard(payment: PaymentRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${payment.direction.localizedLabel()} · ${payment.counterparty}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusChip(label = payment.status.localizedLabel(), tone = toneFor(payment.status))
            }
            if (payment.note.isNotBlank()) {
                Text(
                    payment.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                "₹${payment.amount.toLong()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun toneFor(status: PaymentStatus): StatusTone =
    when (status) {
        PaymentStatus.PENDING -> StatusTone.Warning
        PaymentStatus.ACTIVE -> StatusTone.Warning
        PaymentStatus.COMPLETED -> StatusTone.Success
        PaymentStatus.FAILED -> StatusTone.Error
        PaymentStatus.EXPIRED -> StatusTone.Error
    }

/** Localized display label for a payment status; the enum's `label` stays canonical for search. */
@Composable
internal fun PaymentStatus.localizedLabel(): String =
    when (this) {
        PaymentStatus.PENDING -> stringResource(Res.string.payments_status_pending)
        PaymentStatus.ACTIVE -> stringResource(Res.string.payments_status_active)
        PaymentStatus.COMPLETED -> stringResource(Res.string.payments_status_completed)
        PaymentStatus.FAILED -> stringResource(Res.string.payments_status_failed)
        PaymentStatus.EXPIRED -> stringResource(Res.string.payments_status_expired)
    }

/** Localized display label for a payment direction; the enum's `label` stays canonical for search. */
@Composable
internal fun PaymentDirection.localizedLabel(): String =
    when (this) {
        PaymentDirection.PAY -> stringResource(Res.string.payments_direction_pay)
        PaymentDirection.REQUEST -> stringResource(Res.string.payments_direction_request)
    }
