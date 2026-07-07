package com.mileway.feature.profile.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.network.model.DemoAccount
import com.mileway.core.platform.ReferralManager
import com.mileway.core.security.BiometricGuard
import com.mileway.core.ui.components.GridProfileTile
import com.mileway.core.ui.components.ProfileGridItem
import com.mileway.core.ui.components.ProfileItemStatus
import com.mileway.core.ui.components.ReferralCard
import com.mileway.core.ui.components.buildReferralInvite
import com.mileway.core.ui.platform.LocalShareSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_home_active
import com.mileway.core.ui.resources.profile_home_add
import com.mileway.core.ui.resources.profile_home_add_persona
import com.mileway.core.ui.resources.profile_home_analytics
import com.mileway.core.ui.resources.profile_home_analytics_live
import com.mileway.core.ui.resources.profile_home_cancel
import com.mileway.core.ui.resources.profile_home_deep_link_demo
import com.mileway.core.ui.resources.profile_home_deep_link_desc
import com.mileway.core.ui.resources.profile_home_details
import com.mileway.core.ui.resources.profile_home_edit_photo
import com.mileway.core.ui.resources.profile_home_field_display_name
import com.mileway.core.ui.resources.profile_home_field_employee_code
import com.mileway.core.ui.resources.profile_home_field_organization
import com.mileway.core.ui.resources.profile_home_live_insights
import com.mileway.core.ui.resources.profile_home_remove
import com.mileway.core.ui.resources.profile_home_sign_out
import com.mileway.core.ui.resources.profile_home_switch
import com.mileway.core.ui.resources.profile_home_switch_persona
import com.mileway.core.ui.resources.profile_home_this_persona
import com.mileway.core.ui.resources.profile_home_tile_about_subtitle
import com.mileway.core.ui.resources.profile_home_tile_about_title
import com.mileway.core.ui.resources.profile_home_tile_advance_subtitle
import com.mileway.core.ui.resources.profile_home_tile_advance_title
import com.mileway.core.ui.resources.profile_home_tile_cards_subtitle
import com.mileway.core.ui.resources.profile_home_tile_cards_title
import com.mileway.core.ui.resources.profile_home_tile_delegation_subtitle
import com.mileway.core.ui.resources.profile_home_tile_delegation_title
import com.mileway.core.ui.resources.profile_home_tile_demo_subtitle
import com.mileway.core.ui.resources.profile_home_tile_demo_title
import com.mileway.core.ui.resources.profile_home_tile_details_subtitle
import com.mileway.core.ui.resources.profile_home_tile_insights_subtitle
import com.mileway.core.ui.resources.profile_home_tile_insights_title
import com.mileway.core.ui.resources.profile_home_tile_notifications_subtitle
import com.mileway.core.ui.resources.profile_home_tile_notifications_title
import com.mileway.core.ui.resources.profile_home_tile_preferences_subtitle
import com.mileway.core.ui.resources.profile_home_tile_preferences_title
import com.mileway.core.ui.resources.profile_home_tile_qr_subtitle
import com.mileway.core.ui.resources.profile_home_tile_qr_title
import com.mileway.core.ui.resources.profile_home_tile_sessions_subtitle
import com.mileway.core.ui.resources.profile_home_tile_sessions_title
import com.mileway.core.ui.resources.profile_home_tile_settings_subtitle
import com.mileway.core.ui.resources.profile_home_tile_settings_title
import com.mileway.core.ui.resources.profile_home_total_spend
import com.mileway.core.ui.resources.profile_home_transactions
import com.mileway.core.ui.resources.profile_home_updated_at
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.profile.model.AccountAnalyticsSnapshot
import com.mileway.feature.profile.model.AccountBadge
import com.mileway.feature.profile.model.ProfileHeader
import com.mileway.feature.profile.viewmodel.ProfileAction
import com.mileway.feature.profile.viewmodel.ProfileEffect
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import com.mileway.feature.profile.viewmodel.SwitchAccountViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Account hub (top-level tab). A soft gradient header with a 96dp avatar, the user's
 * identity chips and a static employee-code field, followed by a 2-row colored tile grid that
 * routes into the detail surfaces, and an analytics snapshot card with a tiny sparkline.
 *
 * Stateless against the ViewModel: it observes [ProfileViewModel.uiState] and raises intents.
 * As a top-level tab it floats above the ~100dp bubble bottom bar, so the scrolling content is
 * given >=140dp of bottom padding.
 *
 * @param onOpenDetails navigate to the Profile Details screen
 * @param onOpenPreferences navigate to the Preferences screen
 * @param onOpenNotifications navigate to the notifications surface (reuses the Help route here)
 * @param onOpenSettings navigate to the existing Settings route
 * @param onOpenAboutSupport navigate to the About & Support surface (reuses the Help route here)
 * @param onOpenSessions P6.4: navigate to the full [ActiveSessionsScreen][com.mileway.feature.profile.ui.screens.ActiveSessionsScreen] —
 * promoted from the old read-only `SessionsDialog`, which this screen no longer shows.
 * @param onSignedOut P2.4: the last persona was just signed out of; the app shell should transition
 * back to the login screen (`LauncherActivity`'s `AppStage`), since there is no persona left to show.
 */
@Composable
fun ProfileScreen(
    onOpenDetails: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAboutSupport: () -> Unit,
    onOpenAdvance: () -> Unit = {},
    onOpenCards: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenDelegation: () -> Unit = {},
    onOpenDemoSettings: () -> Unit = {},
    onOpenQr: () -> Unit = {},
    onOpenSessions: () -> Unit = {},
    onSignedOut: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    switchAccountViewModel: SwitchAccountViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // P1.3: surfaces the RemoveDemoAccount guard-rejection message (active/last-account block).
    LaunchedEffect(state.preferenceMessage) {
        state.preferenceMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(ProfileAction.ClearPreferenceMessage)
        }
    }

    // P2.3: biometric-gate path — BiometricPrompt needs a FragmentActivity, which only this
    // Android-only screen (not the commonMain SwitchAccountViewModel) can reach.
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.RequestBiometricGate -> {
                    val activity = context as? FragmentActivity
                    if (activity != null && BiometricGuard.checkAvailability(context) == BiometricGuard.Availability.Available) {
                        BiometricGuard.showPrompt(
                            activity = activity,
                            title = "Confirm it's you",
                            subtitle = "Switching persona",
                            onSuccess = { viewModel.onAction(ProfileAction.CommitAccountSwitch(effect.accountId)) },
                            onFailure = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                        )
                    } else {
                        // No usable biometric hardware/enrolment: fall back to the PIN gate rather
                        // than silently committing the switch with no confirmation at all.
                        switchAccountViewModel.reset()
                        viewModel.onAction(ProfileAction.FallBackToPinGate(effect.accountId))
                    }
                }

                ProfileEffect.NavigateToLogin -> onSignedOut()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // P1.4: persistent active-account badge, scoped to this tab's own shell row (this
            // screen has no hosting Scaffold/TopAppBar — it IS the tab content) — sits outside the
            // LazyColumn so it never scrolls away, unlike the PersonaSwitcherRow chip below.
            ActiveAccountBadgeRow(
                activeAccount = state.accounts.find { it.id == state.selectedAccountId },
            )

            LazyColumn(
                // Top-level tab: own the background so the body renders on the theme surface
                // (deep-dark under the Matrix default) rather than falling through to the
                // window/canvas colour when no host Scaffold paints behind it.
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                // Clearance for the floating bubble bar (content draws behind it).
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                item {
                    ProfileHeaderSection(
                        header = state.header,
                        role = state.profile.role,
                        gender = state.profile.gender,
                    )
                }

                item {
                    PersonaSwitcherRow(
                        personas = state.accounts,
                        selectedId = state.selectedAccountId,
                        onSelect = { viewModel.onAction(ProfileAction.SwitchAccount(it)) },
                        onViewDetails = { viewModel.onAction(ProfileAction.ViewAccountDetails(it)) },
                        onRemove = { viewModel.onAction(ProfileAction.RemoveDemoAccount(it)) },
                        onSignOut = { viewModel.onAction(ProfileAction.SignOut(it)) },
                        onAdd = { name, code, org -> viewModel.onAction(ProfileAction.AddDemoAccount(name, code, org)) },
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                    )
                }

                item {
                    DeepLinkDemoCard(
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                    )
                }

                item {
                    ReferralCardHost(
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                    )
                }

                item {
                    AccountTileGrid(
                        notificationBadge = NOTIFICATION_BADGE,
                        onOpenDetails = onOpenDetails,
                        onOpenNotifications = onOpenNotifications,
                        onOpenSettings = onOpenSettings,
                        onOpenPreferences = onOpenPreferences,
                        onOpenSessions = onOpenSessions,
                        onOpenAboutSupport = onOpenAboutSupport,
                        onOpenAdvance = onOpenAdvance,
                        onOpenCards = onOpenCards,
                        onOpenDelegation = onOpenDelegation,
                        onOpenDemoSettings = onOpenDemoSettings,
                        onOpenQr = onOpenQr,
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                    )
                }

                item {
                    AnalyticsCard(
                        analytics = state.analytics,
                        onClick = onOpenInsights,
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
    }

    state.accountDetailsSheet?.let { account ->
        AccountDetailsSheet(
            account = account,
            onDismiss = { viewModel.onAction(ProfileAction.DismissAccountDetails) },
        )
    }

    state.pendingSwitchAccountId?.let { pendingId ->
        val pinState by switchAccountViewModel.state.collectAsStateWithLifecycle()
        val label = state.accounts.find { it.id == pendingId }?.displayName ?: stringResource(Res.string.profile_home_this_persona)

        LaunchedEffect(pendingId) { switchAccountViewModel.reset() }
        LaunchedEffect(pinState.verified) {
            if (pinState.verified) viewModel.onAction(ProfileAction.CommitAccountSwitch(pendingId))
        }

        SwitchAccountPinSheet(
            accountLabel = label,
            state = pinState,
            onDigit = switchAccountViewModel::onDigitEntered,
            onBackspace = switchAccountViewModel::onBackspace,
            onConfirm = { switchAccountViewModel.verify(pendingId) },
            onDismiss = { viewModel.onAction(ProfileAction.CancelAccountSwitch) },
        )
    }

    if (state.pausedTripNotice != null) {
        PausedTripNoticeSheet(onDismiss = { viewModel.onAction(ProfileAction.DismissPausedTripNotice) })
    }

    if (state.showReconfirmIdentity) {
        ReconfirmIdentitySheet(onDismiss = { viewModel.onAction(ProfileAction.DismissReconfirmIdentity) })
    }
}

/** Badge count shown on the Notifications tile (matches the reference "174"). */
private const val NOTIFICATION_BADGE = 174

private val AvatarSize = 96.dp

/**
 * P1.4: persistent active-account indicator for this tab — a small initials-in-a-ring badge plus
 * the active persona's display name, updating live off [ProfileUiState.selectedAccountId]. Sits
 * above the scrolling content (this screen has no host Scaffold/TopAppBar of its own), so it never
 * needs a scroll to be visible, unlike the [PersonaSwitcherRow] chip row further down.
 */
@Composable
private fun ActiveAccountBadgeRow(activeAccount: DemoAccount?) {
    if (activeAccount == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    horizontal = DesignTokens.Spacing.screenHorizontal,
                    vertical = DesignTokens.Spacing.s,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(DesignTokens.Shape.button)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), DesignTokens.Shape.button),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = AccountBadge.initialsFor(activeAccount.displayName),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = activeAccount.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileHeaderSection(
    header: ProfileHeader,
    role: String,
    gender: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = DesignTokens.Shape.sheetSquared,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Soft gradient wash behind the avatar, fading into the surface.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(AvatarSize / 2)
                        .background(DesignTokens.topBarGradientBrush()),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = DesignTokens.Spacing.screenHorizontal,
                            vertical = DesignTokens.Spacing.l,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                // Avatar + edit pencil badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier =
                            Modifier
                                .size(AvatarSize)
                                .clip(DesignTokens.Shape.button)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = DesignTokens.Shape.button,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = header.initials,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .size(30.dp)
                                .clip(DesignTokens.Shape.button)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, DesignTokens.Shape.button),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.profile_home_edit_photo),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(DesignTokens.IconSize.inline),
                        )
                    }
                }

                // Name + rupee chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    Text(
                        text = header.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clip(DesignTokens.Shape.button)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "₹",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Role + gender chips
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                    if (role.isNotBlank()) IdentityChip(text = role)
                    if (gender.isNotBlank()) IdentityChip(text = gender)
                }

                // Static employee-code dropdown-looking field
                EmployeeCodeField(code = header.code)
            }
        }
    }
}

