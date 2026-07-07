package com.mileway.feature.profile.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_analytics_back
import com.mileway.core.ui.resources.profile_analytics_detail_subtitle
import com.mileway.core.ui.resources.profile_analytics_detail_title
import com.mileway.core.ui.resources.profile_analytics_export
import com.mileway.core.ui.resources.profile_analytics_exporting_report
import com.mileway.core.ui.resources.profile_analytics_pct_of_top_spend
import com.mileway.core.ui.resources.profile_analytics_thirty_day_trend
import com.mileway.core.ui.resources.profile_analytics_top_merchants
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.stub.AnalyticsMockData
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDetailScreen(
    category: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val series = AnalyticsMockData.seriesForCategory(category)
    val merchants = AnalyticsMockData.merchantsForCategory(category)
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportSnackbar by remember { mutableStateOf(false) }
    val exportingMessage = stringResource(Res.string.profile_analytics_exporting_report, category)

    LaunchedEffect(showExportSnackbar) {
        if (showExportSnackbar) {
            snackbarHostState.showSnackbar(exportingMessage)
            showExportSnackbar = false
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
                    IconButton(onClick = { showExportSnackbar = true }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = stringResource(Res.string.profile_analytics_export))
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
                        Spacer(Modifier.height(DesignTokens.Spacing.m))
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        ) {
                            if (series.size < 2) return@Canvas
                            val maxAmount = series.maxOf { it.amountRupees }
                            val points =
                                series.mapIndexed { i, day ->
                                    val x = i * size.width / (series.size - 1).toFloat()
                                    val y = size.height - (day.amountRupees / maxAmount).toFloat() * size.height * 0.85f
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
                        val maxAmount = merchants.maxOfOrNull { it.amountRupees } ?: 1.0
                        merchants.forEachIndexed { i, merchant ->
                            if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

            item { Spacer(Modifier.height(DesignTokens.Spacing.l)) }
        }
    }
}
