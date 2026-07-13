package com.mileway.stub

private const val BASE_MS = 1_781_654_400_000L
private const val DAY_MS = 86_400_000L

data class DailySpend(
    val dateMs: Long,
    val amountRupees: Double,
    val dayLabel: String,
    val transactionCount: Int = 1,
)

data class RecentActivityItem(
    val title: String,
    val subtitle: String,
    val amountRupees: Double,
    val dateMs: Long,
    val category: String,
    val status: String = "Approved",
    val paymentMethod: String = "Card",
)

data class MerchantTotal(val name: String, val amountRupees: Double)

data class MerchantTransaction(
    val id: String,
    val merchantName: String,
    val dateMs: Long,
    val amountRupees: Double,
    val status: String,
)

data class TeamMember(val name: String, val amountRupees: Double, val claimCount: Int, val topCategory: String)

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private val RAW_AMOUNTS =
    listOf(
        1200.0, 3400.0, 800.0, 5600.0, 2100.0, 900.0, 4300.0,
        1800.0, 7200.0, 600.0, 3100.0, 4800.0, 2200.0, 1500.0,
        8900.0, 400.0, 3700.0, 2600.0, 5500.0, 1100.0, 6800.0,
        900.0, 2400.0, 3300.0, 7100.0, 1700.0, 4200.0, 2800.0,
        5900.0, 3600.0,
    )

// PLAN_V29 P29.AN.6: baseline for period-over-period comparison — a distinct 30-day window
// immediately preceding `dailySeries`, ~12% lower on average, so "vs previous period" is a real
// diff against stored data rather than a hardcoded percentage string.
private val PRIOR_RAW_AMOUNTS = RAW_AMOUNTS.map { it * 0.88 }

object AnalyticsMockData {
    val dailySeries: List<DailySpend> =
        RAW_AMOUNTS.mapIndexed { i, amount ->
            DailySpend(
                dateMs = BASE_MS + (i * DAY_MS),
                amountRupees = amount,
                dayLabel = DAY_LABELS[i % 7],
                transactionCount = 1 + (i % 4),
            )
        }

    // Same length/day-alignment as dailySeries, ending the day before it starts — used only as the
    // "previous period" baseline for whatever window length is currently selected.
    val priorPeriodSeries: List<DailySpend> =
        PRIOR_RAW_AMOUNTS.mapIndexed { i, amount ->
            DailySpend(
                dateMs = BASE_MS - (RAW_AMOUNTS.size - i) * DAY_MS,
                amountRupees = amount,
                dayLabel = DAY_LABELS[i % 7],
                transactionCount = 1 + (i % 4),
            )
        }

    val weeklySeries: List<DailySpend> = dailySeries.takeLast(7)

    val categoryTotals: Map<String, Double> =
        mapOf(
            "Mileage" to 18400.0,
            "Expense" to 24700.0,
            "Travel" to 12300.0,
            "Advance" to 48500.0,
        )

    val totalSpend: Double = categoryTotals.values.sum()

    val compliancePercent: Int = 87
    val violationCount: Int = 3
    val hardStopCount: Int = 1

    // PLAN_V29 P29.AN.9: backs the "SLA Breach Risk" insight card with a real threshold check
    // instead of a hardcoded "3 claims" string.
    val pendingApprovalDays: List<Int> = listOf(2, 6, 7, 1)
    val slaBreachThresholdDays: Int = 5

    val topMerchants: List<MerchantTotal> =
        listOf(
            MerchantTotal("TechVision Pvt Ltd", 25000.0),
            MerchantTotal("GoFast Logistics", 12800.0),
            MerchantTotal("OfficePlex Supplies", 9400.0),
            MerchantTotal("Zomato Business", 6200.0),
            MerchantTotal("Swiggy Corporate", 4100.0),
        )

