package com.miletracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miletracker.core.platform.FeatureFlags
import com.miletracker.core.platform.ReviewTracker
import com.miletracker.core.ui.components.DotsIndicator
import com.miletracker.core.ui.platform.LocalAppReviewManager
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.stub.MarketingCarouselItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Bottom content padding so the last section clears the floating ~100dp bubble bottom bar
 * drawn by the shell. The Home screen is a top-level tab, so this padding is mandatory.
 */
private val BottomBarClearance = 140.dp

/**
 * The Home tab.
 *
 * A top-level destination: it owns its top inset through [HomeProfileHeader]'s gradient
 * (which applies `statusBarsPadding` internally) and has NO [androidx.compose.material3.TopAppBar] —
 * the gradient header *is* the top of the screen. All content scrolls in a single column and
 * floats above the bubble bottom bar via [BottomBarClearance].
 *
 * The screen is stateless: it reads an immutable [HomeUiState] and routes every user action
 * back through the [viewModel]'s one-shot intents to the hoisted navigation callbacks.
 *
 * @param onStartTracking opens the live mileage-tracking flow (primary CTA).
 * @param onAddExpense begins expense logging; the integrator maps this to "log miles".
 * @param onOpenAccount opens account / details surfaces (At A Glance rows, account actions).
 */
@Composable
fun HomeScreen(
    onStartTracking: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSearch: () -> Unit = onOpenAccount,
    viewModel: HomeViewModel = koinViewModel(),
    reviewTracker: ReviewTracker = koinInject(),
    featureFlags: FeatureFlags = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // V15 RV.4/CF.1: Home is a meaningful engagement signal — record it and prompt for review if eligible
    // and the in-app-review flag is on.
    val reviewManager = LocalAppReviewManager.current
    LaunchedEffect(Unit) {
        reviewTracker.recordFirstOpenIfNeeded()
        reviewTracker.recordInteraction()
        if (featureFlags.inAppReviewEnabled) reviewTracker.tryPrompt(reviewManager)
    }

    HomeScreenContent(
        state = state,
        onStartTracking = { viewModel.onStartTracking(onStartTracking) },
        onAddExpense = { viewModel.onAddExpense(onAddExpense) },
        onOpenAccount = { viewModel.onOpenAccount(onOpenAccount) },
        onOpenSearch = onOpenSearch,
    )
}

/**
 * Stateless body of the Home tab. Split out from [HomeScreen] so it can be exercised with a
 * hand-built [HomeUiState] and plain lambdas, independent of Koin and the ViewModel.
 */
@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    onStartTracking: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSearch: () -> Unit = onOpenAccount,
) {
    val scrollState = rememberScrollState()
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val illustrativeSnackbar: suspend () -> Unit = { snackbarState.showSnackbar("Detail view available in full version.") }
    val paymentsSnackbar: suspend () -> Unit = { snackbarState.showSnackbar("Payments require network in production.") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            // 1. Gradient header (draws behind the status bar; owns the top inset).
            HomeProfileHeader(
                name = state.greetingName,
                notificationCount = state.notificationCount,
                onSearch = onOpenSearch,
                onNotifications = onOpenAccount,
            )

            // 2. Animated banner strip (rotating 4000ms; replaces static ActionRequired card).
            AnimatedBannerStrip(isTrackingActive = false)

            Column(
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sectionSpacing),
            ) {
                Spacer(Modifier.height(DesignTokens.Spacing.l))

                // 3. Quick Actions.
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    HomeSectionHeader(title = "Quick Actions", leadingIcon = Icons.Filled.Bolt)
                    QuickActionsRow(
                        actions = quickActions(
                            onAddExpense = onAddExpense,
                            onIllustrative = onOpenAccount,
                        ),
                    )
                }

                // 4. Mileage card — promoted directly under Quick Actions so the primary
                //    "Track Journey" / "Log Miles" affordances are above the fold (Bug 5).
                HomeMileageCard(
                    onTrackJourney = onStartTracking,
                    onLogMiles = onAddExpense,
                )

                // 5. My Cards carousel (Phase O).
                MyCardsSection(onSnackbar = paymentsSnackbar)

                // 6. At A Glance 2×2 grid — each cell now routes to a distinct destination (Bug 5).
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    HomeSectionHeader(title = "At A Glance", leadingIcon = Icons.Filled.Insights)
                    AtAGlanceGrid(
                        counts = state.atAGlance,
                        onPendingExpenses = onAddExpense,
                        onUpcomingTrips = onOpenAccount,
                        onPendingApprovals = onOpenAccount,
                        onNotifications = onOpenAccount,
                    )
                }

                // 7. HomeCheckInCard — check-in button + recent list.
                HomeCheckInCard(onCheckIn = onOpenAccount)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.sectionSpacing))

            // 6. Feature / mileage carousel (full-bleed horizontal pager).
            FeatureCarousel(
                onStartTracking = onStartTracking,
                onIllustrative = onOpenAccount,
            )

            Column(
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sectionSpacing),
            ) {
                Spacer(Modifier.height(DesignTokens.Spacing.sectionSpacing))

                // 7. Recent Activity feed (Phase O).
                RecentActivitySection(onSnackbar = illustrativeSnackbar)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.sectionSpacing))

            // 8. Marketing / benefits strip (full-bleed horizontal list).
            MarketingStrip(items = state.marketingItems)

            Spacer(Modifier.height(BottomBarClearance))
        }

        SnackbarHost(snackbarState, modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
    }
}

/**
 * The horizontally paging feature carousel (Mileage / Customer Navigation / Track Reportees /
 * Center Check-In) with a [DotsIndicator] beneath it.
 */
@Composable
private fun FeatureCarousel(
    onStartTracking: () -> Unit,
    onIllustrative: () -> Unit,
) {
    val cards = remember(onStartTracking, onIllustrative) {
        featureCarouselCards(onStartTracking = onStartTracking, onIllustrative = onIllustrative)
    }
    val pagerState = rememberPagerState(pageCount = { cards.size })

    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.screenHorizontal),
            pageSpacing = DesignTokens.Spacing.m,
        ) { page ->
            FeatureCarouselCardView(card = cards[page])
        }
        DotsIndicator(
            pageCount = cards.size,
            selectedIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The static marketing/benefits card strip. */
@Composable
private fun MarketingStrip(items: List<MarketingCarouselItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        Box(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal)) {
            HomeSectionHeader(title = "Benefits", leadingIcon = Icons.Filled.CardGiftcard)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.carouselSpacing),
        ) {
            items(items) { item ->
                MarketingCardView(item = item)
            }
        }
    }
}