@Composable
private fun IdentityChip(text: String) {
    Surface(
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.l,
                    vertical = DesignTokens.Spacing.s,
                ),
        )
    }
}

/** A read-only field styled like a dropdown (chevron, no action), echoing the reference. */
@Composable
private fun EmployeeCodeField(code: String) {
    Surface(
        shape = DesignTokens.Shape.roundedSm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = code.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The 2-row colored tile grid. Each tile carries a distinct hue so the hub reads as a colorful
 * launcher (Details blue, Notifications red, Settings green, Preferences orange,
 * Active Sessions purple, About & Support violet).
 */
@Composable
private fun AccountTileGrid(
    notificationBadge: Int,
    onOpenDetails: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenSessions: () -> Unit,
    onOpenAboutSupport: () -> Unit,
    onOpenAdvance: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenDelegation: () -> Unit,
    onOpenDemoSettings: () -> Unit,
    onOpenQr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val blue = Color(0xFF2563EB)
    val red = Color(0xFFDC2626)
    val green = Color(0xFF16A34A)
    val orange = Color(0xFFEA580C)
    val purple = Color(0xFF7C3AED)
    val violet = Color(0xFF6D28D9)
    val teal = Color(0xFF0F766E)
    val indigo = Color(0xFF3730A3)
    val cyan = Color(0xFF0277BD)
    val darkTeal = Color(0xFF00695C)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        TileRow(
            left =
                accountTile(
                    "acc_details",
                    stringResource(Res.string.profile_home_details),
                    stringResource(Res.string.profile_home_tile_details_subtitle),
                    Icons.Default.Person,
                    blue,
                    onOpenDetails,
                ),
            right =
                accountTile(
                    "acc_notifications",
                    stringResource(Res.string.profile_home_tile_notifications_title),
                    stringResource(Res.string.profile_home_tile_notifications_subtitle),
                    Icons.Default.Notifications,
                    red,
                    onOpenNotifications,
                    badgeCount = notificationBadge,
                ),
        )
        TileRow(
            left =
                accountTile(
                    "acc_settings",
                    stringResource(Res.string.profile_home_tile_settings_title),
                    stringResource(Res.string.profile_home_tile_settings_subtitle),
                    Icons.Default.Settings,
                    green,
                    onOpenSettings,
                ),
            right =
                accountTile(
                    "acc_preferences",
                    stringResource(Res.string.profile_home_tile_preferences_title),
                    stringResource(Res.string.profile_home_tile_preferences_subtitle),
                    Icons.Default.Tune,
                    orange,
                    onOpenPreferences,
                ),
        )
        TileRow(
            left =
                accountTile(
                    "acc_sessions",
                    stringResource(Res.string.profile_home_tile_sessions_title),
                    stringResource(Res.string.profile_home_tile_sessions_subtitle),
                    Icons.Default.Devices,
                    purple,
                    onOpenSessions,
                ),
            right =
                accountTile(
                    "acc_about",
                    stringResource(Res.string.profile_home_tile_about_title),
                    stringResource(Res.string.profile_home_tile_about_subtitle),
                    Icons.AutoMirrored.Filled.HelpOutline,
                    violet,
                    onOpenAboutSupport,
                ),
        )
        TileRow(
            left =
                accountTile(
                    "acc_advance",
                    stringResource(Res.string.profile_home_tile_advance_title),
                    stringResource(Res.string.profile_home_tile_advance_subtitle),
                    Icons.Default.MonetizationOn,
                    teal,
                    onOpenAdvance,
                ),
            right =
                accountTile(
                    "acc_cards",
                    stringResource(Res.string.profile_home_tile_cards_title),
                    stringResource(Res.string.profile_home_tile_cards_subtitle),
                    Icons.Default.CreditCard,
                    indigo,
                    onOpenCards,
                ),
        )
        TileRow(
            left =
                accountTile(
                    "acc_delegation",
                    stringResource(Res.string.profile_home_tile_delegation_title),
                    stringResource(Res.string.profile_home_tile_delegation_subtitle),
                    Icons.Default.SupervisorAccount,
                    cyan,
                    onOpenDelegation,
                ),
            right =
                accountTile(
                    "acc_insights",
                    stringResource(Res.string.profile_home_tile_insights_title),
                    stringResource(Res.string.profile_home_tile_insights_subtitle),
                    Icons.Default.History,
                    Color(0xFF6D28D9),
                    action = {},
                ),
        )
        TileRow(
            left =
                accountTile(
                    "acc_demo",
                    stringResource(Res.string.profile_home_tile_demo_title),
                    stringResource(Res.string.profile_home_tile_demo_subtitle),
                    Icons.Default.BugReport,
                    darkTeal,
                    onOpenDemoSettings,
                ),
            right =
                accountTile(
                    "acc_qr",
                    stringResource(Res.string.profile_home_tile_qr_title),
                    stringResource(Res.string.profile_home_tile_qr_subtitle),
                    Icons.Default.QrCode2,
                    Color(0xFF0F4C81),
                    onOpenQr,
                ),
        )
    }
}

@Composable
private fun TileRow(
    left: ProfileGridItem,
    right: ProfileGridItem,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        GridProfileTile(item = left, modifier = Modifier.weight(1f), compact = true)
        GridProfileTile(item = right, modifier = Modifier.weight(1f), compact = true)
    }
}

