package com.miletracker.feature.profile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoneyOff
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.core.ui.theme.DesignTokens.StatusColors

private val BASE_MS = 1_781_654_400_000L
private val MIN_MS = 60_000L
private val H_MS = 3_600_000L
private val DAY_MS = 86_400_000L

private enum class NotifCategory { ALL, UNREAD, APPROVALS, SYSTEM }

private data class NotifItem(
    val id: String,
    val title: String,
    val body: String,
    val relativeTime: String,
    val isUnread: Boolean,
    val category: NotifCategory,
    val icon: ImageVector,
    val iconColor: Color,
)

private val NOTIFICATIONS =
    listOf(
        NotifItem(
            "N001",
            "Approval Required",
            "Priya Sharma's mileage claim needs your review",
            "2 min ago",
            true,
            NotifCategory.APPROVALS,
            Icons.Filled.Approval,
            Color(0xFF6366F1),
        ),
        NotifItem(
            "N002",
            "Advance Approved",
            "Your advance ADV-001 of ₹8,000 was approved",
            "1 hr ago",
            true,
            NotifCategory.ALL,
            Icons.Filled.Receipt,
            StatusColors.success,
        ),
        NotifItem(
            "N003",
            "Expense Rejected",
            "EXP-003 rejected: receipt unclear",
            "3 hrs ago",
            true,
            NotifCategory.ALL,
            Icons.Filled.MoneyOff,
            StatusColors.error,
        ),
        NotifItem(
            "N004",
            "Policy Alert",
            "3 claims exceed the ₹10/km daily cap this week",
            "Yesterday",
            true,
            NotifCategory.ALL,
            Icons.Filled.Policy,
            StatusColors.warning,
        ),
        NotifItem(
            "N005",
            "Card Blocked",
            "CARD-002 was blocked at your request",
            "Yesterday",
            false,
            NotifCategory.ALL,
            Icons.Filled.CreditCard,
            StatusColors.neutral,
        ),
        NotifItem(
            "N006",
            "Payables Update",
            "PO-2024-002 approved by finance",
            "2 days ago",
            false,
            NotifCategory.ALL,
            Icons.Filled.Receipt,
            StatusColors.info,
        ),
        NotifItem(
            "N007",
            "App Update",
            "Version 2.4.1 available: improved GPS accuracy",
            "3 days ago",
            false,
            NotifCategory.SYSTEM,
            Icons.Filled.SystemUpdate,
            Color(0xFF8B5CF6),
        ),
        NotifItem(
            "N008",
            "System",
            "Scheduled maintenance: Sun 02:00–04:00 IST",
            "4 days ago",
            false,
            NotifCategory.SYSTEM,
            Icons.Filled.Info,
            StatusColors.neutral,
        ),
    )

private val FILTER_LABELS = listOf("ALL", "UNREAD", "APPROVALS", "SYSTEM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCentreScreen(onBack: () -> Unit) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var notifications by remember { mutableStateOf(NOTIFICATIONS) }

    val filtered =
        when (selectedFilter) {
            "UNREAD" -> notifications.filter { it.isUnread }
            "APPROVALS" -> notifications.filter { it.category == NotifCategory.APPROVALS }
            "SYSTEM" -> notifications.filter { it.category == NotifCategory.SYSTEM }
            else -> notifications
        }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Notifications",
                subtitle = "174 unread",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        notifications = notifications.map { it.copy(isUnread = false) }
                    }) {
                        Text("Mark all read")
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
                    NotificationCard(notif = notif)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notif: NotifItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        .clip(CircleShape)
                        .background(notif.iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = notif.icon,
                    contentDescription = null,
                    tint = notif.iconColor,
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
                Text(
                    text = notif.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.CenterVertically),
                )
            }
        }
    }
}