    val recentActivity: List<RecentActivityItem> =
        listOf(
            RecentActivityItem("Trip to Client Site", "Mileage · 47 km", 1410.0, BASE_MS + 29 * DAY_MS, "Mileage", "Approved", "Card"),
            RecentActivityItem("Team Dinner", "Expense · Food", 3200.0, BASE_MS + 28 * DAY_MS, "Expense", "Pending", "UPI"),
            RecentActivityItem("Flight PNQ-BOM", "Travel", 5500.0, BASE_MS + 27 * DAY_MS, "Travel", "Approved", "Card"),
            RecentActivityItem("Office Supplies", "Expense · Office", 900.0, BASE_MS + 26 * DAY_MS, "Expense", "Rejected", "Cash"),
            RecentActivityItem("Client Visit", "Mileage · 32 km", 960.0, BASE_MS + 25 * DAY_MS, "Mileage", "Approved", "UPI"),
        )

    val quickInsights: List<String> =
        listOf(
            "₹3,459 avg this week",
            "3 trips pending approval",
            "₹24,700 on expenses",
            "87% policy compliant",
        )

    // PLAN_V29 P29.AN.8: team leaderboard rows — moved out of the screen (was UI-local) so the
    // ViewModel can sort/search/aggregate against it.
    val teamMembers: List<TeamMember> =
        listOf(
            TeamMember("Aisha Khan", 15_600.0, 8, "Travel"),
            TeamMember("Priya Sharma", 12_400.0, 6, "Expense"),
            TeamMember("Neha Patel", 9_800.0, 5, "Mileage"),
            TeamMember("Rahul Mehra", 8_900.0, 4, "Expense"),
            TeamMember("Vikram Nair", 6_200.0, 3, "Advance"),
        )

    fun seriesForCategory(category: String): List<DailySpend> {
        val scale =
            when (category) {
                "Mileage" -> 0.18
                "Expense" -> 0.24
                "Travel" -> 0.12
                "Advance" -> 0.46
                else -> 0.25
            }
        return dailySeries.map { it.copy(amountRupees = it.amountRupees * scale) }
    }

    fun merchantsForCategory(category: String): List<MerchantTotal> =
        when (category) {
            "Mileage" ->
                listOf(
                    MerchantTotal("Client Site A", 4200.0),
                    MerchantTotal("Airport Route", 3800.0),
                    MerchantTotal("Vendor Visit", 2900.0),
                    MerchantTotal("Office Commute", 2100.0),
                    MerchantTotal("Field Trip", 1800.0),
                )
            "Travel" ->
                listOf(
                    MerchantTotal("IndiGo Airlines", 6200.0),
                    MerchantTotal("Marriott Hotels", 4500.0),
                    MerchantTotal("Ola Rentals", 1600.0),
                    MerchantTotal("Uber Intercity", 800.0),
                    MerchantTotal("IRCTC", 650.0),
                )
            "Advance" ->
                listOf(
                    MerchantTotal("ADV-001 Field Ops", 8000.0),
                    MerchantTotal("ADV-002 Project", 15000.0),
                    MerchantTotal("ADV-003 Conference", 5500.0),
                    MerchantTotal("ADV-004 Travel", 25000.0),
                )
            else -> topMerchants
        }

    // PLAN_V29 P29.AN.4: itemized transactions backing the merchant drill-down list + client-side
    // search. Deterministic split of the merchant's total across a handful of dated line items.
    fun transactionsForMerchant(
        category: String,
        merchantName: String,
    ): List<MerchantTransaction> {
        val merchant = merchantsForCategory(category).find { it.name == merchantName } ?: return emptyList()
        val statuses = listOf("Approved", "Approved", "Pending", "Approved", "Rejected", "Approved")
        val count = 3 + (merchantName.length % 4)
        val perTxn = merchant.amountRupees / count
        return (0 until count).map { i ->
            MerchantTransaction(
                id = "${merchantName.take(3).uppercase()}-${1000 + i}",
                merchantName = merchantName,
                dateMs = BASE_MS + (29 - i * 3).coerceAtLeast(0) * DAY_MS,
                amountRupees = perTxn * (0.8 + 0.1 * (i % 3)),
                status = statuses[i % statuses.size],
            )
        }
    }
}