private fun accountTile(
    id: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    action: () -> Unit,
    badgeCount: Int? = null,
): ProfileGridItem =
    ProfileGridItem(
        id = id,
        title = title,
        subtitle = subtitle,
        icon = icon,
        status = ProfileItemStatus.COMPLETE,
        badgeCount = badgeCount,
        customContainerColor = accent.copy(alpha = 0.10f),
        customContentColor = accent,
        action = action,
    )

/**
 * "Analytics / Live · last 7 days" snapshot card: total spend, transaction count, and a tiny
 * Canvas sparkline drawn from [AccountAnalyticsSnapshot.sparkline].
 */
@Composable
private fun AnalyticsCard(
    analytics: AccountAnalyticsSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = DesignTokens.Shape.roundedLg,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DesignTokens.Elevation.card,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Surface(
                        shape = DesignTokens.Shape.button,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.profile_home_analytics),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(Res.string.profile_home_analytics_live, analytics.window),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(DesignTokens.Shape.button)
                                .background(DesignTokens.StatusColors.success),
                    )
                    Spacer(Modifier.width(DesignTokens.Spacing.xs))
                    Text(
                        text = stringResource(Res.string.profile_home_live_insights),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.profile_home_total_spend),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = analytics.totalSpend,
                        style = MaterialTheme.typography.headlineSmall.dataStyle(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Sparkline(
                    points = analytics.sparkline,
                    modifier =
                        Modifier
                            .padding(horizontal = DesignTokens.Spacing.m)
                            .weight(1f)
                            .height(40.dp),
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(Res.string.profile_home_transactions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = analytics.transactions.toString(),
                        style = MaterialTheme.typography.headlineSmall.dataStyle(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = stringResource(Res.string.profile_home_updated_at, analytics.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A minimal trend line drawn from normalised (0f..1f) [points]. */
@Composable
private fun Sparkline(
    points: List<Float>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val maxX = (points.size - 1).toFloat()
        val stepX = size.width / maxX
        val mapped =
            points.mapIndexed { index, value ->
                val clamped = value.coerceIn(0f, 1f)
                Offset(x = index * stepX, y = size.height * (1f - clamped))
            }
        for (i in 0 until mapped.size - 1) {
            drawLine(
                color = lineColor,
                start = mapped[i],
                end = mapped[i + 1],
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
        // Emphasise the latest point.
        drawCircle(
            color = lineColor,
            radius = 4f,
            center = mapped.last(),
            style = Stroke(width = 2f),
        )
    }
}

/**
 * Demo card that shows `mileway://` deep link buttons. Each button fires a VIEW intent
 * so the system routes back into the app with the deep link, exercising intent parsing.
 */
@Composable
private fun DeepLinkDemoCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val links =
        listOf(
            "home" to "mileway://home",
            "log" to "mileway://log",
            "track" to "mileway://track",
            "profile" to "mileway://profile",
        )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedSm,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(
                text = stringResource(Res.string.profile_home_deep_link_demo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.profile_home_deep_link_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                links.forEach { (label, url) ->
                    OutlinedButton(
                        shape = DesignTokens.Shape.button,
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            )
                        },
                        modifier = Modifier.widthIn(min = 72.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal scrolling row of persona chips. Tapping a chip switches the active account;
 * long-pressing opens an overflow menu (Details / Remove); a trailing "+" tile opens an
 * inline add-persona form. P1.3: closes the "no CRUD affordances" gap — Mileway's own design
 * (overflow menu + trailing tile), not a port of the reference app's `AccountCard` gradient-ring UI.
 */
@Composable
private fun PersonaSwitcherRow(
    personas: List<DemoAccount>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onViewDetails: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSignOut: (String) -> Unit,
    onAdd: (displayName: String, employeeCode: String, organization: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.profile_home_switch_persona),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            contentPadding = PaddingValues(vertical = DesignTokens.Spacing.xs),
        ) {
            items(personas, key = { it.id }) { persona ->
                PersonaChip(
                    persona = persona,
                    isSelected = persona.id == selectedId,
                    onSelect = { onSelect(persona.id) },
                    onViewDetails = { onViewDetails(persona.id) },
                    onRemove = { onRemove(persona.id) },
                    onSignOut = { onSignOut(persona.id) },
                )
            }
            item(key = "add_persona") {
                AddPersonaTile(onClick = { showAddDialog = true })
            }
        }
    }

    if (showAddDialog) {
        AddPersonaSheet(
            onDismiss = { showAddDialog = false },
            onAdd = { name, code, org ->
                onAdd(name, code, org)
                showAddDialog = false
            },
        )
    }
}

/** A single switchable-persona chip; long-press opens the Details/Remove/Sign Out overflow menu. */
@Composable
private fun PersonaChip(
    persona: DemoAccount,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onViewDetails: () -> Unit,
    onRemove: () -> Unit,
    onSignOut: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Card(
            shape = DesignTokens.Shape.button,
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                ),
            modifier =
                Modifier
                    .width(160.dp)
                    .combinedClickable(onClick = onSelect, onLongClick = { showMenu = true }),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(DesignTokens.Shape.button)
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = if (isSelected) stringResource(Res.string.profile_home_active) else stringResource(Res.string.profile_home_switch),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                Text(
                    text = persona.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = persona.organization.ifEmpty { persona.employeeCode },
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        (
                            if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ).copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.profile_home_details)) },
                onClick = {
                    showMenu = false
                    onViewDetails()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.profile_home_remove)) },
                onClick = {
                    showMenu = false
                    onRemove()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.profile_home_sign_out)) },
                onClick = {
                    showMenu = false
                    onSignOut()
                },
            )
        }
    }
}

