package com.mileway.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.ThemeController
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

        val tabs = remember {
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
                    BubbleNavItem("Approvals", Icons.Filled.PersonAdd, Icons.Outlined.PersonAdd)
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
        val topLevelRoutes = remember {
            setOf(
                AppGraph.HOME,
                LoggingRoutes.HOME,
                ProfileRoutes.HOME,
                AppGraph.TRAVEL,
                PayablesRoutes.HOME,
                AppGraph.APPROVALS,
            )
        }
        val onTopLevelDestination = currentDestination?.route in topLevelRoutes

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
                    visible = onTopLevelDestination && !isBottomBarCollapsed,
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
                            )
                        }
                    }
                    navigation(startDestination = TrackingRoutes.SAVED_TRACKS, route = AppGraph.TRACK) {
                        trackingGraph(navController)
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
                        )
                    }
                    // Corporate cards feature module (replaces the old profile card screens).
                    navigation(startDestination = CardRoutes.HOME, route = AppGraph.CARDS) {
                        cardsGraph(navController)
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
                        eventsGraph(navController)
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
                            onOpenShowcase = com.mileway.debug.ShowcaseLauncher.getLaunchIntent(ctx)
                                ?.let { intent -> { ctx.startActivity(intent) } },
                        )
                    }
                    // Global master-search destination, full-screen, outside bottom-nav graphs. A tapped
                    // result routes to the section graph that owns the entity (best-effort; some types have
                    // no destination yet and are ignored).
                    composable(AppRoutes.SEARCH) {
                        MasterSearchRoute(
                            onBack = { navController.popBackStack() },
                            onOpenResult = { result ->
                                val route = result.toSectionRoute()
                                if (route != null) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }

                    // Full-screen AI Agent, entered via FAB throw-up gesture.
                    agentGraph(navController)
                }

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
