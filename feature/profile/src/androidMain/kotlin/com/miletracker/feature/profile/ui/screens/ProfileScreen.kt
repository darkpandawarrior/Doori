package com.miletracker.feature.profile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miletracker.core.network.model.DemoAccount
import com.miletracker.core.network.model.UserSession
import com.miletracker.core.platform.ReferralManager
import com.miletracker.core.ui.components.GridProfileTile
import com.miletracker.core.ui.components.ProfileGridItem
import com.miletracker.core.ui.components.ProfileItemStatus
import com.miletracker.core.ui.components.ReferralCard
import com.miletracker.core.ui.components.buildReferralInvite
import com.miletracker.core.ui.components.sheet.AppActionSheet
import com.miletracker.core.ui.platform.LocalShareSheet
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.dataStyle
import com.miletracker.feature.profile.model.AccountAnalyticsSnapshot
import com.miletracker.feature.profile.model.ProfileHeader
import com.miletracker.feature.profile.viewmodel.ProfileAction
import com.miletracker.feature.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
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
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                onOpenSessions = { viewModel.onAction(ProfileAction.OpenSessionsDialog) },
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

    if (state.showSessionsDialog) {
        SessionsDialog(
            sessions = state.sessions,
            onDismiss = { viewModel.onAction(ProfileAction.DismissSessionsDialog) },
        )
    }
}

/** Badge count shown on the Notifications tile (matches the reference "174"). */
private const val NOTIFICATION_BADGE = 174

private val AvatarSize = 96.dp

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
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape,
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
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit profile photo",
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
                                .clip(CircleShape)
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
            left = accountTile("acc_details", "Details", "Personal info", Icons.Default.Person, blue, onOpenDetails),
            right =
                accountTile(
                    "acc_notifications",
                    "Notifications",
                    "Stay updated",
                    Icons.Default.Notifications,
                    red,
                    onOpenNotifications,
                    badgeCount = notificationBadge,
                ),
        )
        TileRow(
            left = accountTile("acc_settings", "Settings", "App settings", Icons.Default.Settings, green, onOpenSettings),
            right = accountTile("acc_preferences", "Preferences", "Manage settings", Icons.Default.Tune, orange, onOpenPreferences),
        )
        TileRow(
            left = accountTile("acc_sessions", "Active Sessions", "Devices", Icons.Default.Devices, purple, onOpenSessions),
            right = accountTile("acc_about", "About & Support", "Help & info", Icons.AutoMirrored.Filled.HelpOutline, violet, onOpenAboutSupport),
        )
        TileRow(
            left = accountTile("acc_advance", "My Advances", "Cash advances", Icons.Default.MonetizationOn, teal, onOpenAdvance),
            right = accountTile("acc_cards", "Corporate Cards", "Manage cards", Icons.Default.CreditCard, indigo, onOpenCards),
        )
        TileRow(
            left = accountTile("acc_delegation", "Delegation", "Manage authority", Icons.Default.SupervisorAccount, cyan, onOpenDelegation),
            right = accountTile("acc_insights", "Insights", "Analytics view", Icons.Default.History, Color(0xFF6D28D9), action = {}),
        )
        TileRow(
            left = accountTile("acc_demo", "Demo Settings", "Feature toggles", Icons.Default.BugReport, darkTeal, onOpenDemoSettings),
            right = accountTile("acc_qr", "My QR Code", "Scan to share", Icons.Default.QrCode2, Color(0xFF0F4C81), onOpenQr),
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
                        shape = RoundedCornerShape(10.dp),
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
                            text = "Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Live · ${analytics.window}",
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
                                .clip(CircleShape)
                                .background(DesignTokens.StatusColors.success),
                    )
                    Spacer(Modifier.width(DesignTokens.Spacing.xs))
                    Text(
                        text = "Live insights",
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
                        text = "Total Spend",
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
                        text = "Transactions",
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
                text = "Updated at ${analytics.updatedAt}",
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

/** Simple list dialog of the devices this account is signed in on. */
@Composable
private fun SessionsDialog(
    sessions: List<UserSession>,
    onDismiss: () -> Unit,
) {
    AppActionSheet(
        onDismiss = onDismiss,
        title = "Active Sessions",
    ) {
        sessions.forEach { session ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
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
                    Text(
                        text = session.deviceName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = session.platform,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (session.isCurrent) {
                    Surface(
                        shape = DesignTokens.Shape.chip,
                        color = DesignTokens.StatusColors.success.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = "This device",
                            style = MaterialTheme.typography.labelSmall,
                            color = DesignTokens.StatusColors.success,
                            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

/**
 * Demo card that shows `miletracker://` deep link buttons. Each button fires a VIEW intent
 * so the system routes back into the app with the deep link, exercising intent parsing.
 */
@Composable
private fun DeepLinkDemoCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val links =
        listOf(
            "home" to "miletracker://home",
            "log" to "miletracker://log",
            "track" to "miletracker://track",
            "profile" to "miletracker://profile",
        )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(
                text = "Deep Link Demo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Fires miletracker:// VIEW intents to exercise deep link routing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                links.forEach { (label, url) ->
                    OutlinedButton(
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
 * Horizontal scrolling row of persona chips. Tapping a chip switches the active account.
 * The selected chip uses a primary-container highlight; others use surface tones.
 */
@Composable
private fun PersonaSwitcherRow(
    personas: List<DemoAccount>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (personas.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Text(
            text = "Switch Persona",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            contentPadding = PaddingValues(vertical = DesignTokens.Spacing.xs),
        ) {
            items(personas, key = { it.id }) { persona ->
                val isSelected = persona.id == selectedId
                Card(
                    shape = RoundedCornerShape(12.dp),
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
                            .clickable { onSelect(persona.id) },
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
                                        .clip(CircleShape)
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
                                text = if (isSelected) "Active" else "Switch",
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
                        )
                    }
                }
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
    val featureFlags: com.miletracker.core.platform.FeatureFlags = koinInject()
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
