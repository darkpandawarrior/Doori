package com.mileway.feature.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_sync_apply_current
import com.mileway.core.ui.resources.profile_sync_apply_future
import com.mileway.core.ui.resources.profile_sync_events
import com.mileway.core.ui.resources.profile_sync_failed
import com.mileway.core.ui.resources.profile_sync_force
import com.mileway.core.ui.resources.profile_sync_interval_label
import com.mileway.core.ui.resources.profile_sync_interval_value
import com.mileway.core.ui.resources.profile_sync_last_sync
import com.mileway.core.ui.resources.profile_sync_locations
import com.mileway.core.ui.resources.profile_sync_next_due
import com.mileway.core.ui.resources.profile_sync_pending
import com.mileway.core.ui.resources.profile_sync_settings_title
import com.mileway.core.ui.resources.profile_sync_syncing
import com.mileway.core.ui.resources.profile_sync_test_passed
import com.mileway.core.ui.resources.profile_sync_testing
import com.mileway.core.ui.resources.profile_sync_title
import com.mileway.core.ui.resources.profile_sync_toggle_debug
import com.mileway.core.ui.resources.profile_sync_toggle_events
import com.mileway.core.ui.resources.profile_sync_toggle_location
import com.mileway.core.ui.resources.profile_sync_toggle_v2
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.profile.model.SyncMetrics
import com.mileway.feature.profile.viewmodel.SyncDiagnosticsViewModel
import com.mileway.feature.profile.viewmodel.SyncGuardState
import com.mileway.feature.profile.viewmodel.SyncSettingsUiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private const val INTERVAL_MIN = 5
private const val INTERVAL_MAX = 60
private const val INTERVAL_STEP = 5

private fun formatLastSync(ms: Long?): String {
    if (ms == null) return "Never"
    val ldt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = ldt.hour.toString().padStart(2, '0')
    val minute = ldt.minute.toString().padStart(2, '0')
    return "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]}, $hour:$minute"
}

/**
 * Callbacks the Mileage-Sync card fires back to [SyncDiagnosticsViewModel]. Bundled so the stateless
 * content can be previewed/tested without a ViewModel.
 */
data class SyncSettingsCallbacks(
    val onForceSync: () -> Unit,
    val onLocationSync: (Boolean) -> Unit,
    val onEventSync: (Boolean) -> Unit,
    val onDebugEvents: (Boolean) -> Unit,
    val onV2Api: (Boolean) -> Unit,
    val onInterval: (Int) -> Unit,
    val onApplyToFuture: (Boolean) -> Unit,
)

/**
 * PLAN_V22 P6.7 / PLAN_V24 P10.2: Settings' "Mileage Sync" card — the local staging/synced metrics
 * plus the sync-settings surface (per-bucket toggles, auto-sync interval, apply-target switch, and a
 * simulated pre-enable connectivity self-test). All local; no network call — Mileway has no backend
 * to sync against yet (see CLAUDE.md "The backend").
 */
@Composable
fun SyncDiagnosticsCard(viewModel: SyncDiagnosticsViewModel = koinViewModel()) {
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    SyncDiagnosticsCardContent(
        metrics = metrics,
        settings = settings,
        callbacks =
            SyncSettingsCallbacks(
                onForceSync = viewModel::forceSync,
                onLocationSync = viewModel::setLocationSync,
                onEventSync = viewModel::setEventSync,
                onDebugEvents = viewModel::setDebugEvents,
                onV2Api = viewModel::setV2Api,
                onInterval = viewModel::setInterval,
                onApplyToFuture = viewModel::setApplyToFutureJourneys,
            ),
    )
}

@Composable
internal fun SyncDiagnosticsCardContent(
    metrics: SyncMetrics,
    settings: SyncSettingsUiState,
    callbacks: SyncSettingsCallbacks,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.l),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Text(
                text = stringResource(Res.string.profile_sync_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SyncMetricColumn(label = stringResource(Res.string.profile_sync_locations), value = metrics.locationsSynced.toString())
                SyncMetricColumn(label = stringResource(Res.string.profile_sync_events), value = metrics.eventsSynced.toString())
                SyncMetricColumn(
                    label = stringResource(Res.string.profile_sync_failed),
                    value = metrics.failedAttempts.toString(),
                    valueColor =
                        if (metrics.failedAttempts > 0) {
                            DesignTokens.StatusColors.warning
                        } else {
                            DesignTokens.StatusColors.success
                        },
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Text(
                text =
                    stringResource(
                        Res.string.profile_sync_pending,
                        metrics.pendingLocations,
                        metrics.pendingEvents + metrics.pendingDebugEvents,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Text(
                text = stringResource(Res.string.profile_sync_last_sync, formatLastSync(metrics.lastSyncTimeMs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (metrics.nextSyncDueMs != null) {
                Text(
                    text = stringResource(Res.string.profile_sync_next_due, formatLastSync(metrics.nextSyncDueMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = callbacks.onForceSync,
                enabled = !metrics.isSyncing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (metrics.isSyncing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(text = stringResource(Res.string.profile_sync_syncing))
                    }
                } else {
                    Text(text = stringResource(Res.string.profile_sync_force))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.m))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.profile_sync_settings_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                when (settings.guard) {
                    SyncGuardState.Testing ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            Text(
                                text = stringResource(Res.string.profile_sync_testing),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    SyncGuardState.Passed ->
                        Text(
                            text = stringResource(Res.string.profile_sync_test_passed),
                            style = MaterialTheme.typography.labelSmall,
                            color = DesignTokens.StatusColors.success,
                        )
                    SyncGuardState.Idle -> Unit
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.xs))

            SyncToggleRow(stringResource(Res.string.profile_sync_toggle_location), settings.locationEnabled, callbacks.onLocationSync)
            SyncToggleRow(stringResource(Res.string.profile_sync_toggle_events), settings.eventsEnabled, callbacks.onEventSync)
            SyncToggleRow(stringResource(Res.string.profile_sync_toggle_debug), settings.debugEventsEnabled, callbacks.onDebugEvents)
            SyncToggleRow(stringResource(Res.string.profile_sync_toggle_v2), settings.v2ApiEnabled, callbacks.onV2Api)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.profile_sync_interval_label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    shape = DesignTokens.Shape.button,
                    enabled = settings.intervalMinutes > INTERVAL_MIN,
                    onClick = { callbacks.onInterval((settings.intervalMinutes - INTERVAL_STEP).coerceAtLeast(INTERVAL_MIN)) },
                ) { Text("−") }
                Text(
                    text = stringResource(Res.string.profile_sync_interval_value, settings.intervalMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s).width(56.dp),
                )
                OutlinedButton(
                    shape = DesignTokens.Shape.button,
                    enabled = settings.intervalMinutes < INTERVAL_MAX,
                    onClick = { callbacks.onInterval((settings.intervalMinutes + INTERVAL_STEP).coerceAtMost(INTERVAL_MAX)) },
                ) { Text("+") }
            }

            SyncToggleRow(stringResource(Res.string.profile_sync_apply_future), settings.applyToFutureJourneys, callbacks.onApplyToFuture)
            if (settings.sessionOverrideActive) {
                Text(
                    text = stringResource(Res.string.profile_sync_apply_current),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SyncToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SyncMetricColumn(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
