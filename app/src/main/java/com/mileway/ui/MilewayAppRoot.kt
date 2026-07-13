package com.mileway.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.common.deeplink.DeepLinkRouter
import com.mileway.core.data.banner.Banner
import com.mileway.core.data.banner.BannerAssembler
import com.mileway.core.data.banner.BannerDismissalRepository
import com.mileway.core.data.lifecycle.DeletionRequestRepository
import com.mileway.core.data.lifecycle.DeletionState
import com.mileway.core.data.lifecycle.DeletionStatus
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
import com.mileway.core.data.session.DelegationSessionSource
import com.mileway.core.data.session.DelegationState
import com.mileway.core.data.subscription.SubscriptionRepository
import com.mileway.core.data.verification.DocStatus
import com.mileway.core.ui.components.banner.BannerHost
import com.mileway.core.ui.resources.banner_custom_text
import com.mileway.feature.profile.repository.DocumentRepository
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_delegation_acting_as
import com.mileway.core.ui.resources.profile_delegation_end
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.mileway.R
import com.mileway.core.ui.components.bottombar.BubbleBottomBar
import com.mileway.core.ui.toast.AppToastHost
import com.mileway.core.ui.components.bottombar.BubbleNavItem
import com.mileway.core.ui.components.bottombar.CollapsedBottomPuck
import com.mileway.core.ui.state.ShellBottomBarState
import com.mileway.core.ui.support.ShakeReportHost
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.ThemeController
import com.mileway.feature.approvals.model.ClarificationRoomSummary
import com.mileway.feature.approvals.repository.ClarificationRepository
import com.mileway.feature.approvals.ui.navigation.ApprovalsRoutes
import com.mileway.feature.approvals.ui.navigation.approvalsGraph
import com.mileway.feature.cards.ui.navigation.CardRoutes
import com.mileway.feature.cards.ui.navigation.cardsGraph
import com.mileway.feature.payables.ui.navigation.PayablesRoutes
import com.mileway.feature.payables.ui.navigation.payablesGraph
import com.mileway.feature.events.ui.navigation.EventsRoutes
import com.mileway.feature.events.ui.navigation.eventsGraph
import com.mileway.feature.payments.ui.navigation.PaymentsRoutes
import com.mileway.feature.payments.ui.navigation.paymentsGraph
import com.mileway.feature.agent.ui.AssistantEntryMode
import com.mileway.feature.agent.ui.AssistantFabSessionState
import com.mileway.feature.agent.ui.components.AssistantFab
import com.mileway.feature.agent.ui.navigation.agentGraph
import com.mileway.feature.travel.ui.navigation.TravelRoutes
import com.mileway.feature.travel.ui.navigation.travelGraph
import com.mileway.feature.logging.ui.navigation.LoggingRoutes
import com.mileway.feature.logging.ui.navigation.loggingGraph
import com.mileway.feature.media.ui.navigation.MediaRoutes
import com.mileway.feature.media.ui.navigation.mediaGraph
import com.mileway.feature.profile.ui.navigation.ProfileRoutes
import com.mileway.feature.profile.ui.navigation.profileGraph
import com.mileway.feature.tracking.debug.DebugMenuScreen
import com.mileway.feature.tracking.ui.navigation.TrackingRoutes
import com.mileway.feature.tracking.ui.navigation.trackingGraph
import com.mileway.ui.home.HomeScreen
import com.mileway.ui.search.MasterSearchRoute
import com.mileway.ui.search.toSectionRoute
import org.koin.compose.koinInject

private data class TabSpec(
    val graphRoute: String,
    val item: BubbleNavItem
)

