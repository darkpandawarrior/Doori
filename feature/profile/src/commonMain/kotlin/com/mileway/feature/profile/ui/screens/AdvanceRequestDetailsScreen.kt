package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.mvi.DefaultEmptyState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_advance_approver_chain
import com.mileway.core.ui.resources.profile_advance_back
import com.mileway.core.ui.resources.profile_advance_decline_reason
import com.mileway.core.ui.resources.profile_advance_log_expense
import com.mileway.core.ui.resources.profile_advance_not_found_subtitle
import com.mileway.core.ui.resources.profile_advance_not_found_title
import com.mileway.core.ui.resources.profile_advance_request_title
import com.mileway.core.ui.resources.profile_advance_start_trip
import com.mileway.core.ui.resources.profile_advance_status_approved
import com.mileway.core.ui.resources.profile_advance_status_disbursed
import com.mileway.core.ui.resources.profile_advance_status_pending
import com.mileway.core.ui.resources.profile_advance_status_rejected
import com.mileway.core.ui.resources.profile_advance_status_under_review
import com.mileway.core.ui.resources.profile_advance_timeline
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.profile.model.AdvanceRecord
import com.mileway.feature.profile.model.AdvanceStatus
import com.mileway.feature.profile.model.ApproverStep
import com.mileway.feature.profile.model.ApproverStepStatus
import com.mileway.feature.profile.model.TimelineEntry
import com.mileway.feature.profile.viewmodel.AdvanceAction
import com.mileway.feature.profile.viewmodel.AdvanceEffect
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatDate(ms: Long): String =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
        "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
    }

/**
 * Drill-down detail screen for a single [AdvanceRecord], opened by tapping a card on
 * [AdvanceHistoryScreen]. Shows the hero amount/status, the approver chain, and a request
 * timeline, all sourced from the same stub-backed [AdvanceViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceRequestDetailsScreen(
    advanceId: String,
    onBack: () -> Unit,
    onStartTrip: (advanceId: String, tripId: String) -> Unit = { _, _ -> },
    // P27.E.8: default no-op keeps every existing call site unchanged; the app shell's
    // profileGraph supplies the real navigation into feature:logging's expense-entry route.
    onLogExpense: (ExpenseSourceContext) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AdvanceViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(advanceId) {
        viewModel.onAction(AdvanceAction.LoadDetail(advanceId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AdvanceEffect.NavigateToTripStart -> onStartTrip(effect.advanceId, effect.tripId)
                is AdvanceEffect.NavigateToExpenseEntry -> onLogExpense(effect.context)
                is AdvanceEffect.ShowToast -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_advance_request_title),
                subtitle = advanceId,
                depth = NavigationDepth.LEVEL_2,
                titleIcon = Icons.Filled.Payments,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_advance_back))
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        ScreenStateContent(
            state = ui.detail,
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
            empty = {
                DefaultEmptyState(
                    title = stringResource(Res.string.profile_advance_not_found_title),
                    subtitle = stringResource(Res.string.profile_advance_not_found_subtitle),
                )
            },
        ) { record ->
            AdvanceRequestDetailsContent(
                record = record,
                onStartTripClick = { viewModel.onAction(AdvanceAction.StartTripAgainstAdvance(record.id)) },
                onLogExpenseClick = { viewModel.onAction(AdvanceAction.LogExpenseAgainstAdvance(record.id)) },
            )
        }
    }
}

@Composable
private fun AdvanceRequestDetailsContent(
    record: AdvanceRecord,
    onStartTripClick: () -> Unit,
    onLogExpenseClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = DesignTokens.Spacing.l,
                vertical = DesignTokens.Spacing.l,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        item { HeroCard(record) }
        val declineReason = record.declineReason
        if (record.status == AdvanceStatus.REJECTED && !declineReason.isNullOrBlank()) {
            item { DeclineReasonCard(declineReason) }
        }
        if (record.status != AdvanceStatus.REJECTED) {
            item { StartTripCta(onClick = onStartTripClick) }
            // P27.E.8: "Log expense against this advance" — mirrors the start-trip CTA's gating
            // (a rejected advance can't be spent against either).
            item { LogExpenseCta(onClick = onLogExpenseClick) }
        }
        if (record.approverChain.isNotEmpty()) {
            item { ApproverChainCard(record.approverChain) }
        }
        if (record.timeline.isNotEmpty()) {
            item { TimelineCard(record.timeline) }
        }
        item { Spacer(Modifier.height(DesignTokens.Spacing.l)) }
    }
}

@Composable
private fun StartTripCta(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
        Spacer(Modifier.width(DesignTokens.Spacing.s))
        Text(stringResource(Res.string.profile_advance_start_trip))
    }
}

@Composable
private fun LogExpenseCta(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
        Spacer(Modifier.width(DesignTokens.Spacing.s))
        Text(stringResource(Res.string.profile_advance_log_expense))
    }
}

@Composable
private fun HeroCard(record: AdvanceRecord) {
    val (statusLabel, statusColor) =
        when (record.status) {
            AdvanceStatus.PENDING -> stringResource(Res.string.profile_advance_status_pending) to StatusColors.warning
            AdvanceStatus.UNDER_REVIEW -> stringResource(Res.string.profile_advance_status_under_review) to StatusColors.info
            AdvanceStatus.APPROVED -> stringResource(Res.string.profile_advance_status_approved) to StatusColors.success
            AdvanceStatus.DISBURSED -> stringResource(Res.string.profile_advance_status_disbursed) to StatusColors.info
            AdvanceStatus.REJECTED -> stringResource(Res.string.profile_advance_status_rejected) to StatusColors.error
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "₹${record.amountRupees.toLong()}",
                style = MaterialTheme.typography.headlineMedium.dataStyle(),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Surface(color = statusColor.copy(alpha = 0.15f), shape = DesignTokens.Shape.chip) {
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Text(
                record.purpose,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                "Requested ${formatDate(record.requestedDateMs)} · Required by ${record.requiredByDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeclineReasonCard(reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = StatusColors.error.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Text(
                stringResource(Res.string.profile_advance_decline_reason),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = StatusColors.error,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(reason, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ApproverChainCard(steps: List<ApproverStep>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Text(stringResource(Res.string.profile_advance_approver_chain), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            steps.forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs))
                ApproverStepRow(step)
            }
        }
    }
}

@Composable
private fun ApproverStepRow(step: ApproverStep) {
    val statusColor =
        when (step.status) {
            ApproverStepStatus.APPROVED -> StatusColors.success
            ApproverStepStatus.REJECTED -> StatusColors.error
            ApproverStepStatus.PENDING -> StatusColors.warning
        }
    val statusLabel =
        when (step.status) {
            ApproverStepStatus.APPROVED -> stringResource(Res.string.profile_advance_status_approved)
            ApproverStepStatus.REJECTED -> stringResource(Res.string.profile_advance_status_rejected)
            ApproverStepStatus.PENDING -> stringResource(Res.string.profile_advance_status_pending)
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(color = statusColor, shape = DesignTokens.Shape.button),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(step.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(step.stageLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = statusColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimelineCard(entries: List<TimelineEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Text(stringResource(Res.string.profile_advance_timeline), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(entry.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        formatDate(entry.dateMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
