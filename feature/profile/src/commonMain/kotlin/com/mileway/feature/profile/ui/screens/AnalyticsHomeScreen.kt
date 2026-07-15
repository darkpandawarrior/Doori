@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_analytics_ai_insights_period
import com.mileway.core.ui.resources.profile_analytics_back
import com.mileway.core.ui.resources.profile_analytics_category_breakdown
import com.mileway.core.ui.resources.profile_analytics_hard_stops
import com.mileway.core.ui.resources.profile_analytics_home_subtitle
import com.mileway.core.ui.resources.profile_analytics_member_breakdown
import com.mileway.core.ui.resources.profile_analytics_policy_health
import com.mileway.core.ui.resources.profile_analytics_quick_insights
import com.mileway.core.ui.resources.profile_analytics_recent_activity
import com.mileway.core.ui.resources.profile_analytics_seven_day_spending
import com.mileway.core.ui.resources.profile_analytics_tab_insights
import com.mileway.core.ui.resources.profile_analytics_tab_my_spend
import com.mileway.core.ui.resources.profile_analytics_tab_team
import com.mileway.core.ui.resources.profile_analytics_team_period
import com.mileway.core.ui.resources.profile_analytics_team_spend
import com.mileway.core.ui.resources.profile_analytics_title
import com.mileway.core.ui.resources.profile_analytics_total
import com.mileway.core.ui.resources.profile_analytics_total_spend
import com.mileway.core.ui.resources.profile_analytics_violations
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.profile.analytics.AnalyticsMetric
import com.mileway.feature.profile.analytics.DateRangePreset
import com.mileway.feature.profile.analytics.InsightCard
import com.mileway.feature.profile.analytics.InsightType
import com.mileway.feature.profile.analytics.LeaderboardSort
import com.mileway.feature.profile.viewmodel.AnalyticsAction
import com.mileway.feature.profile.viewmodel.AnalyticsUiState
import com.mileway.feature.profile.viewmodel.AnalyticsViewModel
import com.mileway.stub.RecentActivityItem
import com.mileway.stub.TeamMember
import com.siddharth.kmp.common.formatDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsHomeScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(Brush.horizontalGradient(listOf(Color(0xFF0F4C75), Color(0xFF1B6CA8))))
                        .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_analytics_back), tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.profile_analytics_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(Res.string.profile_analytics_home_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "₹${state.totalSpend.toLong()}",
                            style = MaterialTheme.typography.titleMedium.dataStyle(),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            stringResource(Res.string.profile_analytics_total_spend),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                listOf(
                    stringResource(Res.string.profile_analytics_tab_my_spend),
                    stringResource(Res.string.profile_analytics_tab_team),
                    stringResource(Res.string.profile_analytics_tab_insights),
                ).forEachIndexed { idx, title ->
                    val icon =
                        when (idx) {
                            0 -> Icons.Filled.Person
                            1 -> Icons.Filled.Group
                            else -> Icons.Filled.Lightbulb
                        }
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            when (selectedTab) {
                0 -> MySpendTab(state, viewModel, onOpenDetail)
                1 -> TeamTab(state, viewModel)
                2 -> InsightsTab(state)
            }
        }
    }
}

