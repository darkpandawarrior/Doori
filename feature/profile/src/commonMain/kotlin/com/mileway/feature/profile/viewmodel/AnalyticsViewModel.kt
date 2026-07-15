package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.platform.ShareSheet
import com.mileway.feature.profile.analytics.AnalyticsAggregator
import com.mileway.feature.profile.analytics.AnalyticsMetric
import com.mileway.feature.profile.analytics.DateRangePreset
import com.mileway.feature.profile.analytics.InsightCard
import com.mileway.feature.profile.analytics.LeaderboardSort
import com.mileway.feature.profile.analytics.PeriodDelta
import com.mileway.stub.AnalyticsMockData
import com.mileway.stub.DailySpend
import com.mileway.stub.MerchantTotal
import com.mileway.stub.MerchantTransaction
import com.mileway.stub.RecentActivityItem
import com.mileway.stub.TeamMember
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.launch

/**
 * PLAN_V29 P29.AN.1: single state layer for both [com.mileway.feature.profile.ui.screens.AnalyticsHomeScreen]
 * (My Spend / Team / Insights tabs) and [com.mileway.feature.profile.ui.screens.AnalyticsDetailScreen]
 * (per-category drill-down). Previously both screens read [AnalyticsMockData] directly via local
 * `remember` — this replaces that with an MVI ViewModel so date-range/filters/metric-toggle/leaderboard
 * sort/export all funnel through one reducer. Blocks every other AN row per PLAN_V29.
 */
data class AnalyticsUiState(
    val dateRangePreset: DateRangePreset = DateRangePreset.LAST_30,
    val customRangeStart: Long? = null,
    val customRangeEnd: Long? = null,
    val selectedCategories: Set<String> = emptySet(),
    val selectedStatuses: Set<String> = emptySet(),
    val selectedPaymentMethods: Set<String> = emptySet(),
    val metric: AnalyticsMetric = AnalyticsMetric.AMOUNT,
    val leaderboardSort: LeaderboardSort = LeaderboardSort.HIGHEST_SPEND,
    val leaderboardQuery: String = "",
    val detailCategory: String? = null,
    val selectedMerchant: String? = null,
    val merchantSearchQuery: String = "",
    val isExporting: Boolean = false,
    val exportError: String? = null,
    val exportedForCategory: String? = null,
    // Derived, recomputed by the reducer on every relevant action:
    val windowedSeries: List<DailySpend> = AnalyticsMockData.weeklySeries,
    val categoryTotals: Map<String, Double> = AnalyticsMockData.categoryTotals,
    val totalSpend: Double = AnalyticsMockData.totalSpend,
    val filteredActivity: List<RecentActivityItem> = AnalyticsMockData.recentActivity,
    val periodDelta: PeriodDelta = PeriodDelta(0.0, 0.0, 0.0, true),
    val peakDay: DailySpend? = null,
    val leaderboard: List<TeamMember> = AnalyticsMockData.teamMembers,
    val insights: List<InsightCard> = emptyList(),
    val detailSeries: List<DailySpend> = emptyList(),
    val detailMerchants: List<MerchantTotal> = emptyList(),
    val merchantTransactions: List<MerchantTransaction> = emptyList(),
)

sealed interface AnalyticsAction {
    data class DateRangeChanged(val preset: DateRangePreset) : AnalyticsAction

    data class CustomRangeChanged(val start: Long, val end: Long) : AnalyticsAction

    data class CategoryToggled(val category: String) : AnalyticsAction

    data class StatusToggled(val status: String) : AnalyticsAction

    data class PaymentMethodToggled(val method: String) : AnalyticsAction

    data object ClearFilters : AnalyticsAction

    data class MetricChanged(val metric: AnalyticsMetric) : AnalyticsAction

    data class LeaderboardSortChanged(val sort: LeaderboardSort) : AnalyticsAction

    data class LeaderboardQueryChanged(val query: String) : AnalyticsAction

    data class OpenCategoryDetail(val category: String) : AnalyticsAction

    data class SelectMerchant(val merchant: String?) : AnalyticsAction

    data class MerchantSearchQueryChanged(val query: String) : AnalyticsAction

    data class Export(val category: String) : AnalyticsAction

    data object ExportErrorCleared : AnalyticsAction
}

sealed interface AnalyticsEffect

