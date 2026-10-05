package com.mileway.feature.profile.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.formatMinorCurrency
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlExportValidationScreen(
    onBack: () -> Unit,
    viewModel: GlExportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Accounting export") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Approved, approved for payment and paid reports. All local employees.")
                Text("Line-level data only. No coordinates or addresses.")
                Text(
                    if (state.realBackend) {
                        "Real backend: send CSV to the configured accounting endpoint. Server support is required."
                    } else {
                        "Local mode: share a CSV. No accounting upload."
                    },
                )
                if (state.loading) Text("Loading accounting lines...")
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.message?.let { Text(it) }
                if (!state.dataReady && !state.loading) TextButton(onClick = viewModel::load) { Text("Retry") }
            }
            item {
                val summary =
                    when {
                        !state.dataReady -> "Waiting for accounting data"
                        state.lines.isEmpty() -> "No eligible lines to export"
                        state.unmapped.isEmpty() -> "All ${state.lines.size} lines have GL codes"
                        else -> "${state.unmapped.size} unmapped lines. Export is blocked."
                    }
                Text(summary, style = MaterialTheme.typography.titleMedium)
                Button(onClick = viewModel::export, enabled = state.canExport, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.realBackend) "Send accounting CSV" else "Share accounting CSV")
                }
            }
            items(state.unmapped, key = { "${it.reportId.length}:${it.reportId}${it.lineId}" }) { line ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(line.key, style = MaterialTheme.typography.titleMedium)
                        Text("Report ${line.reportId}, line ${line.lineId}")
                        Text(formatMinorCurrency(line.amountMinor, line.currency))
                    }
                }
            }
            items(state.lines.map { it.key }.distinct(), key = { it }) { key ->
                val mapping = state.mappings.firstOrNull { it.category == key }
                GlMappingEditor(key, mapping?.glAccountCode.orEmpty(), mapping?.costCenter.orEmpty(), state.busy) { code, costCenter ->
                    viewModel.saveMapping(key, code, costCenter)
                }
            }
        }
    }
}

@Composable
private fun GlMappingEditor(
    key: String,
    savedCode: String,
    savedCostCenter: String,
    busy: Boolean,
    onSave: (String, String) -> Unit,
) {
    var code by rememberSaveable(key, savedCode) { mutableStateOf(savedCode) }
    var costCenter by rememberSaveable(key, savedCostCenter) { mutableStateOf(savedCostCenter) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mapping: $key", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(code, { code = it }, label = { Text("GL code for $key") }, enabled = !busy, singleLine = true)
            OutlinedTextField(costCenter, { costCenter = it }, label = { Text("Cost center for $key (optional)") }, enabled = !busy, singleLine = true)
            Button(onClick = { onSave(code, costCenter) }, enabled = !busy && code.isNotBlank()) { Text("Save $key mapping") }
        }
    }
}
