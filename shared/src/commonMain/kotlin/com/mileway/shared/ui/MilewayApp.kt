package com.mileway.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.LanguageSelectionSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.advances_home_title
import com.mileway.core.ui.resources.app_name
import com.mileway.core.ui.resources.approvals_title
import com.mileway.core.ui.resources.eco_title
import com.mileway.core.ui.resources.events_history_title
import com.mileway.core.ui.resources.payables_title
import com.mileway.core.ui.resources.payments_history_title
import com.mileway.core.ui.resources.profile_org_title
import com.mileway.core.ui.resources.settings_language
import com.mileway.core.ui.resources.shell_more_agent
import com.mileway.core.ui.resources.shell_more_cards
import com.mileway.core.ui.resources.shell_more_garage
import com.mileway.core.ui.resources.shell_more_offers
import com.mileway.core.ui.resources.shell_more_plugins
import com.mileway.core.ui.resources.shell_more_routes
import com.mileway.core.ui.resources.shell_more_section_android_only
import com.mileway.core.ui.resources.shell_more_section_shared
import com.mileway.core.ui.resources.shell_more_subtitle
import com.mileway.core.ui.resources.shell_more_title
import com.mileway.core.ui.resources.shell_more_unavailable_capture
import com.mileway.core.ui.resources.shell_more_unavailable_capture_why
import com.mileway.core.ui.resources.shell_more_unavailable_debug
import com.mileway.core.ui.resources.shell_more_unavailable_debug_why
import com.mileway.core.ui.resources.shell_more_unavailable_profile
import com.mileway.core.ui.resources.shell_more_unavailable_profile_why
import com.mileway.core.ui.resources.shell_more_unavailable_signature
import com.mileway.core.ui.resources.shell_more_unavailable_signature_why
import com.mileway.core.ui.resources.shell_more_unavailable_storage
import com.mileway.core.ui.resources.shell_more_unavailable_storage_why
import com.mileway.core.ui.resources.support_chat_title
import com.mileway.core.ui.resources.tab_home
import com.mileway.core.ui.resources.tab_more
import com.mileway.core.ui.resources.tab_spends
import com.mileway.core.ui.resources.tab_track
import com.mileway.core.ui.resources.tab_travel
import com.mileway.core.ui.resources.tour_title
import com.mileway.core.ui.theme.MilewayDomain
import com.mileway.core.ui.theme.MilewayDomainTheme
import com.mileway.feature.advances.ui.AdvancesHomeScreen
import com.mileway.feature.agent.ui.screens.AgentChatScreen
import com.mileway.feature.approvals.ui.screens.ApprovalsScreen
import com.mileway.feature.cards.ui.CardsHomeScreen
import com.mileway.feature.events.ui.screens.EventsHistoryScreen
import com.mileway.feature.logging.ui.screens.SpendsHomeScreen
import com.mileway.feature.payables.ui.screens.PayablesHomeScreen
import com.mileway.feature.payments.ui.screens.PaymentsHistoryScreen
import com.mileway.feature.profile.ui.screens.EcoDashboardScreen
import com.mileway.feature.profile.ui.screens.FavouriteRoutesScreen
import com.mileway.feature.profile.ui.screens.OffersHubScreen
import com.mileway.feature.profile.ui.screens.OrgChartScreen
import com.mileway.feature.profile.ui.screens.PluginManagerScreen
import com.mileway.feature.profile.ui.screens.SelfAuditScreen
import com.mileway.feature.profile.ui.screens.SupportChatScreen
import com.mileway.feature.profile.ui.screens.TrainingTourScreen
import com.mileway.feature.profile.ui.screens.VehicleGarageScreen
import com.mileway.feature.tracking.ui.screens.TrackMilesScreen
import com.mileway.feature.travel.ui.screens.TravelHomeScreen
import com.mileway.feature.whatsnew.ui.WhatsNewDetailScreen
import com.mileway.feature.whatsnew.ui.WhatsNewListScreen
import com.mileway.ui.home.HomeScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class ShellTab(
    val label: StringResource,
    val icon: ImageVector,
)