/** Trailing "+" tile in the persona row; opens [AddPersonaSheet]. */
@Composable
private fun AddPersonaTile(onClick: () -> Unit) {
    Card(
        shape = DesignTokens.Shape.button,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier =
            Modifier
                .width(88.dp)
                .height(IntrinsicPersonaChipHeight)
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.profile_home_add_persona),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.profile_home_add),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val IntrinsicPersonaChipHeight = 92.dp

/** Inline add-persona form (name, employee code, organization) opened by [AddPersonaTile]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPersonaSheet(
    onDismiss: () -> Unit,
    onAdd: (displayName: String, employeeCode: String, organization: String) -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var employeeCode by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .padding(bottom = DesignTokens.Spacing.xl, top = DesignTokens.Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                text = stringResource(Res.string.profile_home_add_persona),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(Res.string.profile_home_field_display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = employeeCode,
                onValueChange = { employeeCode = it },
                label = { Text(stringResource(Res.string.profile_home_field_employee_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = organization,
                onValueChange = { organization = it },
                label = { Text(stringResource(Res.string.profile_home_field_organization)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val canAdd = displayName.isNotBlank() && employeeCode.isNotBlank()
            Button(
                onClick = { onAdd(displayName.trim(), employeeCode.trim(), organization.trim()) },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = DesignTokens.Shape.button,
            ) {
                Text(stringResource(Res.string.profile_home_add), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp), shape = DesignTokens.Shape.button) {
                Text(stringResource(Res.string.profile_home_cancel), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * RF.4: hosts the [ReferralCard], wiring the Koin [ReferralManager] (own code + redeem) to the platform
 * [LocalShareSheet] (no-op until SH.1 binds the real share sheet).
 */
@Composable
private fun ReferralCardHost(modifier: Modifier = Modifier) {
    val featureFlags: com.mileway.core.platform.FeatureFlags = koinInject()
    if (!featureFlags.referralEnabled) return // CF.1: gate the referral surface behind the flag.
    val referralManager: ReferralManager = koinInject()
    val shareSheet = LocalShareSheet.current
    val scope = rememberCoroutineScope()
    var myCode by remember { mutableStateOf("") }

    LaunchedEffect(referralManager) {
        myCode = referralManager.myReferralCode()
    }

    ReferralCard(
        myCode = myCode,
        onShare = {
            if (myCode.isNotEmpty()) {
                shareSheet.share(
                    text = buildReferralInvite(myCode),
                    subject = "Join me on Mileway",
                )
            }
        },
        onRedeem = { code -> scope.launch { referralManager.redeem(code) } },
        modifier = modifier,
    )
}
