package com.mileway.feature.approvals.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.scaffold.HistoryListScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.approvals_clarification_history_empty
import com.mileway.core.ui.resources.approvals_clarification_history_subtitle
import com.mileway.core.ui.resources.approvals_clarification_history_title
import com.mileway.core.ui.resources.approvals_date_range_all
import com.mileway.core.ui.resources.approvals_date_range_month
import com.mileway.core.ui.resources.approvals_date_range_today
import com.mileway.core.ui.resources.approvals_date_range_week
import com.mileway.core.ui.resources.approvals_room_status_active
import com.mileway.core.ui.resources.approvals_room_status_closed
import com.mileway.core.ui.resources.approvals_tab_history_active
import com.mileway.core.ui.resources.approvals_tab_history_closed
import com.mileway.core.ui.resources.approvals_tab_history_saved
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import com.mileway.feature.approvals.viewmodel.ClarificationDateRange
import com.mileway.feature.approvals.viewmodel.ClarificationHistoryAction
import com.mileway.feature.approvals.viewmodel.ClarificationHistoryEffect
import com.mileway.feature.approvals.viewmodel.ClarificationHistoryTab
import com.mileway.feature.approvals.viewmodel.ClarificationHistoryViewModel
import com.mileway.feature.approvals.viewmodel.ClarificationRoomListItem
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V28 P28.2/P28.5: top-level entry point for browsing every clarification room, independent
 * of any single approval's detail-screen lifecycle. Tapping a row opens that approval's detail
 * screen (where the actual chat lives) — a dedicated room-only chat surface isn't needed since the
 * thread already renders there. P28.5: Active/Closed/Saved tabs (scaffold's `tabs`), a requester/
 * summary search (scaffold's built-in search field), and a date-range filter reusing the scaffold's
 * `filterChips` slot — no new scaffold API needed for four fixed buckets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClarificationHistoryScreen(
    onBack: () -> Unit,
    onOpenApproval: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClarificationHistoryViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ClarificationHistoryEffect.NavigateToApproval -> onOpenApproval(effect.approvalId)
            }
        }
    }

    val tabLabels =
        listOf(
            stringResource(Res.string.approvals_tab_history_active),
            stringResource(Res.string.approvals_tab_history_closed),
            stringResource(Res.string.approvals_tab_history_saved),
        )
    val dateRangeLabels =
        mapOf(
            ClarificationDateRange.ALL to stringResource(Res.string.approvals_date_range_all),
            ClarificationDateRange.TODAY to stringResource(Res.string.approvals_date_range_today),
            ClarificationDateRange.WEEK to stringResource(Res.string.approvals_date_range_week),
            ClarificationDateRange.MONTH to stringResource(Res.string.approvals_date_range_month),
        )

    HistoryListScaffold(
        title = stringResource(Res.string.approvals_clarification_history_title),
        subtitle = stringResource(Res.string.approvals_clarification_history_subtitle),
        titleIcon = Icons.Filled.Chat,
        onBack = onBack,
        state = ui.rooms,
        onRetry = {},
        modifier = modifier,
        emptyTitle = stringResource(Res.string.approvals_clarification_history_empty),
        itemKey = { it.room.roomId },
        tabs = tabLabels,
        selectedTab = ui.tab.ordinal,
        onSelectTab = { index -> viewModel.onAction(ClarificationHistoryAction.SelectTab(ClarificationHistoryTab.entries[index])) },
        query = ui.query,
        onQueryChange = { viewModel.onAction(ClarificationHistoryAction.SetQuery(it)) },
        filterChips = {
            dateRangeLabels.forEach { (range, label) ->
                FilterChip(
                    selected = ui.dateRange == range,
                    onClick = { viewModel.onAction(ClarificationHistoryAction.SetDateRange(range)) },
                    label = { Text(label) },
                )
            }
        },
    ) { item ->
        ClarificationRoomCard(
            item = item,
            onClick = { viewModel.onAction(ClarificationHistoryAction.OpenRoom(item.room.approvalId)) },
        )
    }
}

@Composable
private fun ClarificationRoomCard(
    item: ClarificationRoomListItem,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick, shape = DesignTokens.Shape.button) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.requesterName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val isActive = item.room.status == ClarificationRoomStatus.ACTIVE
            StatusChip(
                label = stringResource(if (isActive) Res.string.approvals_room_status_active else Res.string.approvals_room_status_closed),
                tone = if (isActive) StatusTone.Success else StatusTone.Neutral,
            )
        }
    }
}
