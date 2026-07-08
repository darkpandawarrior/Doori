package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.resources.profile_plural_devices_signed_in
import com.mileway.core.ui.resources.profile_sessions_back
import com.mileway.core.ui.resources.profile_sessions_revoke
import com.mileway.core.ui.resources.profile_sessions_revoke_desc
import com.mileway.core.ui.resources.profile_sessions_revoke_title
import com.mileway.core.ui.resources.profile_sessions_sign_out_all_others
import com.mileway.core.ui.resources.profile_sessions_signout_all_confirm
import com.mileway.core.ui.resources.profile_sessions_signout_all_desc
import com.mileway.core.ui.resources.profile_sessions_signout_all_title
import com.mileway.core.ui.resources.profile_sessions_status_active
import com.mileway.core.ui.resources.profile_sessions_status_idle
import com.mileway.core.ui.resources.profile_sessions_status_recent
import com.mileway.core.ui.resources.profile_sessions_this_device
import com.mileway.core.ui.resources.profile_sessions_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.profile.model.ActiveSession
import com.mileway.feature.profile.model.SessionStatus
import com.mileway.feature.profile.viewmodel.ActiveSessionsViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/**
 * PLAN_V22 P6.4: full active-sessions surface, promoted from the read-only `SessionsDialog` list
 * previously shown from the Account hub — real Room-backed persistence via [ActiveSessionsViewModel]
 * so a revoke actually survives app kill/relaunch instead of resetting on next launch. Own
 * Matrix/terminal layout — status badges (Active/Recent/Idle) and revoke affordances are new here,
 * not a port of any reference app screen's visual design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveSessionsViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    val now = remember { Clock.System.now().toEpochMilliseconds() }
    var revokeTarget by remember { mutableStateOf<ActiveSession?>(null) }
    var detailsTarget by remember { mutableStateOf<ActiveSession?>(null) }
    var confirmSignOutAll by remember { mutableStateOf(false) }

    val hasOtherSessions = uiState.sessions.any { !it.isCurrent }

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_sessions_title),
                subtitle = pluralStringResource(Res.plurals.profile_plural_devices_signed_in, uiState.sessions.size, uiState.sessions.size),
                depth = NavigationDepth.LEVEL_1,
                titleIcon = Icons.Filled.Devices,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_sessions_back))
                    }
                },
                actions = {
                    if (hasOtherSessions) {
                        TextButton(onClick = { confirmSignOutAll = true }, shape = DesignTokens.Shape.button) {
                            Text(stringResource(Res.string.profile_sessions_sign_out_all_others))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            items(uiState.sessions, key = { it.id }) { session ->
                SessionRow(
                    session = session,
                    status = session.status(now),
                    onRevoke = { revokeTarget = session },
                    onOpenDetails = { detailsTarget = session },
                )
            }
        }
    }

    revokeTarget?.let { target ->
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.profile_sessions_revoke_title),
            description = stringResource(Res.string.profile_sessions_revoke_desc, target.deviceName),
            confirmLabel = stringResource(Res.string.profile_sessions_revoke),
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                viewModel.revoke(target.id)
                revokeTarget = null
            },
            onDismiss = { revokeTarget = null },
        )
    }

    detailsTarget?.let { target ->
        SessionDetailsSheet(session = target, status = target.status(now), onDismiss = { detailsTarget = null })
    }

    if (confirmSignOutAll) {
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.profile_sessions_signout_all_title),
            description = stringResource(Res.string.profile_sessions_signout_all_desc),
            confirmLabel = stringResource(Res.string.profile_sessions_signout_all_confirm),
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                viewModel.revokeAllExceptCurrent()
                confirmSignOutAll = false
            },
            onDismiss = { confirmSignOutAll = false },
        )
    }
}

@Composable
private fun SessionRow(
    session: ActiveSession,
    status: SessionStatus,
    onRevoke: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetails),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(DesignTokens.Shape.button)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                    Text(
                        text = session.deviceName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (session.isCurrent) {
                        Surface(
                            shape = DesignTokens.Shape.chip,
                            color = MilewayColors.success.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = stringResource(Res.string.profile_sessions_this_device),
                                style = MaterialTheme.typography.labelSmall,
                                color = MilewayColors.success,
                                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = session.platform,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
                StatusBadge(status = status)
            }
            if (!session.isCurrent) {
                TextButton(onClick = onRevoke, shape = DesignTokens.Shape.button) {
                    Text(stringResource(Res.string.profile_sessions_revoke), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private val SESSION_MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * PLAN_V24 P7.2: device-details sheet (source: the reference app `SessionDetailsBottomSheet`). Opened by tapping a
 * row; shows the enriched device fields (type/os/app version/ip), last-activity and the derived
 * status. Hidden by default, so it does not alter the list's rendered layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailsSheet(
    session: ActiveSession,
    status: SessionStatus,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                Text(session.deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusBadge(status)
            }
            DetailLine(ss("session_detail_type", "Device type"), session.deviceType.name)
            if (session.os.isNotBlank()) DetailLine(ss("session_detail_os", "Operating system"), session.os)
            if (session.appVersion.isNotBlank()) DetailLine(ss("session_detail_app", "App version"), session.appVersion)
            if (session.ip.isNotBlank()) DetailLine(ss("session_detail_ip", "IP address"), session.ip)
            DetailLine(ss("session_detail_last_active", "Last active"), formatSessionDate(session.lastActiveMillis))
            Spacer(Modifier.height(DesignTokens.Spacing.s))
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatSessionDate(ms: Long): String =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
        "${ldt.dayOfMonth} ${SESSION_MONTHS[ldt.monthNumber - 1]} ${ldt.year}"
    }

@Composable
private fun ss(
    key: String,
    fallback: String,
): String = Res.allStringResources[key]?.let { stringResource(it) } ?: fallback

@Composable
private fun StatusBadge(status: SessionStatus) {
    val (label, color) =
        when (status) {
            SessionStatus.ACTIVE -> stringResource(Res.string.profile_sessions_status_active) to MilewayColors.success
            SessionStatus.RECENT -> stringResource(Res.string.profile_sessions_status_recent) to MilewayColors.warning
            SessionStatus.IDLE -> stringResource(Res.string.profile_sessions_status_idle) to MilewayColors.neutral
        }
    Surface(
        shape = DesignTokens.Shape.chip,
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
        )
    }
}
