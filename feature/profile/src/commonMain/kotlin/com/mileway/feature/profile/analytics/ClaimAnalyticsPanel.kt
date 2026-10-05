package com.mileway.feature.profile.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.formatMinorCurrency
import org.koin.compose.viewmodel.koinViewModel

private const val MILLIS_PER_HOUR = 3_600_000.0

/** Report analytics is separate from the existing demo spend series and never infers missing dates. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClaimAnalyticsPanel(
    onOpenReimbursements: () -> Unit,
    onOpenRates: () -> Unit,
    viewModel: ClaimAnalyticsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Local report analytics", style = MaterialTheme.typography.titleLarge)
            Text("All local employees. Dates filter report creation time. Payments are simulated.")
            TextButton(onClick = onOpenReimbursements) { Text("My reimbursements") }
            TextButton(onClick = onOpenRates) { Text("Local rate editor") }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { days ->
                    FilterChip(state.filter.days == days, { viewModel.filter(state.filter.copy(days = days)) }, label = { Text("${days}D") })
                }
                ClaimAnalyticsView.entries.forEach { view ->
                    FilterChip(state.view == view, { viewModel.selectView(view) }, label = { Text(view.label()) })
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.filter.status == null, { viewModel.filter(state.filter.copy(status = null)) }, label = { Text("All statuses") })
                ReportLifecycleState.entries.forEach { status ->
                    FilterChip(
                        state.filter.status == status,
                        { viewModel.filter(state.filter.copy(status = status)) },
                        label = { Text(status.name.replace('_', ' ').lowercase()) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.filter.category == null, { viewModel.filter(state.filter.copy(category = null)) }, label = { Text("All claim types") })
                ClaimCategory.entries.forEach { category ->
                    FilterChip(state.filter.category == category, {
                        viewModel.filter(state.filter.copy(category = category))
                    }, label = { Text(category.name.replace('_', ' ').lowercase()) })
                }
            }
        }
        item {
            when {
                state.loading -> Text("Loading reports")
                state.error != null -> Text(requireNotNull(state.error), color = MaterialTheme.colorScheme.error)
                state.summary.reportCount == 0 -> Text("No reports in this filter")
                else -> Text("${state.summary.reportCount} reports")
            }
        }
        item { ClaimMetrics(state.summary, state.view) }
    }
}

private fun ClaimAnalyticsView.label(): String =
    when (this) {
        ClaimAnalyticsView.SPEND -> "Spend"
        ClaimAnalyticsView.VIOLATIONS -> "Violations"
        ClaimAnalyticsView.CYCLE_TIME -> "Cycle time"
    }

@Composable
private fun ClaimMetrics(
    summary: ClaimAnalyticsSummary,
    view: ClaimAnalyticsView,
) {
    when (view) {
        ClaimAnalyticsView.SPEND -> {
            Text("Claimed spend by currency")
            summary.spendByCurrency.forEach { (currency, amount) ->
                Text(formatMinorCurrency(amount, currency))
                val types = summary.spendByCurrencyAndType[currency].orEmpty()
                val largest = types.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                types.forEach { (type, total) ->
                    Text("${type.name.replace('_', ' ').lowercase()}: ${formatMinorCurrency(total, currency)}")
                    MetricBar(total.toFloat() / largest)
                }
            }
        }
        ClaimAnalyticsView.VIOLATIONS -> {
            Text("Distinct policy flags")
            if (summary.violationsByCode.isEmpty()) Text("No policy flags")
            val largest =
                summary.violationsByCode.values
                    .maxOrNull()
                    ?.coerceAtLeast(1) ?: 1
            summary.violationsByCode.toList().sortedBy { it.first }.forEach { (code, count) ->
                Text("$code: $count")
                MetricBar(count.toFloat() / largest)
            }
        }
        ClaimAnalyticsView.CYCLE_TIME -> {
            Text("Completed submission-to-paid cycles")
            val cycles = summary.completedCyclesMillis
            if (cycles.isEmpty()) {
                Text("No paid reports with complete timestamps")
            } else {
                Text("${cycles.size} completed cycles")
                Text("Average ${(cycles.average() / MILLIS_PER_HOUR).toFixed(1)} hours")
                Text("Longest ${((cycles.maxOrNull() ?: 0) / MILLIS_PER_HOUR).toFixed(1)} hours")
                val longest = cycles.maxOrNull()?.coerceAtLeast(1) ?: 1
                cycles.forEach { duration -> MetricBar(duration.toFloat() / longest) }
            }
            Text("Unpaid reports and missing timestamps are excluded.")
        }
    }
}

@Composable
private fun MetricBar(fraction: Float) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        drawRect(color, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height))
    }
}