/**
 * Single-Activity bottom-navigation shell. Hosts every feature's nested nav graph and
 * applies the app theme exactly once, honouring the runtime [ThemeController] override.
 *
 * The bottom bar is the bubble system: a floating cutout pill with a draggable FAB
 * (drag horizontally to select, throw up for the assistant action, drag down to collapse
 * into a corner puck whose long-press wheel selects tabs).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilewayAppRoot(
    deepLinkRoute: String? = null,
    themeController: ThemeController = koinInject(),
    onSignedOut: () -> Unit = {},
) {
    val systemDark = isSystemInDarkTheme()
    val override by themeController.darkThemeOverride.collectAsStateWithLifecycle()
    val milewayTheme by themeController.milewayTheme.collectAsStateWithLifecycle()
    val palette by themeController.accentPalette.collectAsStateWithLifecycle()
    val customSeedHex by themeController.customSeedHex.collectAsStateWithLifecycle()
    val useSystemColors by themeController.useSystemColors.collectAsStateWithLifecycle()
    val paletteStyle by themeController.paletteStyle.collectAsStateWithLifecycle()
    val mapProvider by themeController.mapProvider.collectAsStateWithLifecycle()

    MilewayTheme(
        darkTheme = override ?: systemDark,
        milewayTheme = milewayTheme,
        palette = palette,
        customSeedHex = customSeedHex,
        useSystemColors = useSystemColors,
        paletteStyle = paletteStyle,
        mapProvider = mapProvider,
    ) {
        AppToastHost {
        val navController = rememberNavController()

        // PLAN_V24 P7.3: app-wide "Acting as <name>" banner while a session delegation is active.
        val delegationSource = koinInject<DelegationSessionSource>()
        val delegation by delegationSource.delegationState.collectAsStateWithLifecycle(DelegationState())
        val delegationScope = rememberCoroutineScope()

        // PLAN_V28 P28.4: clarification room-summary badge on the Approvals nav tab.
        val clarificationRepository = koinInject<ClarificationRepository>()
        val roomSummary by clarificationRepository.observeRoomSummary().collectAsStateWithLifecycle(ClarificationRoomSummary())

        // Navigate to a deep-linked graph immediately after the nav graph is ready.
        androidx.compose.runtime.LaunchedEffect(deepLinkRoute) {
            if (deepLinkRoute != null) {
                navController.navigate(deepLinkRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        val tabs = remember(roomSummary.totalUnread) {
            listOf(
                TabSpec(
                    AppGraph.TRAVEL,
                    BubbleNavItem("Travel", Icons.Filled.TravelExplore, Icons.Outlined.TravelExplore)
                ),
                TabSpec(
                    AppGraph.LOG,
                    BubbleNavItem("Spends", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong)
                ),
                TabSpec(
                    AppGraph.PAYABLES,
                    BubbleNavItem("Payables", Icons.Filled.Business, Icons.Outlined.Business)
                ),
                TabSpec(
                    AppGraph.HOME,
                    BubbleNavItem(
                        label = "Home",
                        painter = { painterResource(R.drawable.ic_logo_mark) },
                        isHome = true
                    )
                ),
                TabSpec(
                    AppGraph.APPROVALS,
                    BubbleNavItem(
                        "Approvals",
                        Icons.Filled.PersonAdd,
                        Icons.Outlined.PersonAdd,
                        badgeCount = roomSummary.totalUnread.takeIf { it > 0 },
                    )
                ),
                TabSpec(
                    AppGraph.PROFILE,
                    BubbleNavItem("Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
                )
            )
        }
        val homeIndex = remember(tabs) { tabs.indexOfFirst { it.item.isHome } }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val selectedIndex = tabs.indexOfFirst { tab ->
            currentDestination?.hierarchy?.any { it.route == tab.graphRoute } == true
        }.let { if (it >= 0) it else homeIndex }

        var isBottomBarCollapsed by rememberSaveable { mutableStateOf(false) }

        // The floating bubble bar shows only on top-level tab destinations; detail and
        // flow screens (tracking, submission, settings, …) own the full screen, matching
        // the source app, where those flows render without the bottom nav.
        //
        // currentDestination.route is always the *leaf* composable route (never the parent
        // NavGraph's own route), so this set must list each tab's actual start-destination
        // route — not the graph route from AppGraph. Using AppGraph.HOME/TRAVEL/APPROVALS
        // here never matched the leaf destinations ("home_screen"/TravelRoutes.HOME/
        // ApprovalsRoutes.HOME), which hid the bubble bar on those three tabs (V32 SN fix).
        val topLevelRoutes = remember {
            setOf(
                "home_screen",
                LoggingRoutes.HOME,
                ProfileRoutes.HOME,
                TravelRoutes.HOME,
                PayablesRoutes.HOME,
                ApprovalsRoutes.HOME,
            )
        }
        val onTopLevelDestination = currentDestination?.route in topLevelRoutes
        // V32 SN: a top-level screen (e.g. Approvals' bulk-selection bar) can show its own
        // pinned bottom bar; suppress the floating bubble bar while that's up so they don't stack.
        val contextualBarActive by ShellBottomBarState.contextualBarActive.collectAsStateWithLifecycle()

        BackHandler(enabled = isBottomBarCollapsed) {
            isBottomBarCollapsed = false
        }

        fun navigateToTab(index: Int) {
            val tab = tabs.getOrNull(index) ?: return
            navController.navigate(tab.graphRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Scaffold(
            // Edge-to-edge: the shell consumes no insets itself. Every screen owns its
            // insets via its own Scaffold/TopAppBar, so the status-bar inset is applied
            // exactly once, and content draws behind the floating bubble bar.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AnimatedVisibility(
                    visible = onTopLevelDestination && !isBottomBarCollapsed && !contextualBarActive,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f)
                ) {
                    BubbleBottomBar(
                        items = tabs.map { it.item },
                        selectedItemIndex = selectedIndex,
                        onItemSelected = ::navigateToTab,
                        onItemReselected = { index ->
                            // Re-tapping the FAB pops the current tab back to its start.
                            val tab = tabs.getOrNull(index) ?: return@BubbleBottomBar
                            navController.navigate(tab.graphRoute) {
                                popUpTo(tab.graphRoute) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        // Throw-up gesture on the centre FAB opens the full-screen AI Agent.
                        onFabThrowUp = { navController.navigate(AppRoutes.AGENT_CHAT) },
                        onCollapseRequested = { isBottomBarCollapsed = true }
                    )
                }
            }
        ) { _ ->
            // innerPadding is deliberately NOT applied: content draws full-bleed behind the
            // floating bar; top-level screens add their own bottom content padding instead.
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = AppGraph.HOME,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Centre tab: the Home screen. Its mileage card opens the tracking flow.
                    navigation(startDestination = "home_screen", route = AppGraph.HOME) {
                        composable("home_screen") {
                            HomeScreen(
                                onStartTracking = { navController.navigate(AppGraph.TRACK) },
                                onAddExpense = { navigateToTab(tabs.indexOfFirst { it.graphRoute == AppGraph.LOG }) },
                                onOpenAccount = { navigateToTab(tabs.indexOfFirst { it.graphRoute == AppGraph.PROFILE }) },
                                onOpenSearch = { navController.navigate(AppRoutes.SEARCH) },
                                onOpenAgent = {
                                    AssistantFabSessionState.onChatOpen()
                                    navController.navigate(AppRoutes.AGENT_CHAT)
                                },
                                // P29.H.2: "Add Invoice" quick action opens the real Payables create flow.
                                onAddInvoice = { navController.navigate(PayablesRoutes.CREATE) { popUpTo(AppGraph.PAYABLES) } },
                                // P29.H.2: "Ask Advance" — config-aware QR vs classic form, both real Profile-tab flows.
                                onAskAdvanceQr = { navController.navigate(ProfileRoutes.QR_HOME) { popUpTo(AppGraph.PROFILE) } },
                                onAskAdvanceClassic = { navController.navigate(ProfileRoutes.ASK_ADVANCE) { popUpTo(AppGraph.PROFILE) } },
                            )
                        }
                    }
                    navigation(startDestination = TrackingRoutes.SAVED_TRACKS, route = AppGraph.TRACK) {
                        trackingGraph(
                            navController = navController,
                            // P27.E.5: trip-completion "Add Expense" CTA.
                            onAddExpense = { ctx -> navController.navigate(LoggingRoutes.expenseEntryRoute(ctx)) },
                        )
                    }
                    navigation(startDestination = LoggingRoutes.HOME, route = AppGraph.LOG) {
                        loggingGraph(navController)
                    }
                    navigation(startDestination = MediaRoutes.SELECTION, route = AppGraph.MEDIA) {
                        mediaGraph(navController)
                    }
                    navigation(startDestination = ProfileRoutes.HOME, route = AppGraph.PROFILE) {
                        profileGraph(
                            navController = navController,
                            onOpenDebugMenu = { navController.navigate(AppRoutes.DEBUG_MENU) },
                            onOpenCards = { navController.navigate(AppGraph.CARDS) },
                            onStartTripForAdvance = { _, tripId ->
                                navController.navigate(TrackingRoutes.liveTrack(tripId)) {
                                    popUpTo(AppGraph.TRACK)
                                }
                            },
                            // P27.E.8: "Log expense against this advance" CTA.
                            onLogExpenseFromAdvance = { ctx -> navController.navigate(LoggingRoutes.expenseEntryRoute(ctx)) },
                            onSignedOut = onSignedOut,
                            // P29.S.6: Notification Centre card taps — same best-effort resolve-and-navigate
                            // as master search's onOpenResult; unresolvable links are a safe no-op.
                            onOpenDeepLink = { link -> navController.navigateToSectionRoute(DeepLinkRouter.resolve(link).toAppRoute()) },
                        )
                    }
                    // Corporate cards feature module (replaces the old profile card screens).
                    navigation(startDestination = CardRoutes.HOME, route = AppGraph.CARDS) {
                        cardsGraph(
                            navController = navController,
                            // P27.E.7: claim-transaction CTA (replaces the old toast-only stub).
                            onClaimTransaction = { ctx -> navController.navigate(LoggingRoutes.expenseEntryRoute(ctx)) },
                        )
                    }
                    navigation(startDestination = TravelRoutes.HOME, route = AppGraph.TRAVEL) {
                        travelGraph(navController)
                    }
                    navigation(startDestination = PayablesRoutes.HOME, route = AppGraph.PAYABLES) {
                        payablesGraph(navController)
                    }
                    navigation(startDestination = PaymentsRoutes.HOME, route = AppGraph.PAYMENTS) {
                        paymentsGraph(navController)
                    }
                    navigation(startDestination = EventsRoutes.HOME, route = AppGraph.EVENTS) {
                        eventsGraph(
                            navController = navController,
                            onLogExpense = { ctx -> navController.navigate(LoggingRoutes.expenseEntryRoute(ctx)) },
                        )
                    }
                    navigation(startDestination = ApprovalsRoutes.HOME, route = AppGraph.APPROVALS) {
                        approvalsGraph(navController)
                    }
                    // Global debug destination, outside bottom-nav graphs so it renders
                    // full-screen without the bottom bar.
                    composable(AppRoutes.DEBUG_MENU) {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        DebugMenuScreen(
                            onBack = { navController.popBackStack() },
                            onOpenHttpInspector = com.mileway.debug.WormaCeptorHelper.getLaunchIntent(ctx)
                                ?.let { intent -> { ctx.startActivity(intent) } },
                            onOpenNetworkLog = { navController.navigate(AppRoutes.NETWORK_LOG) },
                            onOpenShowcase = com.mileway.debug.ShowcaseLauncher.getLaunchIntent(ctx)
                                ?.let { intent -> { ctx.startActivity(intent) } },
                        )
                    }
                    // V21 §3 Wave 4: local network log screen, reached from the debug menu.
                    composable(AppRoutes.NETWORK_LOG) {
                        com.mileway.feature.tracking.debug.NetworkLogScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    // Global master-search destination, full-screen, outside bottom-nav graphs. A tapped
                    // result routes to the section graph that owns the entity (best-effort; some types have
                    // no destination yet and are ignored).
                    composable(AppRoutes.SEARCH) {
                        MasterSearchRoute(
                            onBack = { navController.popBackStack() },
                            onOpenResult = { result -> navController.navigateToSectionRoute(result.toSectionRoute()) },
                            // P29.S.5: quick-action tap — same best-effort DeepLinkRouter resolve as a
                            // tapped result; QuickActionRegistry deeplinks not yet resolvable are ignored.
                            onOpenAction = { action -> navController.navigateToSectionRoute(DeepLinkRouter.resolve(action.deeplink).toAppRoute()) },
                        )
                    }

                    // Full-screen AI Agent, entered via FAB throw-up gesture.
                    agentGraph(navController)
                }

                // PLAN_V24 P13.1: the priority banner stack, pinned to the top and visible on every
                // screen. Empty in the baseline; the delegate "Acting as <name>" banner is one variant.
                RootBannerHost(
                    delegation = delegation,
                    onDelegateEnd = { delegationScope.launch { delegationSource.endDelegation() } },
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                // P31.MISC.1: shake-to-report — no visible UI until the device is shaken, then pops
                // the quick-actions sheet open on top of whatever screen is showing.
                ShakeReportHost(screen = backStackEntry?.destination?.route ?: "unknown")

                // Global AI assistant FAB — hidden while agent chat is open.
                val fabMode by AssistantFabSessionState.mode.collectAsStateWithLifecycle()
                val isChatOpen by AssistantFabSessionState.isChatOpen.collectAsStateWithLifecycle()
                val currentRoute = backStackEntry?.destination?.route
                val onAgentScreen = currentRoute?.startsWith("agent/") == true
                AnimatedVisibility(
                    visible = fabMode == AssistantEntryMode.FAB && !isChatOpen && !onAgentScreen,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 88.dp),
                ) {
                    AssistantFab(
                        onOpen = {
                            AssistantFabSessionState.onChatOpen()
                            navController.navigate(AppRoutes.AGENT_CHAT)
                        },
                        onDismissToTopbar = { AssistantFabSessionState.switchToTopbar() },
                    )
                }

                // Collapsed-state puck: bottom-end overlay with long-press wheel selector.
                AnimatedVisibility(
                    visible = isBottomBarCollapsed,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + scaleIn(
                        initialScale = 0.68f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + scaleOut(
                        targetScale = 0.68f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    CollapsedBottomPuck(
                        items = tabs.map { it.item },
                        selectedItemIndex = selectedIndex,
                        onExpand = { isBottomBarCollapsed = false },
                        onItemSelected = ::navigateToTab
                    )
                }
            }
        }

        } // AppToastHost

    }
}

/**
 * PLAN_V24 P13.1: collects every priority-banner source (delegate, deletion, documents,
 * subscription, plus the plugin-gated custom/update-ready banners), assembles them with
 * [BannerAssembler], and renders the [BannerHost]. Split out of [MilewayAppRoot] so the root stays
 * within the cyclomatic-complexity budget. Every non-delegate source is default-off / empty in the
 * baseline, so this renders nothing there and the goldens stay byte-identical.
 */