class AnalyticsViewModel(
    private val shareSheet: ShareSheet,
) : BaseViewModel<AnalyticsUiState, AnalyticsEffect, AnalyticsAction>(recomputed(AnalyticsUiState())) {
    override fun onAction(action: AnalyticsAction) {
        when (action) {
            is AnalyticsAction.DateRangeChanged ->
                setState { recomputed(copy(dateRangePreset = action.preset, customRangeStart = null, customRangeEnd = null)) }
            is AnalyticsAction.CustomRangeChanged ->
                setState { recomputed(copy(dateRangePreset = DateRangePreset.CUSTOM, customRangeStart = action.start, customRangeEnd = action.end)) }
            is AnalyticsAction.CategoryToggled ->
                setState { recomputed(copy(selectedCategories = selectedCategories.toggle(action.category))) }
            is AnalyticsAction.StatusToggled ->
                setState { recomputed(copy(selectedStatuses = selectedStatuses.toggle(action.status))) }
            is AnalyticsAction.PaymentMethodToggled ->
                setState { recomputed(copy(selectedPaymentMethods = selectedPaymentMethods.toggle(action.method))) }
            AnalyticsAction.ClearFilters ->
                setState { recomputed(copy(selectedCategories = emptySet(), selectedStatuses = emptySet(), selectedPaymentMethods = emptySet())) }
            is AnalyticsAction.MetricChanged -> setState { copy(metric = action.metric) }
            is AnalyticsAction.LeaderboardSortChanged ->
                setState { recomputed(copy(leaderboardSort = action.sort)) }
            is AnalyticsAction.LeaderboardQueryChanged ->
                setState { recomputed(copy(leaderboardQuery = action.query)) }
            is AnalyticsAction.OpenCategoryDetail ->
                setState { recomputedDetail(copy(detailCategory = action.category, selectedMerchant = null, merchantSearchQuery = "")) }
            is AnalyticsAction.SelectMerchant ->
                setState { recomputedDetail(copy(selectedMerchant = action.merchant, merchantSearchQuery = "")) }
            is AnalyticsAction.MerchantSearchQueryChanged ->
                setState { recomputedDetail(copy(merchantSearchQuery = action.query)) }
            is AnalyticsAction.Export -> export(action.category)
            AnalyticsAction.ExportErrorCleared -> setState { copy(exportError = null) }
        }
    }

    private fun export(category: String) {
        setState { copy(isExporting = true, exportError = null) }
        viewModelScope.launch {
            try {
                val series = AnalyticsAggregator.windowedSeries(AnalyticsMockData.seriesForCategory(category), currentState.dateRangePreset)
                val merchants = AnalyticsMockData.merchantsForCategory(category)
                val csv = AnalyticsAggregator.generateCsv(category, series, merchants)
                shareSheet.share(text = csv, subject = "$category analytics export")
                setState { copy(isExporting = false, exportedForCategory = category) }
            } catch (e: Exception) {
                setState { copy(isExporting = false, exportError = e.message ?: "Export failed") }
            }
        }
    }

    companion object {
        private fun Set<String>.toggle(value: String): Set<String> = if (value in this) this - value else this + value

        /** Recomputes the My Spend/Team/Insights-tab derived fields from the current filter/date state. */
        private fun recomputed(state: AnalyticsUiState): AnalyticsUiState {
            val window = AnalyticsAggregator.windowedSeries(AnalyticsMockData.dailySeries, state.dateRangePreset, state.customRangeStart, state.customRangeEnd)
            val filteredActivity =
                AnalyticsAggregator.filterActivity(
                    AnalyticsMockData.recentActivity,
                    state.selectedCategories,
                    state.selectedStatuses,
                    state.selectedPaymentMethods,
                )
            return state.copy(
                windowedSeries = window,
                periodDelta = AnalyticsAggregator.periodDelta(window, AnalyticsMockData.priorPeriodSeries),
                peakDay = AnalyticsAggregator.peakDay(window),
                filteredActivity = filteredActivity,
                leaderboard = AnalyticsAggregator.sortLeaderboard(AnalyticsMockData.teamMembers, state.leaderboardSort, state.leaderboardQuery),
                insights =
                    AnalyticsAggregator.deriveInsights(
                        AnalyticsMockData.teamMembers,
                        AnalyticsMockData.pendingApprovalDays,
                        AnalyticsMockData.slaBreachThresholdDays,
                        AnalyticsMockData.dailySeries,
                        AnalyticsMockData.compliancePercent,
                        AnalyticsMockData.categoryTotals,
                    ),
            )
        }

        /** Recomputes the detail-screen (per-category drill-down) derived fields. */
        private fun recomputedDetail(state: AnalyticsUiState): AnalyticsUiState {
            val category = state.detailCategory ?: return state
            val series =
                AnalyticsAggregator.windowedSeries(
                    AnalyticsMockData.seriesForCategory(category),
                    state.dateRangePreset,
                    state.customRangeStart,
                    state.customRangeEnd,
                )
            val merchants = AnalyticsMockData.merchantsForCategory(category)
            val transactions =
                state.selectedMerchant?.let { merchant ->
                    AnalyticsAggregator.searchTransactions(
                        AnalyticsMockData.transactionsForMerchant(category, merchant),
                        state.merchantSearchQuery,
                    )
                } ?: emptyList()
            return state.copy(detailSeries = series, detailMerchants = merchants, merchantTransactions = transactions)
        }
    }
}
