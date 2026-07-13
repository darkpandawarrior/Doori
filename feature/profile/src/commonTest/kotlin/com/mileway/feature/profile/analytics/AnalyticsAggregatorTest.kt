package com.mileway.feature.profile.analytics

import com.mileway.stub.AnalyticsMockData
import com.mileway.stub.DailySpend
import com.mileway.stub.MerchantTransaction
import com.mileway.stub.RecentActivityItem
import com.mileway.stub.TeamMember
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V29 P29.AN.1-9: pure aggregation-logic coverage for the analytics feature — date windowing,
 * period-over-period delta, filtering, leaderboard sort/search, CSV export, and insight derivation.
 * All functions under test are dependency-free (no ViewModel/Compose harness needed).
 */
class AnalyticsAggregatorTest {
    private fun day(
        i: Int,
        amount: Double,
        label: String = "Mon",
        count: Int = 1,
    ) = DailySpend(dateMs = i * 86_400_000L, amountRupees = amount, dayLabel = label, transactionCount = count)

    // ── windowLength / windowedSeries ─────────────────────────────────────────

    @Test
    fun `windowLength clips preset days to available series size`() {
        val series = List(10) { day(it, 100.0) }
        assertEquals(7, AnalyticsAggregator.windowLength(DateRangePreset.LAST_7, series))
        assertEquals(10, AnalyticsAggregator.windowLength(DateRangePreset.LAST_90, series))
    }

    @Test
    fun `windowedSeries returns the last N days for a preset`() {
        val series = List(30) { day(it, it.toDouble()) }
        val window = AnalyticsAggregator.windowedSeries(series, DateRangePreset.LAST_7)
        assertEquals(7, window.size)
        assertEquals(23.0, window.first().amountRupees)
        assertEquals(29.0, window.last().amountRupees)
    }

    @Test
    fun `windowedSeries with custom range filters by dateMs bounds`() {
        val series = List(10) { day(it, 100.0) }
        val window = AnalyticsAggregator.windowedSeries(series, DateRangePreset.CUSTOM, customStart = 2 * 86_400_000L, customEnd = 4 * 86_400_000L)
        assertEquals(3, window.size)
    }

    @Test
    fun `windowedSeries with invalid custom range falls back to full series`() {
        val series = List(5) { day(it, 100.0) }
        val window = AnalyticsAggregator.windowedSeries(series, DateRangePreset.CUSTOM, customStart = null, customEnd = null)
        assertEquals(5, window.size)
    }

    // ── periodDelta ──────────────────────────────────────────────────────────

    @Test
    fun `periodDelta computes a real percent change against the prior window`() {
        val current = listOf(day(0, 100.0), day(1, 100.0)) // total 200
        val prior = listOf(day(0, 50.0), day(1, 50.0)) // total 100
        val delta = AnalyticsAggregator.periodDelta(current, prior)
        assertEquals(200.0, delta.currentTotal)
        assertEquals(100.0, delta.previousTotal)
        assertEquals(100.0, delta.percentChange, 0.001)
        assertTrue(delta.isIncrease)
    }

    @Test
    fun `periodDelta reports a decrease when spend drops`() {
        val current = listOf(day(0, 50.0))
        val prior = listOf(day(0, 100.0))
        val delta = AnalyticsAggregator.periodDelta(current, prior)
        assertEquals(-50.0, delta.percentChange, 0.001)
        assertTrue(!delta.isIncrease)
    }

    @Test
    fun `periodDelta handles a zero previous total without dividing by zero`() {
        val delta = AnalyticsAggregator.periodDelta(listOf(day(0, 50.0)), listOf(day(0, 0.0)))
        assertEquals(0.0, delta.percentChange)
    }

    @Test
    fun `real mock data has a distinct prior period baseline`() {
        val current = AnalyticsMockData.weeklySeries
        val delta = AnalyticsAggregator.periodDelta(current, AnalyticsMockData.priorPeriodSeries)
        assertTrue(delta.previousTotal > 0.0)
        assertTrue(delta.currentTotal != delta.previousTotal)
    }

    // ── peakDay ──────────────────────────────────────────────────────────────

    @Test
    fun `peakDay returns the highest-amount entry`() {
        val series = listOf(day(0, 10.0), day(1, 500.0), day(2, 50.0))
        assertEquals(500.0, AnalyticsAggregator.peakDay(series)?.amountRupees)
    }

    // ── filterActivity ───────────────────────────────────────────────────────

    private fun activity(
        category: String,
        status: String,
        payment: String,
    ) = RecentActivityItem("t", "s", 10.0, 0L, category, status, payment)

    @Test
    fun `filterActivity with no filters returns everything`() {
        val items = listOf(activity("Mileage", "Approved", "Card"), activity("Travel", "Pending", "UPI"))
        assertEquals(2, AnalyticsAggregator.filterActivity(items, emptySet(), emptySet(), emptySet()).size)
    }