@Composable
private fun RootBannerHost(
    delegation: DelegationState,
    onDelegateEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val dismissalRepo = koinInject<BannerDismissalRepository>()
    val deletionRepo = koinInject<DeletionRequestRepository>()
    val documentRepo = koinInject<DocumentRepository>()
    val subscriptionRepo = koinInject<SubscriptionRepository>()
    val registry = koinInject<PluginRegistry>()

    val dismissedIds by dismissalRepo.observeDismissed().collectAsStateWithLifecycle(emptySet())
    val deletionState by deletionRepo.observe().collectAsStateWithLifecycle(DeletionState())
    val activeSubscription by subscriptionRepo.observeActive().collectAsStateWithLifecycle(null)
    val documents by documentRepo.observeAll().collectAsStateWithLifecycle(emptyList())
    val customEnabled by registry.observe("customBannerEnabled").collectAsStateWithLifecycle(false)
    val updateEnabled by registry.observe("updateReadyBanner").collectAsStateWithLifecycle(false)
    val docExpiryEnabled by registry.observe("documentExpiryBanner").collectAsStateWithLifecycle(false)
    val subAlertValue by registry.observeValue("subscriptionExpiryAlertDays")
        .collectAsStateWithLifecycle(PluginValue.IntVal(7))
    val customText = stringResource(Res.string.banner_custom_text)

    val banners =
        assembleRootBanners(
            updateReadyEnabled = updateEnabled,
            customBannerEnabled = customEnabled,
            customBannerText = customText,
            delegation = delegation,
            deletionStatus = deletionState.status,
            documentExpiryEnabled = docExpiryEnabled,
            rejectedDocCount = documents.count { it.status == DocStatus.REJECTED },
            activeSubscription = activeSubscription,
            subscriptionThresholdDays = (subAlertValue as? PluginValue.IntVal)?.value ?: 7,
            dismissedIds = dismissedIds,
            nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        )

    BannerHost(
        banners = banners,
        onDismiss = { banner -> scope.launch { dismissalRepo.dismiss(banner.id) } },
        onDelegateEnd = onDelegateEnd,
        modifier = modifier,
    )
}