/**
 * PLAN_V36 P8 (spec §10) — the overlay state for the reduced iOS shell, which has no Navigation-3
 * host (that graph is Android-only, see `whatsNewGraph`'s KDoc). A simple `remember` (not
 * `rememberSaveable`) — same choice this file already makes for `tab`/`showLanguage`, process death
 * just re-lands on the tab scaffold.
 *
 * Widened from the original four What's New states once feature:profile's screens and the
 * profile/media Koin modules reached commonMain: every branch below renders a real screen whose
 * whole dependency chain — screen, ViewModel, repository, Koin definition — now lives in
 * commonMain. What is still Android-only is listed on the More tab as an explicit, labelled row
 * with the reason, rather than silently omitted; see [AndroidOnlyEntry].
 *
 * One level deep by design. Every entry screen's "open a detail" callback is left at a no-op here:
 * a detail needs a back stack, and a back stack on iOS is exactly the Navigation-3 host this shell
 * deliberately does not have. The two pairs that are wired ([WhatsNew] → [WhatsNewEntry],
 * [VehicleGarage] → [VehicleSelfAudit]) each carry their own origin in the state, which is how the
 * original What's New overlay already handled its one push.
 */
private sealed interface ShellScreen {
    data object None : ShellScreen

    data object WhatsNew : ShellScreen

    data class WhatsNewEntry(
        val entryId: String,
        val cameFromList: Boolean,
    ) : ShellScreen

    data object Advances : ShellScreen

    data object Agent : ShellScreen

    data object Approvals : ShellScreen

    data object Cards : ShellScreen

    data object Events : ShellScreen

    data object Payables : ShellScreen

    data object Payments : ShellScreen

    data object Eco : ShellScreen

    data object FavouriteRoutes : ShellScreen

    data object Offers : ShellScreen

    data object OrgChart : ShellScreen

    data object PluginManager : ShellScreen

    data object SupportChat : ShellScreen

    data object TrainingTour : ShellScreen

    data object VehicleGarage : ShellScreen

    data class VehicleSelfAudit(
        val vehicleId: String,
    ) : ShellScreen
}

/** One openable row on the More tab. */
private data class MoreEntry(
    val label: StringResource,
    val screen: ShellScreen,
)

/**
 * One row on the More tab for a screen that genuinely cannot run here, kept visible with its reason
 * instead of being dropped from the list. Every one of these is blocked by a real platform API, not
 * by where its file happens to live: OS settings Intents and the biometric prompt (profile hub /
 * settings), CameraX and ML Kit (capture and document scan), the Android cache and database
 * directories (storage management and the debug menu), the Android bitmap encoder (signature pad).
 */
private data class AndroidOnlyEntry(
    val label: StringResource,
    val reason: StringResource,
)

private val shellTabs =
    listOf(
        ShellTab(Res.string.tab_home, Icons.Filled.Home),
        ShellTab(Res.string.tab_track, Icons.Filled.DirectionsCar),
        ShellTab(Res.string.tab_spends, Icons.Filled.ReceiptLong),
        ShellTab(Res.string.tab_travel, Icons.Filled.Flight),
        ShellTab(Res.string.tab_more, Icons.Filled.MoreHoriz),
    )

// Tab indices. MORE_TAB already existed; the rest were raw literals, which is what detekt's
// MagicNumber rule was pointing at. Naming all five is the fix, not a suppression.
private const val HOME_TAB = 0
private const val TRACK_TAB = 1
private const val SPENDS_TAB = 2
private const val TRAVEL_TAB = 3
private const val MORE_TAB = 4

/**
 * Mirrors Android's fourteen `NavGraphBuilder` graphs, minus the four already on their own tab
 * (home, tracking, logging, travel) and minus the three whose graph file is still androidMain —
 * `profileGraph` and `mediaGraph` import screens that need Intents/CameraX, and
 * `trackingNavigation` imports the odometer camera. The screens those three graphs host that DO
 * work here are listed individually below rather than through their graph.
 */
