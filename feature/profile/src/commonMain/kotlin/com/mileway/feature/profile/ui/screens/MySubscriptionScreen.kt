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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.subscription.SubscriptionStatus
import com.mileway.core.data.util.CommonUtils
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.viewmodel.SubscriptionViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SUB_MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * PLAN_V24 P6.2: active subscription detail (source: the reference app active-subscription screen). Shows plan,
 * status, dates, the "savings so far" counter, and renew / change-plan / cancel actions. Cancel keeps
 * access until the period end (source semantics) — surfaced as a "cancels on …" note, not an instant
 * loss of access. Renew and cancel are guarded by confirm dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySubscriptionScreen(
    onBack: () -> Unit,
    onOpenPlans: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCancel by remember { mutableStateOf(false) }
    var showRenew by remember { mutableStateOf(false) }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF0B2A6B))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = sub("mysub_back", "Back"), tint = Color.White)
                    }
                    Text(
                        sub("mysub_title", "My subscription"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            val active = state.active
            val plan = state.activePlan
            if (active == null || plan == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(DesignTokens.Spacing.xl))
                    Text(
                        sub("mysub_empty", "You don't have an active subscription."),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onOpenPlans, modifier = Modifier.fillMaxWidth()) { Text(sub("mysub_see_plans", "See plans")) }
                }
                return@Column
            }

            Column(
                modifier =
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedMd,
                    elevation = CardDefaults.cardElevation(DesignTokens.Elevation.card),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            statusLabel(active.status),
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor(active.status),
                            fontWeight = FontWeight.SemiBold,
                        )
                        DetailRow(sub("mysub_started", "Started"), formatSubDate(active.startedAtMs))
                        DetailRow(
                            if (active.cancelAtPeriodEnd) sub("mysub_access_until", "Access until") else sub("mysub_renews", "Renews on"),
                            formatSubDate(active.renewsAtMs),
                        )
                    }
                }

                Surface(color = Color(0xFF16A34A).copy(alpha = 0.10f), shape = DesignTokens.Shape.roundedMd, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            sub("mysub_savings_label", "Saved so far"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            CommonUtils.formatCurrencyAmount(state.savingsSoFar),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                        )
                    }
                }

                if (active.cancelAtPeriodEnd) {
                    Text(
                        subArg("mysub_cancel_note", "Your subscription cancels on %1\$s.", formatSubDate(active.renewsAtMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(onClick = { showRenew = true }, modifier = Modifier.fillMaxWidth()) { Text(sub("mysub_renew", "Renew now")) }
                OutlinedButton(onClick = onOpenPlans, modifier = Modifier.fillMaxWidth()) { Text(sub("mysub_change", "Change plan")) }
                if (!active.cancelAtPeriodEnd) {
                    TextButton(onClick = { showCancel = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(sub("mysub_cancel", "Cancel subscription"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showRenew) {
        ConfirmDialog(
            title = sub("mysub_renew_title", "Renew subscription?"),
            body = sub("mysub_renew_body", "This extends your subscription by one more period."),
            confirmLabel = sub("mysub_renew_confirm", "Renew"),
            onConfirm = {
                viewModel.renew()
                showRenew = false
            },
            onDismiss = { showRenew = false },
        )
    }
    if (showCancel) {
        ConfirmDialog(
            title = sub("mysub_cancel_title", "Cancel subscription?"),
            body = sub("mysub_cancel_body", "You'll keep access until the end of the current period."),
            confirmLabel = sub("mysub_cancel_confirm", "Cancel subscription"),
            onConfirm = {
                viewModel.cancel()
                showCancel = false
            },
            onDismiss = { showCancel = false },
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(sub("mysub_dialog_dismiss", "Not now")) } },
    )
}

@Composable
private fun statusLabel(status: SubscriptionStatus): String =
    when (status) {
        SubscriptionStatus.ACTIVE -> sub("mysub_status_active", "Active")
        SubscriptionStatus.EXPIRED -> sub("mysub_status_expired", "Expired")
        SubscriptionStatus.CANCELLED -> sub("mysub_status_cancelled", "Cancelled")
    }

private fun statusColor(status: SubscriptionStatus): Color =
    when (status) {
        SubscriptionStatus.ACTIVE -> Color(0xFF16A34A)
        SubscriptionStatus.EXPIRED -> Color(0xFFB91C1C)
        SubscriptionStatus.CANCELLED -> Color(0xFFB45309)
    }

private fun formatSubDate(ms: Long): String =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
        "${ldt.dayOfMonth} ${SUB_MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
    }

@Composable
private fun sub(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback

@Composable
private fun subArg(
    key: String,
    fallback: String,
    arg: String,
): String = Res.allStringResources[key]?.let { stringResource(it, arg) } ?: fallback.replace("%1\$s", arg)
