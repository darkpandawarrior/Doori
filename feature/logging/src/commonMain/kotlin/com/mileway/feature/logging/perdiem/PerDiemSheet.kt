package com.mileway.feature.logging.perdiem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold

/** Per-diem entry context in the Spends wizard, followed by the shared report review. */
@Composable
fun PerDiemSheet(
    viewModel: PerDiemEntryViewModel,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdReportId) {
        state.createdReportId?.let { id ->
            viewModel.consumeCreatedReport()
            onOpenReport(id)
        }
    }
    PerDiemContent(state, onBack, viewModel::createReport, viewModel::select, viewModel::dates, viewModel::load)
}

@Composable
internal fun PerDiemContent(
    state: PerDiemEntryViewModel.State,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onSelect: (PerDiemEntryViewModel.RateCard) -> Unit,
    onDates: (String, String) -> Unit,
    onRetry: () -> Unit,
) {
    FormSubmissionScaffold(
        title = "Per diem",
        subtitle = "One daily allowance per eligible day",
        onBack = onBack,
        onSubmit = onCreate,
        submitLabel = "Create report and review",
        canSubmit = !state.loading && state.preview.error == null && state.preview.lines.isNotEmpty(),
        isSubmitting = state.busy,
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loading) Text("Loading per-diem rates")
            if (!state.loading && state.cards.isEmpty()) Text("No per-diem rate cards available. Ask your administrator to configure a dated rate.")
            state.cards.forEach { card ->
                TextButton(onClick = { onSelect(card) }, enabled = !state.busy) {
                    val selected = if (card == state.selected) "Selected · " else ""
                    Text("$selected${card.region} · ${card.grade} · ${card.currency}")
                }
            }
            OutlinedTextField(
                value = state.start,
                onValueChange = { onDates(it, state.end) },
                label = { Text("Start date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.busy,
            )
            OutlinedTextField(
                value = state.end,
                onValueChange = { onDates(state.start, it) },
                label = { Text("End date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.busy,
            )
            Text("Start and end dates are included. Maximum $MaxPerDiemRangeDays days.")
            state.preview.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.preview.skipped.forEach { Text("${it.date}: ${it.reason}") }
            state.preview.lines.forEach { Text("${it.incurredOn} · ${formatMinorCurrency(it.dailyRateMinor, it.currency)}") }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, enabled = !state.busy) { Text("Reload rates") }
            }
        }
    }
}

@Preview
@Composable
private fun PerDiemPreview() {
    MaterialTheme {
        PerDiemContent(PerDiemEntryViewModel.State(loading = false), {}, {}, {}, { _, _ -> }, {})
    }
}
