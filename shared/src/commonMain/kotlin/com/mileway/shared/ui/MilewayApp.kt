package com.mileway.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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
import com.mileway.feature.approvals.ui.screens.ApprovalDetailsScreen
import com.mileway.feature.approvals.ui.screens.ApprovalsScreen
import com.mileway.feature.approvals.ui.screens.ReportApprovalScreen
import com.mileway.feature.cards.ui.CardsHomeScreen
import com.mileway.feature.events.ui.screens.EventsHistoryScreen
import com.mileway.feature.logging.perdiem.PerDiemSheet
import com.mileway.feature.logging.report.ReportGroupingScreen
import com.mileway.feature.logging.report.ReportSubmitScreen
import com.mileway.feature.logging.ui.screens.SpendsHomeScreen
import com.mileway.feature.payables.ui.screens.PayablesHomeScreen
import com.mileway.feature.payments.ui.screens.PaymentsHistoryScreen
import com.mileway.feature.profile.admin.RateTableEditorScreen
import com.mileway.feature.profile.status.ReimbursementStatusScreen
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
import org.koin.compose.viewmodel.koinViewModel

private data class ShellTab(
    val label: StringResource,
    val icon: ImageVector,
)

/**
 * Destinations layered over the shared tab shell. Claim details carry their origin so Back returns
 * to the entry that opened them. Each visible overlay owns a ViewModel store, cleared on exit.
 * The shell keeps its existing in-memory navigation state; restoring a process opens Home.
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

    data class ApprovalDetail(
        val id: String,
    ) : ShellScreen

    data class ReportGrouping(
        val reportId: String? = null,
        val returnTo: ShellScreen = None,
    ) : ShellScreen

    data object PerDiem : ShellScreen

    data class ReportSubmit(
        val reportId: String,
        val origin: ShellScreen,
    ) : ShellScreen

    data object Reimbursements : ShellScreen

    data object Rates : ShellScreen

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
                            onExpenseReports = { screen = ShellScreen.ReportGrouping() },
                            onPerDiem = { screen = ShellScreen.PerDiem },
                        )
                    TRAVEL_TAB -> TravelHomeScreen()
                    else -> MoreTab(onOpen = { screen = it })
                }
            }
        }

        if (screen != ShellScreen.None) {
            key(screen) {
                val owner =
                    remember {
                        object : ViewModelStoreOwner {
                            override val viewModelStore = ViewModelStore()
                        }
                    }
                DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    Surface(Modifier.fillMaxSize()) {
                        ShellOverlay(screen = screen, onNavigate = { screen = it })
                    }
                }
            }
        }
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
        item {
            ListItem(
                headlineContent = { Text("Reimbursements") },
                modifier = Modifier.clickable { onOpen(ShellScreen.Reimbursements) },
            )
            ListItem(
                headlineContent = { Text("Local rate editor") },
                modifier = Modifier.clickable { onOpen(ShellScreen.Rates) },
            )
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

/** Shared screen dispatch, assembled above the features without feature dependencies. */
@Composable
// Exhaustive destination dispatch is deliberately kept in one place.
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
                Column(Modifier.fillMaxSize()) {
                    TextButton(onClick = back) { Text("Back") }
                    Box(Modifier.weight(1f)) {
                        ApprovalsScreen(onOpenDetail = { onNavigate(ShellScreen.ApprovalDetail(it)) })
                    }
                }
            }
        is ShellScreen.ApprovalDetail ->
            MilewayDomainTheme(MilewayDomain.APPROVALS) {
                val backToApprovals = { onNavigate(ShellScreen.Approvals) }
                if (screen.id.startsWith("report:")) {
                    ReportApprovalScreen(screen.id.removePrefix("report:"), onBack = backToApprovals)
                } else {
                    ApprovalDetailsScreen(approvalId = screen.id, onBack = backToApprovals)
                }
            }
        is ShellScreen.ReportGrouping ->
            MilewayDomainTheme(MilewayDomain.EXPENSES) {
                ReportGroupingScreen(
                    viewModel = koinViewModel(),
                    reportId = screen.reportId,
                    onBack = { onNavigate(screen.returnTo) },
                    onOpenReport = { id ->
                        onNavigate(if (id == screen.reportId) screen.returnTo else ShellScreen.ReportSubmit(id, screen))
                    },
                )
            }
        ShellScreen.PerDiem ->
            MilewayDomainTheme(MilewayDomain.EXPENSES) {
                PerDiemSheet(
                    viewModel = koinViewModel(),
                    onBack = back,
                    onOpenReport = { onNavigate(ShellScreen.ReportSubmit(it, ShellScreen.PerDiem)) },
                )
            }
        is ShellScreen.ReportSubmit ->
            MilewayDomainTheme(MilewayDomain.EXPENSES) {
                ReportSubmitScreen(
                    reportId = screen.reportId,
                    viewModel = koinViewModel(),
                    onBack = { onNavigate(screen.origin) },
                    onEdit = { onNavigate(ShellScreen.ReportGrouping(screen.reportId, returnTo = screen)) },
                )
            }
        ShellScreen.Reimbursements -> ReimbursementStatusScreen(onBack = back)
        ShellScreen.Rates -> RateTableEditorScreen(onBack = back)
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
