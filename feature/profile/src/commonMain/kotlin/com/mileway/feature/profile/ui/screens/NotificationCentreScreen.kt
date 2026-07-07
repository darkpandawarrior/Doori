package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.ExpandableText
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_notifications_back
import com.mileway.core.ui.resources.profile_notifications_mark_all_read
import com.mileway.core.ui.resources.profile_notifications_title
import com.mileway.core.ui.resources.profile_notifications_unread_count
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.DesignTokens.StatusColors
import com.mileway.feature.profile.data.NotifType
import com.mileway.feature.profile.data.NotificationRecord
import com.mileway.feature.profile.viewmodel.NotificationViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class NotifCategory { ALL, UNREAD, APPROVALS, SYSTEM }

/** Icon + tint per [NotifType], matching the previous per-item hardcoded styling. */
private fun NotifType.icon(): ImageVector =
    when (this) {
        NotifType.APPROVAL -> Icons.Filled.Approval
        NotifType.ADVANCE -> Icons.Filled.Receipt
        NotifType.EXPENSE -> Icons.Filled.MoneyOff
        NotifType.POLICY -> Icons.Filled.Policy
        NotifType.CARD -> Icons.Filled.CreditCard
        NotifType.PAYABLES -> Icons.Filled.Receipt
        NotifType.APP_UPDATE -> Icons.Filled.SystemUpdate
        NotifType.SYSTEM -> Icons.Filled.Info
    }

private fun NotifType.iconColor(): Color =
    when (this) {
        NotifType.APPROVAL -> Color(0xFF6366F1)
        NotifType.ADVANCE -> StatusColors.success
        NotifType.EXPENSE -> StatusColors.error
        NotifType.POLICY -> StatusColors.warning
        NotifType.CARD -> StatusColors.neutral
        NotifType.PAYABLES -> StatusColors.info
        NotifType.APP_UPDATE -> Color(0xFF8B5CF6)
        NotifType.SYSTEM -> StatusColors.neutral
    }

private fun NotifType.category(): NotifCategory =
    when (this) {
        NotifType.APPROVAL -> NotifCategory.APPROVALS
        NotifType.SYSTEM, NotifType.APP_UPDATE -> NotifCategory.SYSTEM
        else -> NotifCategory.ALL
    }

private val FILTER_LABELS = listOf("ALL", "UNREAD", "APPROVALS", "SYSTEM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCentreScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = koinViewModel(),
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val state by viewModel.state.collectAsState()

    val filtered =
        when (selectedFilter) {
            "UNREAD" -> state.notifications.filter { it.isUnread }
            "APPROVALS" -> state.notifications.filter { it.type.category() == NotifCategory.APPROVALS }
            "SYSTEM" -> state.notifications.filter { it.type.category() == NotifCategory.SYSTEM }
            else -> state.notifications
        }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_notifications_title),
                subtitle = stringResource(Res.string.profile_notifications_unread_count, state.unreadCount),
                depth = NavigationDepth.LEVEL_1,
                titleIcon = Icons.Filled.Notifications,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_notifications_back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllRead() }, shape = DesignTokens.Shape.button) {
                        Text(stringResource(Res.string.profile_notifications_mark_all_read))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            LazyRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                items(FILTER_LABELS) { label ->
                    FilterChip(
                        selected = selectedFilter == label,
                        onClick = { selectedFilter = label },
                        label = { Text(label) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = DesignTokens.Spacing.l,
                        vertical = DesignTokens.Spacing.s,
                    ),
            ) {
                items(filtered, key = { it.id }) { notif ->
                    NotificationCard(
                        notif = notif,
                        onToggleRead = { viewModel.setUnread(notif.id, !notif.isUnread) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notif: NotificationRecord,
    onToggleRead: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleRead),
        shape = DesignTokens.Shape.roundedMd,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (notif.isUnread) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(DesignTokens.Shape.button)
                        .background(notif.type.iconColor().copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = notif.type.icon(),
                    contentDescription = null,
                    tint = notif.type.iconColor(),
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notif.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (notif.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(2.dp))
                ExpandableText(
                    text = notif.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    collapsedMaxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notif.relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            if (notif.isUnread) {
                Spacer(Modifier.width(DesignTokens.Spacing.xs))
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(DesignTokens.Shape.button)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.CenterVertically),
                )
            }
        }
    }
}
