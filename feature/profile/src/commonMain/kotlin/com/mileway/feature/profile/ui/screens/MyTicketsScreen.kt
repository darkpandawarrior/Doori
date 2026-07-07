package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_ticket_status_in_progress
import com.mileway.core.ui.resources.profile_ticket_status_open
import com.mileway.core.ui.resources.profile_ticket_status_resolved
import com.mileway.core.ui.resources.profile_tickets_back
import com.mileway.core.ui.resources.profile_tickets_empty
import com.mileway.core.ui.resources.profile_tickets_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.model.SupportTicket
import com.mileway.feature.profile.model.SupportTicketStatus
import com.mileway.feature.profile.viewmodel.SupportTicketViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatTicketDate(ms: Long): String =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
        "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
    }

private fun SupportTicketStatus.tone(): StatusTone =
    when (this) {
        SupportTicketStatus.OPEN -> StatusTone.Info
        SupportTicketStatus.IN_PROGRESS -> StatusTone.Warning
        SupportTicketStatus.RESOLVED -> StatusTone.Success
    }

@Composable
private fun SupportTicketStatus.label(): String =
    when (this) {
        SupportTicketStatus.OPEN -> stringResource(Res.string.profile_ticket_status_open)
        SupportTicketStatus.IN_PROGRESS -> stringResource(Res.string.profile_ticket_status_in_progress)
        SupportTicketStatus.RESOLVED -> stringResource(Res.string.profile_ticket_status_resolved)
    }

/**
 * PLAN_V22 P6.8: "My Tickets" — the real, persisted list of tickets submitted via `HelpScreen`'s
 * "Contact Support" form, replacing that form's previous fire-and-forget snackbar with nothing
 * inspectable afterward. Own Matrix/terminal card layout, not a port of any reference app screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupportTicketViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_tickets_title),
                subtitle = "${uiState.tickets.size} ticket${if (uiState.tickets.size == 1) "" else "s"} submitted",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_tickets_back))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.tickets.isEmpty()) {
            Column(
                modifier = modifier.fillMaxSize().padding(padding).padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(Res.string.profile_tickets_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                items(uiState.tickets, key = { it.id }) { ticket -> TicketRow(ticket) }
            }
        }
    }
}

@Composable
private fun TicketRow(ticket: SupportTicket) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ticket.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                StatusChip(label = ticket.status.label(), tone = ticket.status.tone())
            }
            Text(ticket.subject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(ticket.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatTicketDate(ticket.createdAtMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