private val moreEntries =
    listOf(
        MoreEntry(Res.string.advances_home_title, ShellScreen.Advances),
        MoreEntry(Res.string.shell_more_cards, ShellScreen.Cards),
        MoreEntry(Res.string.approvals_title, ShellScreen.Approvals),
        MoreEntry(Res.string.payables_title, ShellScreen.Payables),
        MoreEntry(Res.string.payments_history_title, ShellScreen.Payments),
        MoreEntry(Res.string.events_history_title, ShellScreen.Events),
        MoreEntry(Res.string.shell_more_agent, ShellScreen.Agent),
        MoreEntry(Res.string.shell_more_garage, ShellScreen.VehicleGarage),
        MoreEntry(Res.string.shell_more_routes, ShellScreen.FavouriteRoutes),
        MoreEntry(Res.string.shell_more_offers, ShellScreen.Offers),
        MoreEntry(Res.string.eco_title, ShellScreen.Eco),
        MoreEntry(Res.string.profile_org_title, ShellScreen.OrgChart),
        MoreEntry(Res.string.tour_title, ShellScreen.TrainingTour),
        MoreEntry(Res.string.support_chat_title, ShellScreen.SupportChat),
        MoreEntry(Res.string.shell_more_plugins, ShellScreen.PluginManager),
    )

private val androidOnlyEntries =
    listOf(
        AndroidOnlyEntry(Res.string.shell_more_unavailable_profile, Res.string.shell_more_unavailable_profile_why),
        AndroidOnlyEntry(Res.string.shell_more_unavailable_capture, Res.string.shell_more_unavailable_capture_why),
        AndroidOnlyEntry(Res.string.shell_more_unavailable_storage, Res.string.shell_more_unavailable_storage_why),
        AndroidOnlyEntry(Res.string.shell_more_unavailable_debug, Res.string.shell_more_unavailable_debug_why),
        AndroidOnlyEntry(Res.string.shell_more_unavailable_signature, Res.string.shell_more_unavailable_signature_why),
    )

/**
 * The shared app-shell root that renders the real Mileway screens under a bottom-tab bar. Both the
 * Android `:app` (via its Navigation-3 host) and the iOS entry can render Mileway's screens; iOS
 * uses this composable directly (the Navigation-3 graph is Android-only), so iOS now shows the real
 * home dashboard + core features instead of a component showcase — full KMP/CMP shell parity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilewayApp() {
    var tab by remember { mutableIntStateOf(HOME_TAB) }
    var showLanguage by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf<ShellScreen>(ShellScreen.None) }
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.app_name)) },
                    actions = {
                        // Shared language switcher — the only in-app entry on iOS (Android also exposes it
                        // in Settings). Selecting a language flips LocalAppLocale, re-resolving every
                        // string instantly on both platforms.
                        IconButton(onClick = { showLanguage = true }) {
                            Icon(
                                Icons.Filled.Language,
                                contentDescription = stringResource(Res.string.settings_language),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    shellTabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, contentDescription = stringResource(t.label)) },
                            label = { Text(stringResource(t.label)) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    HOME_TAB ->
                        HomeScreen(
                            onStartTracking = { tab = 1 },
                            onAddExpense = { tab = 2 },
                            onOpenAccount = { tab = MORE_TAB },
                            onOpenWhatsNewEntry = { entryId ->
                                screen = ShellScreen.WhatsNewEntry(entryId, cameFromList = false)
                            },
                            onSeeAllWhatsNew = { screen = ShellScreen.WhatsNew },
                        )
                    TRACK_TAB ->
                        TrackMilesScreen(
                            onStop = { _, _, _, _, _ -> tab = HOME_TAB },
                            onOpenMap = {},
                            onOpenHwEvents = {},
                        )
                    SPENDS_TAB ->
                        SpendsHomeScreen(
                            onTrackMileage = { tab = TRACK_TAB },
                            onAddExpense = {},
                            onMileageHistory = {},
                            onExpenseHistory = {},
                        )
                    TRAVEL_TAB -> TravelHomeScreen()
                    else -> MoreTab(onOpen = { screen = it })
                }
            }
        }

        // Full-screen overlay above the tab scaffold. Back always lands on ShellScreen.None (the
        // tab the user came from is still selected underneath), except for the two wired pairs,
        // which pop to their origin — mirroring Android's NavHost backstack pop.
        ShellOverlay(screen = screen, onNavigate = { screen = it })
    }
    if (showLanguage) {
        LanguageSelectionSheet(onDismiss = { showLanguage = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreTab(onOpen: (ShellScreen) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.shell_more_title)) },
                supportingContent = { Text(stringResource(Res.string.shell_more_subtitle)) },
            )
            HorizontalDivider()
            SectionHeader(Res.string.shell_more_section_shared)
        }
        items(moreEntries) { entry ->
            ListItem(
                headlineContent = { Text(stringResource(entry.label)) },
                modifier = Modifier.clickable { onOpen(entry.screen) },
            )
        }
        item {
            HorizontalDivider()
            SectionHeader(Res.string.shell_more_section_android_only)
        }
        items(androidOnlyEntries) { entry ->
            // Deliberately not clickable: the row exists to say WHY the screen is missing, so a
            // reader of the iOS shell never has to diff it against Android's graph list to find out.
            ListItem(
                headlineContent = { Text(stringResource(entry.label)) },
                supportingContent = { Text(stringResource(entry.reason)) },
            )
        }
    }
}

@Composable
private fun SectionHeader(label: StringResource) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * Every shared screen the iOS shell can reach. One branch per destination, and
 * [ShellScreen.None] renders nothing so the tab scaffold shows through.
 *
 * The `{}` callbacks are not oversights: each opens a DETAIL screen, which needs a back stack, and
 * the back stack is the Navigation-3 host this shell does not have on iOS. They stay no-ops until
 * that host exists on both platforms.
 */
