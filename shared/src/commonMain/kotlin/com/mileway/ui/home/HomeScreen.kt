package com.mileway.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.banner.HomeBanner
import com.mileway.core.platform.FeatureFlags
import com.mileway.core.platform.ReviewTracker
import com.mileway.core.ui.components.DotsIndicator
import com.mileway.core.ui.components.RateAppSheet
import com.mileway.core.ui.components.WhatsNewAnimatedButton
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.allStringResources
import com.mileway.core.ui.resources.home_banner_club_badge
import com.mileway.core.ui.resources.home_banner_club_subtitle
import com.mileway.core.ui.resources.home_banner_club_title
import com.mileway.core.ui.resources.home_banner_offers_badge
import com.mileway.core.ui.resources.home_banner_offers_subtitle
import com.mileway.core.ui.resources.home_banner_offers_title
import com.mileway.core.ui.resources.shared_home_at_a_glance
import com.mileway.core.ui.resources.shared_home_quick_actions
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource
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
 * (which applies `statusBarsPadding` internally) and has NO [androidx.compose.material3.TopAppBar],
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
@Suppress("LongParameterList") // Composable entry point: Koin-defaulted VM/service params, not real call-site burden.
@Composable
fun HomeScreen(
    onStartTracking: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSearch: () -> Unit = onOpenAccount,
    onOpenAgent: (() -> Unit)? = null,
    viewModel: HomeViewModel = koinViewModel(),
    firstLoginBannerViewModel: FirstLoginBannerViewModel = koinViewModel(),
    whatsNewViewModel: WhatsNewViewModel = koinViewModel(),
    reviewTracker: ReviewTracker = koinInject(),
    featureFlags: FeatureFlags = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val welcomeBannerState by firstLoginBannerViewModel.uiState.collectAsStateWithLifecycle()
    val whatsNewState by whatsNewViewModel.uiState.collectAsStateWithLifecycle()

    // PLAN_V24 P13.2: the home banner carousel is fed by plugin-gated typed sources — seeded
    // announcements (baseline) + campaigns (P5.4) + a club promo (P6.1) + an offers promo (P12.9).
    // Impressions log to the local analytics stub. The shared CampaignRepository also feeds the
    // profile marketing hub.
    val campaignRepository = org.koin.compose.koinInject<com.mileway.core.data.campaign.CampaignRepository>()
    val campaignPluginRegistry = org.koin.compose.koinInject<com.mileway.core.data.plugin.PluginRegistry>()
    val campaignMarketingEnabled by campaignPluginRegistry.observe("campaignMarketingEnabled").collectAsStateWithLifecycle(initialValue = false)
    val clubEnabled by campaignPluginRegistry.observe("clubEnabled").collectAsStateWithLifecycle(initialValue = false)
    val offersHubEnabled by campaignPluginRegistry.observe("offersHubEnabled").collectAsStateWithLifecycle(initialValue = false)
    val campaigns by campaignRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    LaunchedEffect(Unit) { campaignRepository.seedIfEmpty() }
    val analytics = com.mileway.core.ui.platform.LocalAnalyticsHelper.current
    val clubBannerTitle = stringResource(Res.string.home_banner_club_title)
    val clubBannerSubtitle = stringResource(Res.string.home_banner_club_subtitle)
    val clubBannerBadge = stringResource(Res.string.home_banner_club_badge)
    val offersBannerTitle = stringResource(Res.string.home_banner_offers_title)
    val offersBannerSubtitle = stringResource(Res.string.home_banner_offers_subtitle)
    val offersBannerBadge = stringResource(Res.string.home_banner_offers_badge)
    val homeBanners =
        buildList {
            state.marketingItems.forEachIndexed { index, item ->
                add(HomeBanner(id = "seed_$index", title = item.title, subtitle = item.subtitle, style = item.badge))
            }
            if (campaignMarketingEnabled) {
                campaigns.forEach {
                    add(HomeBanner(id = "camp_${it.id}", title = it.name, subtitle = it.description, style = it.badge, deepLink = "account"))
                }
            }
            if (clubEnabled) {
                add(HomeBanner(id = "club", title = clubBannerTitle, subtitle = clubBannerSubtitle, style = clubBannerBadge, deepLink = "account"))
            }
            if (offersHubEnabled) {
                add(HomeBanner(id = "offers", title = offersBannerTitle, subtitle = offersBannerSubtitle, style = offersBannerBadge, deepLink = "account"))
            }
        }

    // V15 RV.4/CF.1 + PLAN_V24 P12.3: Home is a meaningful engagement signal — record first-open +
    // an interaction. The review prompt is now the native RateAppSheet (no Play SDK), gated by the
    // reviewPrompt plugin AND the in-app-review flag, and only when the engagement gate is satisfied.
    val reviewPromptEnabled by campaignPluginRegistry.observe("reviewPrompt").collectAsStateWithLifecycle(initialValue = false)
    var showReviewSheet by remember { mutableStateOf(false) }
    // PLAN_V24 P12.4: manual re-open of the "What's new" changelog from the animated home entry.
    var showWhatsNewManually by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        reviewTracker.recordFirstOpenIfNeeded()
        reviewTracker.recordInteraction()
    }
    LaunchedEffect(reviewPromptEnabled) {
        if (reviewPromptEnabled && featureFlags.inAppReviewEnabled && reviewTracker.shouldPrompt()) {
            reviewTracker.markPrompted()
            showReviewSheet = true
        }
    }

    HomeScreenContent(
        state = state,
        onStartTracking = { viewModel.onStartTracking(onStartTracking) },
        onAddExpense = { viewModel.onAddExpense(onAddExpense) },
        onOpenAccount = { viewModel.onOpenAccount(onOpenAccount) },
        onOpenSearch = onOpenSearch,
        onOpenAgent = onOpenAgent,
        welcomeBanner = welcomeBannerState,
        onWelcomeBannerShown = firstLoginBannerViewModel::onBannerShown,
        homeBanners = homeBanners,
        onBannerClick = { banner -> if (banner.deepLink != null) viewModel.onOpenAccount(onOpenAccount) },
        onBannerImpression = { banner, dwellMs ->
            analytics.log(
                com.mileway.core.platform.AnalyticsEvent(
                    type = "home_banner_impression",
                    params = mapOf("id" to banner.id, "dwell_ms" to dwellMs.toString()),
                ),
            )
        },
        // PLAN_V24 P12.4: animated "What's new" entry — pulses while this release is unseen, re-opens
        // the changelog. Nullable in HomeScreenContent so the stateless gallery render omits it.
        whatsNewUnseen = whatsNewState.isVisible,
        onOpenWhatsNew = { showWhatsNewManually = true },
    )

    // PLAN_V24 P12.3: the native "Rate Mileway" sheet shown when the engagement gate is satisfied.
    if (showReviewSheet) {
        RateAppSheet(onDismiss = { showReviewSheet = false })
    }

    // PLAN_V24 P12.9: the one-shot best-offer popup on Home entry — gated by showOfferPopup, over the
    // top LIVE campaign (the same repo the marketing strip reads; no new data). ponytail: session-scoped
    // one-shot (a remember flag) — persistent per-release suppression lands with the P13.3 coordinator.
    val offerPopupEnabled by campaignPluginRegistry.observe("showOfferPopup").collectAsStateWithLifecycle(initialValue = false)
    val bestOffer = campaigns.firstOrNull { it.status == com.mileway.core.data.campaign.CampaignStatus.LIVE }
    var offerPopupSeen by remember { mutableStateOf(false) }
    var showOfferDialog by remember { mutableStateOf(false) }
    LaunchedEffect(offerPopupEnabled, bestOffer) {
        if (offerPopupEnabled && !offerPopupSeen && bestOffer != null) {
            offerPopupSeen = true
            showOfferDialog = true
        }
    }
    if (showOfferDialog && bestOffer != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showOfferDialog = false },
            title = { androidx.compose.material3.Text(bestOffer.name) },
            text = { androidx.compose.material3.Text(bestOffer.description) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showOfferDialog = false }) {
                    androidx.compose.material3.Text(
                        Res.allStringResources["shared_offer_popup_cta"]?.let { stringResource(it) } ?: "Got it",
                    )
                }
            },
        )
    }

    // PLAN_V24 P2.2 + P12.4: the "What's new" sheet — shown once per release after login, or on demand
    // from the animated home entry. Dismissing acknowledges the version so the auto-show never replays.
    if (whatsNewState.isVisible || showWhatsNewManually) {
        WhatsNewSheet(
            items = whatsNewState.items,
            onDismiss = {
                whatsNewViewModel.acknowledge()
                showWhatsNewManually = false
            },
        )
    }
}

