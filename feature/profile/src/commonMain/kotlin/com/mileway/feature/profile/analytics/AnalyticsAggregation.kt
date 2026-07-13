package com.mileway.feature.profile.analytics

import com.mileway.stub.DailySpend
import com.mileway.stub.MerchantTransaction
import com.mileway.stub.RecentActivityItem
import com.mileway.stub.TeamMember
import kotlin.math.roundToInt

/** PLAN_V29 P29.AN.2: date-range presets driving both the overview and detail-screen charts. */
enum class DateRangePreset(val days: Int) {
    LAST_7(7),
    LAST_30(30),
    LAST_90(90),
    CUSTOM(0),
}

/** PLAN_V29 P29.AN.5: metric toggle for the trend chart (amount vs transaction count). */
enum class AnalyticsMetric { AMOUNT, COUNT }

/** PLAN_V29 P29.AN.8: leaderboard sort options. */
enum class LeaderboardSort { HIGHEST_SPEND, MOST_CLAIMS, ALPHABETICAL }

/** PLAN_V29 P29.AN.9: kept richer than DiCE's two-type system (gap: "none" — do not shrink). */
enum class InsightType { ANOMALY, BREACH_RISK, PATTERN, SAVINGS }

data class InsightCard(val id: String, val title: String, val body: String, val type: InsightType)

data class PeriodDelta(val currentTotal: Double, val previousTotal: Double, val percentChange: Double, val isIncrease: Boolean)

// ponytail: String.format is JVM-only; commonMain needs its own fixed-decimal formatter.
internal fun Double.toFixed(decimals: Int): String {
    val factor = List(decimals) { 10 }.fold(1) { acc, ten -> acc * ten }
    val scaled = (this * factor).roundToInt()
    val whole = scaled / factor
    val frac = kotlin.math.abs(scaled % factor)
    return if (decimals == 0) "$whole" else "$whole.${frac.toString().padStart(decimals, '0')}"
}

/**
 * Pure, ViewModel-agnostic aggregation logic for the analytics feature — kept dependency-free so
 * every rule (date windowing, filtering, sorting, CSV export, insight derivation) is unit-testable
 * without a Compose/ViewModel harness.
 */
object AnalyticsAggregator {
    /** Clips a preset (or explicit custom start/end) down to the days actually available in [series]. */
    fun windowLength(
        preset: DateRangePreset,
        series: List<DailySpend>,
        customStart: Long? = null,
        customEnd: Long? = null,
    ): Int =
        when (preset) {
            DateRangePreset.CUSTOM -> {
                if (customStart == null || customEnd == null || customEnd < customStart) {
                    series.size
                } else {
                    val dayMs = 86_400_000L
                    (((customEnd - customStart) / dayMs) + 1).toInt().coerceIn(1, series.size)
                }
            }
            else -> preset.days.coerceAtMost(series.size)
        }

    fun windowedSeries(
        series: List<DailySpend>,
        preset: DateRangePreset,
        customStart: Long? = null,
        customEnd: Long? = null,
    ): List<DailySpend> {
        if (preset == DateRangePreset.CUSTOM && customStart != null && customEnd != null && customEnd >= customStart) {
            return series.filter { it.dateMs in customStart..customEnd }.ifEmpty { series.takeLast(windowLength(preset, series, customStart, customEnd)) }
        }
        return series.takeLast(windowLength(preset, series))
    }

    /** Real period-over-period delta: current window total vs the same-length window from [priorSeries]. */
    fun periodDelta(
        currentWindow: List<DailySpend>,
        priorSeries: List<DailySpend>,
    ): PeriodDelta {
        val currentTotal = currentWindow.sumOf { it.amountRupees }
        val previousTotal = priorSeries.takeLast(currentWindow.size).sumOf { it.amountRupees }
        val percentChange = if (previousTotal == 0.0) 0.0 else ((currentTotal - previousTotal) / previousTotal) * 100.0
        return PeriodDelta(currentTotal, previousTotal, percentChange, percentChange >= 0.0)
    }

    /** The single highest-amount day in [series], for the trend chart's peak annotation. */
    fun peakDay(series: List<DailySpend>): DailySpend? = series.maxByOrNull { it.amountRupees }