@Composable
private fun DateRangeChips(
    selected: DateRangePreset,
    onSelect: (DateRangePreset) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        listOf(DateRangePreset.LAST_7 to "7D", DateRangePreset.LAST_30 to "30D", DateRangePreset.LAST_90 to "90D").forEach { (preset, label) ->
            FilterChip(
                selected = selected == preset,
                onClick = { onSelect(preset) },
                label = { Text(label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    state: AnalyticsUiState,
    viewModel: AnalyticsViewModel,
) {
    val categories = listOf("Mileage", "Expense", "Travel", "Advance")
    val statuses = listOf("Approved", "Pending", "Rejected")
    val paymentMethods = listOf("Card", "Cash", "UPI")

    FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        categories.forEach { category ->
            FilterChip(
                selected = category in state.selectedCategories,
                onClick = { viewModel.onAction(AnalyticsAction.CategoryToggled(category)) },
                label = { Text(category) },
            )
        }
        statuses.forEach { status ->
            FilterChip(
                selected = status in state.selectedStatuses,
                onClick = { viewModel.onAction(AnalyticsAction.StatusToggled(status)) },
                label = { Text(status) },
            )
        }
        paymentMethods.forEach { method ->
            FilterChip(
                selected = method in state.selectedPaymentMethods,
                onClick = { viewModel.onAction(AnalyticsAction.PaymentMethodToggled(method)) },
                label = { Text(method) },
            )
        }
        if (state.selectedCategories.isNotEmpty() || state.selectedStatuses.isNotEmpty() || state.selectedPaymentMethods.isNotEmpty()) {
            FilterChip(
                selected = false,
                onClick = { viewModel.onAction(AnalyticsAction.ClearFilters) },
                label = { Text("Clear") },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MySpendTab(
    state: AnalyticsUiState,
    viewModel: AnalyticsViewModel,
    onOpenDetail: (String) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = DesignTokens.Spacing.l,
                vertical = DesignTokens.Spacing.l,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        item { DateRangeChips(state.dateRangePreset, { viewModel.onAction(AnalyticsAction.DateRangeChanged(it)) }) }
        item { SpendingOverviewCard(state, viewModel) }
        item { CategoryBreakdownCard(state, onOpenDetail) }
        item { PolicyHealthCard(state) }
        item {
            Text(stringResource(Res.string.profile_analytics_recent_activity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            FilterChipsRow(state, viewModel)
        }
        items(state.filteredActivity) { item ->
            RecentActivityRow(item)
        }
        item {
            Text(stringResource(Res.string.profile_analytics_quick_insights), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                com.mileway.stub.AnalyticsMockData.quickInsights.forEach { insight ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = DesignTokens.Shape.chip,
                    ) {
                        Text(
                            insight,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.m, vertical = DesignTokens.Spacing.s),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(DesignTokens.Spacing.l)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamTab(
    state: AnalyticsUiState,
    viewModel: AnalyticsViewModel,
) {
    val teamTotal = state.leaderboard.sumOf { it.amountRupees }
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                stringResource(Res.string.profile_analytics_team_spend),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(Res.string.profile_analytics_team_period),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "₹${teamTotal.toLong()}",
                                style = MaterialTheme.typography.titleMedium.dataStyle(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                stringResource(Res.string.profile_analytics_total),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.leaderboardQuery,
                onValueChange = { viewModel.onAction(AnalyticsAction.LeaderboardQueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search team members") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(LeaderboardSort.HIGHEST_SPEND to "Spend", LeaderboardSort.MOST_CLAIMS to "Claims", LeaderboardSort.ALPHABETICAL to "A-Z")
                options.forEachIndexed { index, (sort, label) ->
                    SegmentedButton(
                        selected = state.leaderboardSort == sort,
                        onClick = { viewModel.onAction(AnalyticsAction.LeaderboardSortChanged(sort)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) { Text(label) }
                }
            }
        }

        item {
            Text(stringResource(Res.string.profile_analytics_member_breakdown), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
            ) {
                Column {
                    state.leaderboard.forEachIndexed { index, member ->
                        TeamMemberRow(member = member, teamTotal = teamTotal)
                        if (index < state.leaderboard.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamMemberRow(
    member: TeamMember,
    teamTotal: Double,
) {
    val fraction = (if (teamTotal > 0) member.amountRupees / teamTotal else 0.0).toFloat()
    val categoryColor =
        when (member.topCategory) {
            "Travel" -> MilewayColors.premium
            "Expense" -> MilewayColors.info
            "Mileage" -> MilewayColors.success
            else -> MilewayColors.warning
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(DesignTokens.Shape.button).background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(member.name.first().toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = categoryColor)
        }
        Spacer(Modifier.width(DesignTokens.Spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(member.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("₹${member.amountRupees.toLong()}", style = MaterialTheme.typography.bodyMedium.dataStyle(), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(DesignTokens.Shape.button).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth(fraction).height(4.dp).clip(DesignTokens.Shape.button).background(categoryColor))
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${member.claimCount} claims · ${member.topCategory}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InsightsTab(state: AnalyticsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF5C6BC0), modifier = Modifier.size(20.dp))
                Text(
                    stringResource(Res.string.profile_analytics_ai_insights_period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.insights) { insight ->
            AiInsightCard(insight = insight)
        }
        item { Spacer(Modifier.height(DesignTokens.Spacing.l)) }
    }
}

@Composable
private fun AiInsightCard(insight: InsightCard) {
    val (icon, accentColor) =
        when (insight.type) {
            InsightType.ANOMALY -> Icons.AutoMirrored.Filled.TrendingUp to MilewayColors.warning
            InsightType.BREACH_RISK -> Icons.Default.Warning to MilewayColors.danger
            InsightType.PATTERN -> Icons.Default.Lightbulb to MilewayColors.info
            InsightType.SAVINGS -> Icons.Default.AutoAwesome to MilewayColors.success
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(DesignTokens.Shape.button).background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(insight.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpendingOverviewCard(
    state: AnalyticsUiState,
    viewModel: AnalyticsViewModel,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val peakColor = MaterialTheme.colorScheme.tertiary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.profile_analytics_seven_day_spending),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val delta = state.periodDelta
                val deltaColor = if (delta.isIncrease) DesignTokens.StatusColors.warning else DesignTokens.StatusColors.success
                Surface(color = deltaColor.copy(alpha = 0.15f), shape = DesignTokens.Shape.button) {
                    Text(
                        "${if (delta.isIncrease) "+" else ""}${delta.percentChange.formatDecimal(1)}% vs prev period",
                        style = MaterialTheme.typography.labelSmall,
                        color = deltaColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(AnalyticsMetric.AMOUNT to "Amount", AnalyticsMetric.COUNT to "Count")
                options.forEachIndexed { index, (metric, label) ->
                    SegmentedButton(
                        selected = state.metric == metric,
                        onClick = { viewModel.onAction(AnalyticsAction.MetricChanged(metric)) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(DesignTokens.Spacing.m))

            val series = state.windowedSeries.ifEmpty { com.mileway.stub.AnalyticsMockData.weeklySeries }
            val values = series.map { if (state.metric == AnalyticsMetric.AMOUNT) it.amountRupees else it.transactionCount.toDouble() }
            val maxAmount = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxWidth().height(100.dp),
            ) {
                val barCount = series.size
                val totalWidth = size.width
                val barWidth = totalWidth / (barCount * 1.8f)
                val gap = totalWidth / (barCount * 1.8f) * 0.8f
                val totalBlock = barWidth + gap
                val leftPad = (totalWidth - totalBlock * barCount + gap) / 2

                series.forEachIndexed { i, day ->
                    val value = values[i]
                    val barHeight = (value / maxAmount).toFloat() * size.height * 0.85f
                    val x = leftPad + i * totalBlock
                    val y = size.height - barHeight
                    val isPeak = day.dateMs == state.peakDay?.dateMs
                    drawRoundRect(
                        color = if (isPeak) peakColor else primaryColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xs))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                series.forEach { day ->
                    Text(day.dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    state: AnalyticsUiState,
    onOpenDetail: (String) -> Unit,
) {
    val categoryColors =
        mapOf(
            "Mileage" to Color(0xFF2563EB),
            "Expense" to Color(0xFF7C3AED),
            "Travel" to Color(0xFFEA580C),
            "Advance" to Color(0xFF0F766E),
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Text(stringResource(Res.string.profile_analytics_category_breakdown), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DesignTokens.Spacing.m))

            val total = state.categoryTotals.values.sum()
            // Horizontal stacked bar
            Row(
                modifier = Modifier.fillMaxWidth().height(16.dp).clip(DesignTokens.Shape.button),
            ) {
                state.categoryTotals.entries.forEach { (category, amount) ->
                    val fraction = (amount / total).toFloat()
                    val color = categoryColors[category] ?: MaterialTheme.colorScheme.primary
                    Box(modifier = Modifier.fillMaxSize().weight(fraction).background(color))
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.m))
            state.categoryTotals.entries.forEach { (category, amount) ->
                val color = categoryColors[category] ?: MaterialTheme.colorScheme.primary
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDetail(category) }.padding(vertical = DesignTokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(DesignTokens.Shape.button).background(color))
                    Text(category, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("₹${amount.toLong()}", style = MaterialTheme.typography.bodySmall.dataStyle(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PolicyHealthCard(state: AnalyticsUiState) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val sweep = 360f * (com.mileway.stub.AnalyticsMockData.compliancePercent / 100f)
                    drawArc(
                        color = surfaceVariantColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Text(
                    "${com.mileway.stub.AnalyticsMockData.compliancePercent}%",
                    style = MaterialTheme.typography.labelLarge.dataStyle(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.profile_analytics_policy_health), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(DesignTokens.Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                    PolicyStat(
                        label = stringResource(Res.string.profile_analytics_violations),
                        value = "${com.mileway.stub.AnalyticsMockData.violationCount}",
                        color = DesignTokens.StatusColors.warning,
                    )
                    PolicyStat(
                        label = stringResource(Res.string.profile_analytics_hard_stops),
                        value = "${com.mileway.stub.AnalyticsMockData.hardStopCount}",
                        color = DesignTokens.StatusColors.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyStat(
    label: String,
    value: String,
    color: Color,
) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium.dataStyle(), fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentActivityRow(item: RecentActivityItem) {
    val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(DesignTokens.Shape.button).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.category.first().toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${item.subtitle} · ${Instant.fromEpochMilliseconds(item.dateMs).toLocalDateTime(TimeZone.currentSystemDefault()).let {
                        ldt ->
                    "${ldt.dayOfMonth} ${MONTHS[ldt.monthNumber - 1]}"
                }} · ${item.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("₹${item.amountRupees.toLong()}", style = MaterialTheme.typography.bodyMedium.dataStyle(), fontWeight = FontWeight.SemiBold)
    }
}
