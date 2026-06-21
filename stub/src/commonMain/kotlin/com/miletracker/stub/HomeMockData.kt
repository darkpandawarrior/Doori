package com.miletracker.stub

/**
 * Home-screen "At a Glance" counters. Shell-only model — these counts feed the
 * dashboard summary cards and never round-trip through the network layer.
 */
data class AtAGlanceCounts(
    val unreportedTransactions: Int,
    val upcomingTrips: Int,
    val pendingInvoices: Int,
    val pendingExpenses: Int,
    val vouchersToFile: Int,
    val pendingApprovals: Int = 3,
    val unreadNotifications: Int = 4,
)

/** Banner shown at the top of the home screen when items need user action. */
data class ActionRequiredBanner(
    val amountText: String,
    val count: Int,
    val message: String,
)

/** A single card in the home-screen marketing carousel. */
data class MarketingCarouselItem(
    val title: String,
    val subtitle: String,
    val badge: String,
)

/**
 * Obviously-fake auth token pair so the shell can satisfy any "is logged in" checks
 * without ever talking to a real auth backend.
 */
data class DemoAuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Fixed, deterministic mock data for the home/shell screens. Every value here is a
 * constant — no time, no randomness — so UI snapshots and unit tests stay stable.
 */
object HomeMockData {
    // Counts are kept internally consistent with the other home signals: pendingExpenses
    // matches the "9 expenses awaiting" action banner, and unreadNotifications matches the
    // header bell's notificationCount() (4). Bug 5 — the At A Glance tile reads as one
    // coherent snapshot rather than four unrelated figures.
    fun atAGlance(): AtAGlanceCounts =
        AtAGlanceCounts(
            unreportedTransactions = 18,
            upcomingTrips = 5,
            pendingInvoices = 7,
            pendingExpenses = 9,
            vouchersToFile = 6,
            pendingApprovals = 3,
            unreadNotifications = 4,
        )

    fun actionRequiredBanner(): ActionRequiredBanner =
        ActionRequiredBanner(
            amountText = "₹12,480.00",
            count = 9,
            message = "9 expenses are awaiting your action",
        )

    fun carouselItems(): List<MarketingCarouselItem> =
        listOf(
            MarketingCarouselItem(
                title = "Track every mile, automatically",
                subtitle = "Background tracking captures your business trips hands-free",
                badge = "NEW",
            ),
            MarketingCarouselItem(
                title = "Snap receipts on the go",
                subtitle = "Receipt capture extracts amount, date and merchant for you",
                badge = "BETA",
            ),
            MarketingCarouselItem(
                title = "Monthly mileage report",
                subtitle = "One tap to export everything your finance team needs",
                badge = "TIP",
            ),
        )

    fun notificationCount(): Int = 4

    fun authTokens(): DemoAuthTokens =
        DemoAuthTokens(
            accessToken = "demo-access-token-not-a-real-credential",
            refreshToken = "demo-refresh-token-not-a-real-credential",
        )
}