/**
 * PLAN_V24 P13.1: the pure banner-collection step — turns the resolved source states into the
 * ordered, dismissal-filtered banner list. Kept a plain function (out of the composable) so the
 * branchy assembly does not inflate [MilewayAppRoot]/[RootBannerHost] complexity.
 */
private fun assembleRootBanners(
    updateReadyEnabled: Boolean,
    customBannerEnabled: Boolean,
    customBannerText: String,
    delegation: DelegationState,
    deletionStatus: DeletionStatus,
    documentExpiryEnabled: Boolean,
    rejectedDocCount: Int,
    activeSubscription: com.mileway.core.data.subscription.ActiveSubscription?,
    subscriptionThresholdDays: Int,
    dismissedIds: Set<String>,
    nowMs: Long,
): List<Banner> {
    val raw =
        buildList {
            if (updateReadyEnabled) add(Banner.UpdateReady)
            if (customBannerEnabled) add(Banner.Custom(customBannerText))
            if (delegation.isActing) add(Banner.Delegate(delegation.actingName.orEmpty()))
            if (deletionStatus != DeletionStatus.NONE) add(Banner.DeletionRequested)
            if (documentExpiryEnabled && rejectedDocCount > 0) add(Banner.DocumentExpiry(rejectedDocCount))
            activeSubscription?.let { sub ->
                val daysLeft = ((sub.renewsAtMs - nowMs) / 86_400_000L).toInt()
                if (daysLeft in 0..subscriptionThresholdDays) add(Banner.SubscriptionExpiry(daysLeft))
            }
        }
    return BannerAssembler.assemble(raw, dismissedIds)
}