@Composable
// A flat dispatch table over a sealed interface: 19 branches, zero nesting, no conditional logic.
// CyclomaticComplexity counts branches, so exhaustive `when` dispatch always trips it; splitting
// this into sub-functions would hide the one place that maps a destination to its screen, which is
// the opposite of readable. The compiler already enforces exhaustiveness.
@Suppress("CyclomaticComplexMethod")
private fun ShellOverlay(
    screen: ShellScreen,
    onNavigate: (ShellScreen) -> Unit,
) {
    val back = { onNavigate(ShellScreen.None) }
    when (screen) {
        ShellScreen.None -> Unit
        ShellScreen.WhatsNew ->
            WhatsNewListScreen(
                onBack = back,
                onOpenEntry = { entryId -> onNavigate(ShellScreen.WhatsNewEntry(entryId, cameFromList = true)) },
            )
        is ShellScreen.WhatsNewEntry ->
            WhatsNewDetailScreen(
                entryId = screen.entryId,
                // Origin-aware: opened from the List row → back lands on List; opened directly from
                // Home's digest sheet/banner → back lands on Home, never on a List never opened.
                onBack = { onNavigate(if (screen.cameFromList) ShellScreen.WhatsNew else ShellScreen.None) },
            )
        ShellScreen.Advances ->
            AdvancesHomeScreen(
                onOpenPettyCard = {},
                onOpenQrCard = {},
                onRequestPettyAdvance = {},
                onRequestQrCard = {},
            )
        ShellScreen.Agent ->
            AgentChatScreen(onBack = back, onOpenHistory = {})
        ShellScreen.Approvals ->
            MilewayDomainTheme(MilewayDomain.APPROVALS) {
                ApprovalsScreen(onOpenDetail = {})
            }
        ShellScreen.Cards ->
            MilewayDomainTheme(MilewayDomain.CARDS) {
                CardsHomeScreen(onOpenCard = {}, onRequestCard = {})
            }
        ShellScreen.Events ->
            EventsHistoryScreen(onBack = back)
        ShellScreen.Payables ->
            MilewayDomainTheme(MilewayDomain.PAYABLES) {
                PayablesHomeScreen(onNewRequest = {}, onOpenPo = {})
            }
        ShellScreen.Payments ->
            PaymentsHistoryScreen(onBack = back)
        ShellScreen.Eco -> EcoDashboardScreen(onBack = back)
        ShellScreen.FavouriteRoutes -> FavouriteRoutesScreen(onBack = back)
        ShellScreen.Offers -> OffersHubScreen(onBack = back)
        ShellScreen.OrgChart -> OrgChartScreen(onBack = back)
        ShellScreen.PluginManager -> PluginManagerScreen(onBack = back)
        ShellScreen.SupportChat -> SupportChatScreen(onBack = back)
        ShellScreen.TrainingTour -> TrainingTourScreen(onBack = back)
        ShellScreen.VehicleGarage ->
            VehicleGarageScreen(
                onBack = back,
                onOpenSelfAudit = { vehicleId -> onNavigate(ShellScreen.VehicleSelfAudit(vehicleId)) },
            )
        is ShellScreen.VehicleSelfAudit ->
            SelfAuditScreen(
                vehicleId = screen.vehicleId,
                onBack = { onNavigate(ShellScreen.VehicleGarage) },
            )
    }
}
