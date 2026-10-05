package com.mileway.feature.profile.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.formatMinorCurrency
import org.koin.compose.viewmodel.koinViewModel

/** Live local reimbursement status for the active employee. */
@Composable
fun ReimbursementStatusScreen(
    onBack: () -> Unit,
    viewModel: ReimbursementStatusViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Reimbursements", style = MaterialTheme.typography.headlineSmall)
            Text("Local reports. Payments are simulated.")
        }
        item {
            when {
                state.loading -> Text("Loading reimbursements")
                state.error != null -> Text(requireNotNull(state.error), color = MaterialTheme.colorScheme.error)
                state.reports.isEmpty() -> Text("No reports for this employee")
            }
        }
        items(state.reports, key = { it.id }) { report ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(report.id, style = MaterialTheme.typography.titleMedium)
                    Text(formatMinorCurrency(report.totalAmountMinor(), report.currency()))
                    ReimbursementStatusStepper(report.state)
                }
            }
        }
    }
}
