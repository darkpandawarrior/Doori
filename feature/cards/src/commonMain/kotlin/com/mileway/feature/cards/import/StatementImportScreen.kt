package com.mileway.feature.cards.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** Reachable from Cards; pasting local text also works on hosts without a document picker. */
@Composable
fun StatementImportScreen(
    onBack: () -> Unit,
    viewModel: StatementImportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickerError by remember { mutableStateOf<String?>(null) }
    val pick =
        rememberStatementImportLauncher(
            onPicked = { name, text -> viewModel.onAction(StatementImportAction.Import(name, text)) },
            onError = { pickerError = "Unable to read the statement. Choose a UTF-8 CSV or OFX file under 2 MB." },
        )
    StatementImportContent(state, viewModel::onAction, onBack, pick, pickerError)
}

@Composable
internal fun StatementImportContent(
    state: StatementImportState,
    onAction: (StatementImportAction) -> Unit,
    onBack: () -> Unit,
    pick: (() -> Unit)? = null,
    pickerError: String? = null,
) {
    var name by remember { mutableStateOf("statement.csv") }
    var text by remember { mutableStateOf("") }
    var vpa by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back to cards") }
        Text("Import card statement", style = MaterialTheme.typography.headlineSmall)
        Text("Import CSV or OFX purchases from a local file. Group expenses into a draft report first. Matching uses amount, capture date and merchant.")
        Text(
            "CSV headers: id (optional), date, merchant, amount, currency. Use YYYY-MM-DD and positive two-decimal amounts. Optional: foreign_amount, foreign_currency.",
        )
        pick?.let { Button(onClick = it, enabled = !state.busy) { Text("Choose CSV or OFX file") } }
        OutlinedTextField(name, { name = it }, label = { Text("Statement name") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            text,
            { text = it },
            label = { Text("Paste CSV or OFX text") },
            minLines = 4,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onAction(StatementImportAction.Import(name, text)) }, enabled = !state.busy && text.isNotBlank()) { Text("Import and match") }
        if (state.busy) Text("Saving locally…")
        (state.error ?: pickerError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.result?.let { result ->
            if (result.alreadyImported) {
                Text(
                    "This statement was already imported.",
                )
            } else {
                Text("${result.matched} matched; ${result.rows.size - result.matched} unmatched. Matched amounts are locked.")
            }
            result.rows.forEachIndexed { index, row ->
                Text("Row ${index + 1}: ${row.lineId?.let { "matched to $it" } ?: row.reason}")
            }
        }
        Text("Payout beneficiary", style = MaterialTheme.typography.titleLarge)
        Text("Save a payout address on this device. No bank account is linked and no payment is sent.")
        Text(if (state.beneficiarySaved) "Payout address saved securely" else "No payout address saved")
        OutlinedTextField(vpa, {
            vpa = it
        }, label = { Text("Payout address (name@handle)") }, singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            onAction(StatementImportAction.SaveBeneficiary(vpa))
            vpa = ""
        }, enabled = !state.busy && vpa.isNotBlank()) { Text("Save payout address") }
        if (state.beneficiarySaved) {
            TextButton(
                onClick = { onAction(StatementImportAction.ClearBeneficiary) },
                enabled = !state.busy,
            ) { Text("Remove payout address") }
        }
    }
}

@Preview
@Composable
private fun StatementImportPreview() {
    MaterialTheme { StatementImportContent(StatementImportState(), {}, {}) }
}
