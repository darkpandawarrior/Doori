@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

package com.mileway.feature.profile.ui.screens

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.mvi.DefaultEmptyState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_advance_back
import com.mileway.core.ui.resources.profile_advance_empty_subtitle
import com.mileway.core.ui.resources.profile_advance_empty_title
import com.mileway.core.ui.resources.profile_advance_my_advances
import com.mileway.core.ui.resources.profile_advance_records_count
import com.mileway.core.ui.resources.profile_advance_request_advance
import com.mileway.core.ui.resources.profile_advance_status_approved
import com.mileway.core.ui.resources.profile_advance_status_disbursed
import com.mileway.core.ui.resources.profile_advance_status_pending
import com.mileway.core.ui.resources.profile_advance_status_rejected
import com.mileway.core.ui.resources.profile_advance_status_under_review
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.profile.model.AdvanceRecord
import com.mileway.feature.profile.model.AdvanceStatus
import com.mileway.feature.profile.model.AdvanceType
import com.mileway.feature.profile.viewmodel.AdvanceAction
import com.mileway.feature.profile.viewmodel.AdvanceTabFilter
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdvanceHistoryScreen(
    onBack: () -> Unit,
    onRequestAdvance: () -> Unit,
    onOpenDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AdvanceViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRequestAdvance,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.profile_advance_request_advance)) },
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
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1A237E), Color(0xFF3949AB))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_advance_back), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.profile_advance_my_advances),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(Res.string.profile_advance_records_count, ui.list.records.dataOrNull?.size ?: 0),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                AdvanceTabFilter.entries.forEach { tab ->
                    FilterChip(
                        selected = ui.list.activeTab == tab,
                        onClick = { viewModel.onAction(AdvanceAction.SetTab(tab)) },
                        label = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                    )
                }
            }

            ScreenStateContent(
                state = ui.list.records,
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                onRetry = { viewModel.onAction(AdvanceAction.Refresh) },
                empty = {
                    DefaultEmptyState(
                        title = stringResource(Res.string.profile_advance_empty_title),
                        subtitle = stringResource(Res.string.profile_advance_empty_subtitle),
                        ctaLabel = stringResource(Res.string.profile_advance_request_advance),
                        onCta = onRequestAdvance,
                    )
                },
            ) { records ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = DesignTokens.Spacing.l,
                            vertical = DesignTokens.Spacing.m,
                        ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    items(records, key = { it.id }) { record ->
                        AdvanceCard(record = record, onClick = { onOpenDetail(record.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

internal fun AdvanceType.label(): String =
    when (this) {
        AdvanceType.TRAVEL -> "Travel"
        AdvanceType.FIELD_VISIT -> "Field Visit"
        AdvanceType.TRAINING -> "Training"
        AdvanceType.CLIENT_ONBOARDING -> "Client Onboarding"
    }

@Composable
private fun AdvanceCard(
    record: AdvanceRecord,
    onClick: () -> Unit,
) {
    val (statusLabel, statusColor) =
        when (record.status) {
            AdvanceStatus.PENDING -> stringResource(Res.string.profile_advance_status_pending) to StatusColors.warning
            AdvanceStatus.UNDER_REVIEW -> stringResource(Res.string.profile_advance_status_under_review) to StatusColors.info
            AdvanceStatus.APPROVED -> stringResource(Res.string.profile_advance_status_approved) to StatusColors.success
            AdvanceStatus.DISBURSED -> stringResource(Res.string.profile_advance_status_disbursed) to StatusColors.info
            AdvanceStatus.REJECTED -> stringResource(Res.string.profile_advance_status_rejected) to StatusColors.error
        }
    val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    Card(
        onClick = onClick,
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
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(record.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(record.purpose, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                if (record.type != null) {
                    Text(
                        record.type.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    Instant.fromEpochMilliseconds(record.requestedDateMs).toLocalDateTime(TimeZone.currentSystemDefault()).let {
                            ldt ->
                        "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${record.amountRupees.toLong()}", style = MaterialTheme.typography.titleSmall.dataStyle(), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
