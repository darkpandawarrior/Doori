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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_accounts_connected
import com.mileway.core.ui.resources.profile_accounts_disconnected
import com.mileway.core.ui.resources.profile_accounts_subtitle
import com.mileway.core.ui.resources.profile_accounts_title
import com.mileway.core.ui.resources.profile_sessions_back
import com.mileway.core.ui.resources.profile_wallet_balance
import com.mileway.core.ui.resources.profile_wallet_link
import com.mileway.core.ui.resources.profile_wallet_linked
import com.mileway.core.ui.resources.profile_wallet_otp_cancel
import com.mileway.core.ui.resources.profile_wallet_otp_demo
import com.mileway.core.ui.resources.profile_wallet_otp_sent
import com.mileway.core.ui.resources.profile_wallet_otp_title
import com.mileway.core.ui.resources.profile_wallet_otp_wrong
import com.mileway.core.ui.resources.profile_wallet_section_title
import com.mileway.core.ui.resources.profile_wallet_unlink
import com.mileway.core.ui.resources.profile_wallet_unlink_confirm
import com.mileway.core.ui.resources.profile_wallet_unlink_desc
import com.mileway.core.ui.resources.profile_wallet_unlink_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.feature.profile.model.ConnectedAccount
import com.mileway.feature.profile.model.PaymentWallet
import com.mileway.feature.profile.viewmodel.ConnectedAccountsViewModel
import com.mileway.feature.profile.viewmodel.WalletLinkFlow
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
    var unlinkTarget by remember { mutableStateOf<PaymentWallet?>(null) }

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

            // PLAN_V24 P8.1: external payment wallets — gated on walletLinkingEnabled (off by default).
            if (state.walletsEnabled) {
                item {
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    Text(
                        text = stringResource(Res.string.profile_wallet_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.wallets, key = { it.id }) { wallet ->
                    WalletRow(
                        wallet = wallet,
                        onLink = { viewModel.startLink(wallet) },
                        onUnlink = { unlinkTarget = wallet },
                    )
                }
            }
        }
    }

    state.linkFlow?.let { flow ->
        WalletOtpSheet(
            flow = flow,
            onCodeChange = viewModel::onLinkCodeChange,
            onCancel = viewModel::cancelLink,
        )
    }

    unlinkTarget?.let { target ->
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.profile_wallet_unlink_title, target.providerName),
            description = stringResource(Res.string.profile_wallet_unlink_desc),
            confirmLabel = stringResource(Res.string.profile_wallet_unlink_confirm),
            tone = ActionConfirmationToneType.Danger,
            onConfirm = {
                viewModel.unlink(target.id)
                unlinkTarget = null
            },
            onDismiss = { unlinkTarget = null },
        )
    }
}

@Composable
private fun WalletRow(
    wallet: PaymentWallet,
    onLink: () -> Unit,
    onUnlink: () -> Unit,
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
                        .background(MilewayColors.success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MilewayColors.success,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wallet.providerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text =
                        if (wallet.isLinked) {
                            stringResource(Res.string.profile_wallet_balance, wallet.balanceLabel)
                        } else {
                            wallet.mobile
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (wallet.isLinked) {
                    Surface(
                        shape = DesignTokens.Shape.chip,
                        color = MilewayColors.success.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = stringResource(Res.string.profile_wallet_linked),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MilewayColors.success,
                            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
                        )
                    }
                }
            }
            if (wallet.isLinked) {
                OutlinedButton(onClick = onUnlink, shape = DesignTokens.Shape.button) {
                    Text(stringResource(Res.string.profile_wallet_unlink))
                }
            } else {
                Button(onClick = onLink, shape = DesignTokens.Shape.button) {
                    Text(stringResource(Res.string.profile_wallet_link))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletOtpSheet(
    flow: WalletLinkFlow,
    onCodeChange: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                stringResource(Res.string.profile_wallet_otp_title, flow.providerName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(Res.string.profile_wallet_otp_sent, flow.target),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.profile_wallet_otp_demo, flow.demoCode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedTextField(
                value = flow.code,
                onValueChange = onCodeChange,
                singleLine = true,
                isError = flow.wrongCode,
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            if (flow.wrongCode) {
                Text(
                    stringResource(Res.string.profile_wallet_otp_wrong),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(
                onClick = onCancel,
                shape = DesignTokens.Shape.button,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.profile_wallet_otp_cancel))
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
