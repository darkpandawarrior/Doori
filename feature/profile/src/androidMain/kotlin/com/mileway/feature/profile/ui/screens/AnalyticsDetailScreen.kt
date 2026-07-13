package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_analytics_back
import com.mileway.core.ui.resources.profile_analytics_detail_subtitle
import com.mileway.core.ui.resources.profile_analytics_detail_title
import com.mileway.core.ui.resources.profile_analytics_export
import com.mileway.core.ui.resources.profile_analytics_pct_of_top_spend
import com.mileway.core.ui.resources.profile_analytics_thirty_day_trend
import com.mileway.core.ui.resources.profile_analytics_top_merchants
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.profile.analytics.AnalyticsMetric
import com.mileway.feature.profile.viewmodel.AnalyticsAction
import com.mileway.feature.profile.viewmodel.AnalyticsViewModel
import com.mileway.stub.MerchantTransaction
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDetailScreen(
    category: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(category) {
        viewModel.onAction(AnalyticsAction.OpenCategoryDetail(category))
    }

    LaunchedEffect(state.exportedForCategory) {
        if (state.exportedForCategory == category) {
            snackbarHostState.showSnackbar("$category report shared")
        }
    }
    LaunchedEffect(state.exportError) {
        state.exportError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onAction(AnalyticsAction.ExportErrorCleared)
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.profile_analytics_detail_title, category),
                subtitle = stringResource(Res.string.profile_analytics_detail_subtitle),
                depth = NavigationDepth.LEVEL_2,
                titleIcon = Icons.Filled.BarChart,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_analytics_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onAction(AnalyticsAction.Export(category)) }, enabled = !state.isExporting) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.FileDownload, contentDescription = stringResource(Res.string.profile_analytics_export))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        LazyColumn(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    horizontal = DesignTokens.Spacing.l,
                    vertical = DesignTokens.Spacing.l,
                ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedMd,
                    elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
                        Text(
                            stringResource(Res.string.profile_analytics_thirty_day_trend),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
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
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        val peakColor = MaterialTheme.colorScheme.tertiary
                        val series = state.detailSeries
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        ) {
                            if (series.size < 2) return@Canvas
                            val values = series.map { if (state.metric == AnalyticsMetric.AMOUNT) it.amountRupees else it.transactionCount.toDouble() }
                            val maxAmount = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                            val points =
                                series.mapIndexed { i, _ ->
                                    val x = i * size.width / (series.size - 1).toFloat()
                                    val y = size.height - (values[i] / maxAmount).toFloat() * size.height * 0.85f
                                    Offset(x, y)
                                }

                            // Bezier line
                            val linePath = Path()
                            linePath.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val cp1x = prev.x + (curr.x - prev.x) / 3f
                                val cp2x = prev.x + 2f * (curr.x - prev.x) / 3f
                                linePath.cubicTo(cp1x, prev.y, cp2x, curr.y, curr.x, curr.y)
                            }
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )

                            // Fill
                            val fillPath = Path()
                            fillPath.addPath(linePath)
                            fillPath.lineTo(points.last().x, size.height)
                            fillPath.lineTo(points.first().x, size.height)
                            fillPath.close()
                            drawPath(path = fillPath, color = fillColor)

                            // Endpoint dot
                            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = points.last())

                            // Peak annotation
                            val peakIndex = values.indices.maxByOrNull { values[it] }
                            if (peakIndex != null) {
                                drawCircle(color = peakColor, radius = 5.dp.toPx(), center = points[peakIndex])
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedMd,
                    elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
                        Text(
                            stringResource(Res.string.profile_analytics_top_merchants),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(DesignTokens.Spacing.m))
                        val merchants = state.detailMerchants
                        val maxAmount = merchants.maxOfOrNull { it.amountRupees } ?: 1.0
                        merchants.forEachIndexed { i, merchant ->
                            if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs))
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            viewModel.onAction(
                                                AnalyticsAction.SelectMerchant(
                                                    if (state.selectedMerchant == merchant.name) null else merchant.name,
                                                ),
                                            )
                                        },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                            ) {
                                Text(
                                    "#${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = DesignTokens.Spacing.xs),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(merchant.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    val pct = (merchant.amountRupees / maxAmount * 100).toInt()
                                    Text(
                                        stringResource(Res.string.profile_analytics_pct_of_top_spend, pct),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text("₹%,.0f".format(merchant.amountRupees), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (state.selectedMerchant != null) {
                item {
                    MerchantDrilldownCard(
                        merchantName = state.selectedMerchant!!,
                        query = state.merchantSearchQuery,
                        transactions = state.merchantTransactions,
                        onQueryChanged = { viewModel.onAction(AnalyticsAction.MerchantSearchQueryChanged(it)) },
                    )
                }
            }

            item { Spacer(Modifier.height(DesignTokens.Spacing.l)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantDrilldownCard(
    merchantName: String,
    query: String,
    transactions: List<MerchantTransaction>,
    onQueryChanged: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Text(merchantName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search transactions") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            transactions.forEachIndexed { i, txn ->
                if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(txn.id, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        FilterChip(selected = false, onClick = {}, label = { Text(txn.status) }, enabled = false)
                    }
                    Text("₹%,.0f".format(txn.amountRupees), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (transactions.isEmpty()) {
                Text("No matching transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
