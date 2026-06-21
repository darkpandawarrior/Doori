package com.miletracker.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.miletracker.core.ui.components.GridProfileTile
import com.miletracker.core.ui.components.ProfileGridItem
import com.miletracker.core.ui.components.ProfileItemStatus
import com.miletracker.core.ui.components.ProfileSectionHeader
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.feature.profile.viewmodel.ProfileAction
import com.miletracker.feature.profile.viewmodel.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Preferences — a focused service/system preferences screen pushed from the Account hub.
 *
 * A 2-column grid of large tonal tiles under a single "Settings" section header. The two
 * stateful tiles (Push Notifications, Usage Analytics) flip a ViewModel-held toggle and update
 * their "Enabled / Disabled" subtitle; the remaining tiles raise a one-shot demo snackbar (in
 * the real app they deep-link into system screens or bottom sheets).
 *
 * Full-screen flow (no bubble bar): the grid's content insets carry the bottom padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface any one-shot preference demo message as a snackbar, then clear it.
    LaunchedEffect(state.preferenceMessage) {
        state.preferenceMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(ProfileAction.ClearPreferenceMessage)
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Preferences",
                subtitle = "Manage your settings",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val prefs = state.preferences
        val items =
            listOf(
                ProfileGridItem(
                    id = "push_notifications",
                    title = "Push Notifications",
                    subtitle = if (prefs.pushNotifications) "Enabled" else "Disabled",
                    icon = Icons.Default.Notifications,
                    status = if (prefs.pushNotifications) ProfileItemStatus.COMPLETE else ProfileItemStatus.INCOMPLETE,
                    action = { viewModel.onAction(ProfileAction.TogglePushNotifications) },
                ),
                ProfileGridItem(
                    id = "usage_analytics",
                    title = "Usage Analytics",
                    subtitle = if (prefs.usageAnalytics) "Enabled" else "Disabled",
                    icon = Icons.Default.BarChart,
                    status = if (prefs.usageAnalytics) ProfileItemStatus.COMPLETE else ProfileItemStatus.INCOMPLETE,
                    action = { viewModel.onAction(ProfileAction.ToggleUsageAnalytics) },
                ),
                ProfileGridItem(
                    id = "notification_center",
                    title = "Notification Center",
                    subtitle = "WhatsApp, Slack",
                    icon = Icons.Default.NotificationsActive,
                    status = ProfileItemStatus.COMPLETE,
                    action = { viewModel.onAction(ProfileAction.RaisePreferenceMessage("Notification Center is a demo placeholder.")) },
                ),
                ProfileGridItem(
                    id = "connected_accounts",
                    title = "Connected Accounts",
                    subtitle = "Cabs, Passport",
                    icon = Icons.Default.Link,
                    status = ProfileItemStatus.COMPLETE,
                    action = { viewModel.onAction(ProfileAction.RaisePreferenceMessage("Connected Accounts is a demo placeholder.")) },
                ),
                ProfileGridItem(
                    id = "permissions",
                    title = "Permissions",
                    subtitle = "System settings",
                    icon = Icons.Default.Security,
                    status = ProfileItemStatus.COMPLETE,
                    action = { viewModel.onAction(ProfileAction.RaisePreferenceMessage("Opens system settings in the full app.")) },
                ),
                ProfileGridItem(
                    id = "storage",
                    title = "Storage",
                    subtitle = "Manage data",
                    icon = Icons.Default.Storage,
                    status = ProfileItemStatus.COMPLETE,
                    action = { viewModel.onAction(ProfileAction.RaisePreferenceMessage("Manage local data in the full app.")) },
                ),
            )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(DesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            item(span = { GridItemSpan(2) }) {
                ProfileSectionHeader(
                    title = "Settings",
                    itemCount = items.size,
                    icon = Icons.Default.Settings,
                )
            }
            items(items, key = { it.id }) { item ->
                GridProfileTile(item = item)
            }
        }
    }
}
