package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_accounts_connected
import com.mileway.core.ui.resources.profile_accounts_disconnected
import com.mileway.core.ui.resources.profile_accounts_subtitle
import com.mileway.core.ui.resources.profile_accounts_title
import com.mileway.core.ui.resources.profile_sessions_back
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.profile.model.ConnectedAccount
import com.mileway.feature.profile.viewmodel.ConnectedAccountsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PLAN_V22 P6.6: Preferences' "Connected Accounts" tile pushed to a real destination — a
 * connect/disconnect list of mock cab/passport-style integrations, backed by
 * [ConnectedAccountsViewModel]/Room, replacing the tile's previous
 * `RaisePreferenceMessage("Connected Accounts is a demo placeholder.")` snackbar tap. Own
 * Matrix/terminal card-and-switch layout — not a port of any reference app screen's visual design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedAccountsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectedAccountsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_accounts_title),
                subtitle = stringResource(Res.string.profile_accounts_subtitle),
                depth = NavigationDepth.LEVEL_2,
                titleIcon = Icons.Filled.Link,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_sessions_back))
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
            items(state.accounts, key = { it.id }) { account ->
                ConnectedAccountRow(
                    account = account,
                    onToggle = { connected -> viewModel.toggle(account.id, connected) },
                )
            }
        }
    }
}

@Composable
private fun ConnectedAccountRow(
    account: ConnectedAccount,
    onToggle: (Boolean) -> Unit,
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
                        .clip(DesignTokens.Shape.button)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.providerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = account.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = DesignTokens.Shape.chip,
                    color = (if (account.isConnected) MilewayColors.success else MilewayColors.neutral).copy(alpha = 0.15f),
                ) {
                    Text(
                        text =
                            if (account.isConnected) {
                                stringResource(
                                    Res.string.profile_accounts_connected,
                                )
                            } else {
                                stringResource(Res.string.profile_accounts_disconnected)
                            },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (account.isConnected) MilewayColors.success else MilewayColors.neutral,
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
                    )
                }
            }
            Switch(checked = account.isConnected, onCheckedChange = onToggle)
        }
    }
}
