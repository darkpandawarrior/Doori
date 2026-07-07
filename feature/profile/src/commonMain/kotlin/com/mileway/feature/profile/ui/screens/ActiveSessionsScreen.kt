package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var confirmSignOutAll by remember { mutableStateOf(false) }

    val hasOtherSessions = uiState.sessions.any { !it.isCurrent }

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_sessions_title),
                subtitle = "${uiState.sessions.size} device${if (uiState.sessions.size == 1) "" else "s"} signed in",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_sessions_back))
                    }
                },
                actions = {
                    if (hasOtherSessions) {
                        TextButton(onClick = { confirmSignOutAll = true }) {
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        .clip(CircleShape)
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
                TextButton(onClick = onRevoke) {
                    Text(stringResource(Res.string.profile_sessions_revoke), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

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