    fun filterActivity(
        items: List<RecentActivityItem>,
        categories: Set<String>,
        statuses: Set<String>,
        paymentMethods: Set<String>,
    ): List<RecentActivityItem> =
        items.filter { item ->
            (categories.isEmpty() || item.category in categories) &&
                (statuses.isEmpty() || item.status in statuses) &&
                (paymentMethods.isEmpty() || item.paymentMethod in paymentMethods)
        }

    fun sortLeaderboard(
        members: List<TeamMember>,
        sort: LeaderboardSort,
        query: String,
    ): List<TeamMember> {
        val filtered =
            if (query.isBlank()) {
                members
            } else {
                members.filter { it.name.contains(query, ignoreCase = true) || it.topCategory.contains(query, ignoreCase = true) }
            }
        return when (sort) {
            LeaderboardSort.HIGHEST_SPEND -> filtered.sortedByDescending { it.amountRupees }
            LeaderboardSort.MOST_CLAIMS -> filtered.sortedByDescending { it.claimCount }
            LeaderboardSort.ALPHABETICAL -> filtered.sortedBy { it.name }
        }
    }

    fun searchTransactions(
        transactions: List<MerchantTransaction>,
        query: String,
    ): List<MerchantTransaction> =
        if (query.isBlank()) {
            transactions
        } else {
            transactions.filter { it.merchantName.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true) }
        }

    /** PLAN_V29 P29.AN.7: local CSV artifact — replaces the fake "Exporting…" snackbar. */
    fun generateCsv(
        category: String,
        series: List<DailySpend>,
        merchants: List<com.mileway.stub.MerchantTotal>,
    ): String =
        buildString {
            appendLine("$category analytics export")
            appendLine()
            appendLine("Date,Amount (INR),Transactions")
            series.forEach { day -> appendLine("${day.dayLabel},${day.amountRupees.toFixed(2)},${day.transactionCount}") }
            appendLine()
            appendLine("Merchant,Amount (INR)")
            merchants.forEach { m -> appendLine("${m.name},${m.amountRupees.toFixed(2)}") }
        }

    /**
     * PLAN_V29 P29.AN.9: derives the four insight cards from real mock aggregates. Keeps Mileway's
     * richer 4-archetype taxonomy (gap: "none" vs DiCE's 2-type system) — only the copy becomes real.
     */
    fun deriveInsights(
        teamMembers: List<TeamMember>,
        pendingApprovalDays: List<Int>,
        slaBreachThresholdDays: Int,
        dailySeries: List<DailySpend>,
        compliancePercent: Int,
        categoryTotals: Map<String, Double>,
    ): List<InsightCard> {
        val avgSpend = teamMembers.map { it.amountRupees }.average()
        val topSpender = teamMembers.maxByOrNull { it.amountRupees }
        val anomalyRatio = if (topSpender != null && avgSpend > 0) topSpender.amountRupees / avgSpend else 0.0

        val breaching = pendingApprovalDays.count { it > slaBreachThresholdDays }

        val byWeekday = dailySeries.groupBy { it.dayLabel }.mapValues { (_, days) -> days.sumOf { it.amountRupees } }
        val peakWeekday = byWeekday.maxByOrNull { it.value }

        val travelTotal = categoryTotals["Travel"] ?: 0.0
        val savingsAmount = travelTotal * ((100 - compliancePercent) / 100.0 * 0.5 + 0.1)

        return listOf(
            InsightCard(
                "I001",
                "Unusual Travel Spend",
                "${topSpender?.name ?: "A team member"}'s spend is ${anomalyRatio.toFixed(1)}x above the team average this month.",
                InsightType.ANOMALY,
            ),
            InsightCard(
                "I002",
                "SLA Breach Risk",
                "$breaching claim(s) have been pending approval for over $slaBreachThresholdDays days: expected SLA breach soon.",
                InsightType.BREACH_RISK,
            ),
            InsightCard(
                "I003",
                "Submission Pattern",
                if (peakWeekday != null) {
                    "Spend peaks on ${peakWeekday.key}s (₹${peakWeekday.value.roundToInt()} total). Consider scheduling batch review reminders."
                } else {
                    "No clear weekday pattern detected yet."
                },
                InsightType.PATTERN,
            ),
            InsightCard(
                "I004",
                "Savings Identified",
                "Policy-compliant travel selections saved ₹${savingsAmount.roundToInt()} this quarter compared to category average.",
                InsightType.SAVINGS,
            ),
        )
    }
}
