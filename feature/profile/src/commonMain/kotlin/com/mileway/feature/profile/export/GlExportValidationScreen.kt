package com.mileway.feature.profile.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.formatMinorCurrency
import org.koin.compose.viewmodel.koinViewModel

/** Lists unmapped lines, maps categories inline, and enables export only when nothing is unmapped. */
@Composable
fun GlExportValidationScreen(
    onBack: () -> Unit,
    viewModel: GlExportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("GL export check", style = MaterialTheme.typography.headlineSmall)
            Text("Approved and paid claim lines. Every line needs a GL code before export.")
            Text(
                if (state.realBackend) {
                    "Backend is on: the export is sent to the accounting endpoint."
                } else {
                    "Backend is off: the export is a CSV you share."
                },
            )
        }
        item {
            Text(
                if (state.unmapped.isEmpty()) {
                    "All ${state.lines.size} lines have a GL code."
                } else {
                    "${state.unmapped.size} lines have no GL code"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(state.unmapped.map { it.key }.distinct()) { key ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category: $key", style = MaterialTheme.typography.titleSmall)
                    state.unmapped.filter { it.key == key }.forEach { line ->
                        Text("${line.reportId} / ${line.lineId}: ${formatMinorCurrency(line.amountMinor, line.currency)}")
                    }
                    OutlinedTextField(
                        value = state.drafts[key].orEmpty(),
                        onValueChange = { viewModel.editDraft(key, it) },
                        label = { Text("GL code for $key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { viewModel.map(key) }) { Text("Map $key") }
                }
            }
        }
        item {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it) }
            Button(onClick = viewModel::export, enabled = state.canExport) { Text("Export") }
            if (state.lines.isEmpty()) Text("No approved or paid claim lines to export.")
        }
    }
}
