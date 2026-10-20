package com.mileway.feature.logging.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.logging.policy.fxProvenanceLabel

/** Spends entry point for date suggestions, manual grouping and reopening persisted reports. */
@Composable
fun ReportGroupingScreen(
    viewModel: ReportGroupingViewModel,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    reportId: String? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(reportId) { viewModel.load(reportId) }
    LaunchedEffect(state.createdReportId) {
        state.createdReportId?.let { id ->
            viewModel.consumeCreatedReport()
            onOpenReport(id)
        }
    }
    ReportGroupingContent(state, onBack, viewModel::createReport, viewModel::toggle, viewModel::selectDate, onOpenReport, { viewModel.load() })
}

@Composable
private fun ReportGroupingContent(
    state: ReportGroupingViewModel.State,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onToggle: (String) -> Unit,
    onSelectDate: (String) -> Unit,
    onOpenReport: (String) -> Unit,
    onRetry: () -> Unit,
) {
    FormSubmissionScaffold(
        title = "Expense reports",
        subtitle = "Select at least two logged expenses",
        onBack = onBack,
        onSubmit = onCreate,
        submitLabel = if (state.screen.dataOrNull?.editingReport == null) "Group and review" else "Save grouping",
        canSubmit =
            state.screen.dataOrNull
                ?.selectedIds
                .orEmpty()
                .size >= MinimumGroupedExpenses,
        isSubmitting = state.busy,
    ) { padding ->
        ScreenStateContent(state.screen, modifier = Modifier.padding(padding).padding(16.dp), onRetry = onRetry) { grouping ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (grouping.loose.isEmpty()) Text("All logged expenses are already grouped")
                grouping.suggestions.forEach { (date, expenses) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(date, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onSelectDate(date) }, enabled = !state.busy) { Text("Select date") }
                    }
                    expenses.forEach { expense ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = expense.id in grouping.selectedIds, onCheckedChange = { onToggle(expense.id) }, enabled = !state.busy)
                            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                Text(expense.merchantName)
                                Text(formatMinorCurrency(expense.toClaimLine().amountMinor, expense.currencyCode), style = MaterialTheme.typography.bodySmall)
                                expense.fxProvenanceLabel()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
                if (grouping.reports.isNotEmpty()) Text("Your reports", style = MaterialTheme.typography.titleMedium)
                grouping.reports.forEach { report ->
                    TextButton(onClick = { onOpenReport(report.id) }, enabled = !state.busy) {
                        Text("${report.id.take(8)} · ${report.state.name} · ${report.lines.size} items")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ReportGroupingPreview() {
    ReportGroupingContent(
        state =
            ReportGroupingViewModel.State(
                screen = ScreenState.Content(ReportGroupingViewModel.Grouping(emptyList(), emptyMap(), listOf(Report("demo", "employee")), "employee")),
            ),
        onBack = {},
        onCreate = {},
        onToggle = {},
        onSelectDate = {},
        onOpenReport = {},
        onRetry = {},
    )
}