    @Test
    fun `filterActivity narrows by category status and payment method together`() {
        val items =
            listOf(
                activity("Mileage", "Approved", "Card"),
                activity("Mileage", "Pending", "Card"),
                activity("Travel", "Approved", "Card"),
            )
        val filtered = AnalyticsAggregator.filterActivity(items, setOf("Mileage"), setOf("Approved"), setOf("Card"))
        assertEquals(1, filtered.size)
    }

    // ── leaderboard sort/search ──────────────────────────────────────────────

    private val members =
        listOf(
            TeamMember("Aisha Khan", 15_600.0, 8, "Travel"),
            TeamMember("Priya Sharma", 12_400.0, 6, "Expense"),
            TeamMember("Neha Patel", 9_800.0, 20, "Mileage"),
        )

    @Test
    fun `sortLeaderboard highest spend sorts descending by amount`() {
        val sorted = AnalyticsAggregator.sortLeaderboard(members, LeaderboardSort.HIGHEST_SPEND, "")
        assertEquals(listOf("Aisha Khan", "Priya Sharma", "Neha Patel"), sorted.map { it.name })
    }

    @Test
    fun `sortLeaderboard most claims sorts descending by claimCount`() {
        val sorted = AnalyticsAggregator.sortLeaderboard(members, LeaderboardSort.MOST_CLAIMS, "")
        assertEquals("Neha Patel", sorted.first().name)
    }

    @Test
    fun `sortLeaderboard alphabetical sorts by name`() {
        val sorted = AnalyticsAggregator.sortLeaderboard(members, LeaderboardSort.ALPHABETICAL, "")
        assertEquals(listOf("Aisha Khan", "Neha Patel", "Priya Sharma"), sorted.map { it.name })
    }

    @Test
    fun `sortLeaderboard query filters by name or top category case-insensitively`() {
        val filtered = AnalyticsAggregator.sortLeaderboard(members, LeaderboardSort.HIGHEST_SPEND, "travel")
        assertEquals(listOf("Aisha Khan"), filtered.map { it.name })
    }

    // ── merchant transaction search ──────────────────────────────────────────

    @Test
    fun `searchTransactions with blank query returns all`() {
        val txns = listOf(MerchantTransaction("A-1", "Merchant A", 0L, 10.0, "Approved"))
        assertEquals(1, AnalyticsAggregator.searchTransactions(txns, "").size)
    }

    @Test
    fun `searchTransactions matches by id or merchant name`() {
        val txns =
            listOf(
                MerchantTransaction("ABC-1", "Merchant A", 0L, 10.0, "Approved"),
                MerchantTransaction("XYZ-2", "Merchant B", 0L, 20.0, "Approved"),
            )
        assertEquals(1, AnalyticsAggregator.searchTransactions(txns, "abc").size)
        assertEquals(1, AnalyticsAggregator.searchTransactions(txns, "Merchant B").size)
        assertEquals(0, AnalyticsAggregator.searchTransactions(txns, "nope").size)
    }

    // ── CSV export ───────────────────────────────────────────────────────────

    @Test
    fun `generateCsv includes header, every day, and every merchant`() {
        val series = listOf(day(0, 100.0, "Mon"), day(1, 200.0, "Tue"))
        val merchants = AnalyticsMockData.merchantsForCategory("Mileage")
        val csv = AnalyticsAggregator.generateCsv("Mileage", series, merchants)
        assertTrue(csv.contains("Date,Amount (INR),Transactions"))
        assertTrue(csv.contains("Mon,100.00,1"))
        assertTrue(csv.contains("Tue,200.00,1"))
        merchants.forEach { assertTrue(csv.contains(it.name)) }
    }

    // ── insight derivation ───────────────────────────────────────────────────

    @Test
    fun `deriveInsights returns all four archetypes with non-blank copy`() {
        val insights =
            AnalyticsAggregator.deriveInsights(
                AnalyticsMockData.teamMembers,
                AnalyticsMockData.pendingApprovalDays,
                AnalyticsMockData.slaBreachThresholdDays,
                AnalyticsMockData.dailySeries,
                AnalyticsMockData.compliancePercent,
                AnalyticsMockData.categoryTotals,
            )
        assertEquals(setOf(InsightType.ANOMALY, InsightType.BREACH_RISK, InsightType.PATTERN, InsightType.SAVINGS), insights.map { it.type }.toSet())
        insights.forEach { assertTrue(it.body.isNotBlank()) }
    }

    @Test
    fun `deriveInsights breach count reflects the SLA threshold`() {
        val insights =
            AnalyticsAggregator.deriveInsights(
                AnalyticsMockData.teamMembers,
                listOf(10, 1, 8),
                slaBreachThresholdDays = 5,
                AnalyticsMockData.dailySeries,
                AnalyticsMockData.compliancePercent,
                AnalyticsMockData.categoryTotals,
            )
        val breach = insights.first { it.type == InsightType.BREACH_RISK }
        assertTrue(breach.body.contains("2 claim"))
    }
}