/**
 * Stateless body of the Home tab. Split out from [HomeScreen] so it can be exercised with a
 * hand-built [HomeUiState] and plain lambdas, independent of Koin and the ViewModel.
 */
@Suppress("LongParameterList") // Stateless preview/test entry point mirroring HomeScreen's own param set.
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onStartTracking: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSearch: () -> Unit = onOpenAccount,
    onOpenAgent: (() -> Unit)? = null,
    welcomeBanner: FirstLoginBannerUiState = FirstLoginBannerUiState(),
    onWelcomeBannerShown: () -> Unit = {},
    // PLAN_V24 P13.2: typed carousel rows; null falls back to the seeded state.marketingItems (baseline).
    homeBanners: List<HomeBanner>? = null,
    onBannerClick: (HomeBanner) -> Unit = {},
    onBannerImpression: (HomeBanner, Long) -> Unit = { _, _ -> },
    // PLAN_V24 P12.4: the animated "What's new" entry. onOpenWhatsNew == null (the default, used by the
    // stateless preview/gallery render) hides it entirely, keeping the home golden byte-identical.
    whatsNewUnseen: Boolean = false,
    onOpenWhatsNew: (() -> Unit)? = null,
) {
    // Clear the one-shot flag right after the banner is actually composed once — not on every
    // recomposition of the Home tab, so it never reappears on scroll/rotation before sign-out.
    LaunchedEffect(welcomeBanner.isVisible) {
        if (welcomeBanner.isVisible) onWelcomeBannerShown()
    }

    val scrollState = rememberScrollState()
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val illustrativeSnackbar: suspend () -> Unit = { snackbarState.showSnackbar("Detail view available in full version.") }
    val paymentsSnackbar: suspend () -> Unit = { snackbarState.showSnackbar("Payments require network in production.") }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                // Top-level tab: own the background so the body renders on the theme surface
                // (deep-dark under the Matrix default) instead of inheriting whatever the host
                // happens to paint. Without this the scrollable body is transparent and falls
                // through to the window/canvas colour.
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
        ) {
            // 1. Gradient header (draws behind the status bar; owns the top inset).
            HomeProfileHeader(
                name = state.greetingName,
                notificationCount = state.notificationCount,
                onSearch = onOpenSearch,
                onNotifications = onOpenAccount,
                onOpenAgent = onOpenAgent,
                currentPin = state.currentPin,
            )

            // 2. Animated banner strip (rotating 4000ms; replaces static ActionRequired card).
            AnimatedBannerStrip(isTrackingActive = false)

            Column(
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sectionSpacing),
            ) {
                Spacer(Modifier.height(DesignTokens.Spacing.l))

                // 2b. One-time post-sign-in welcome banner (PLAN_V22 P7.1) — shows once, then
                //     the flag is cleared and it never reappears until the next fresh sign-in.
                if (welcomeBanner.isVisible) {
                    WelcomeBanner(
                        displayName = welcomeBanner.displayName,
                        officeName = welcomeBanner.officeName,
                        onDismiss = onWelcomeBannerShown,
                    )
                }

                // 2c. PLAN_V24 P12.4: animated "What's new" entry (only in the stateful HomeScreen).
                onOpenWhatsNew?.let { open ->
                    WhatsNewAnimatedButton(hasUnseen = whatsNewUnseen, onClick = open)
                }

                // 3. Quick Actions.
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    HomeSectionHeader(title = stringResource(Res.string.shared_home_quick_actions), leadingIcon = Icons.Filled.Bolt)
                    QuickActionsRow(
                        actions =
                            quickActions(
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
                    HomeSectionHeader(title = stringResource(Res.string.shared_home_at_a_glance), leadingIcon = Icons.Filled.Insights)
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

            // 8. PLAN_V24 P13.2: the ONE auto-advancing home banner carousel (supersedes the P5.4
            // marketing strip). Fed by plugin-gated typed sources in the stateful HomeScreen; falls
            // back to the seeded state.marketingItems here (keeps the stateless golden byte-identical).
            BannerCarousel(
                items =
                    homeBanners ?: state.marketingItems.mapIndexed { index, item ->
                        HomeBanner(id = "seed_$index", title = item.title, subtitle = item.subtitle, style = item.badge)
                    },
                onBannerClick = onBannerClick,
                onImpression = onBannerImpression,
            )

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
    val cards =
        remember(onStartTracking, onIllustrative) {
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
