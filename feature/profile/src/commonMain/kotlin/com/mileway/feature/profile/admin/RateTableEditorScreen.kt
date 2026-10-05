package com.mileway.feature.profile.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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

/** Local, unauthenticated administration with immutable historical versions and JSON export. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RateTableEditorScreen(
    onBack: () -> Unit,
    viewModel: RateTableEditorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Local rate editor", style = MaterialTheme.typography.headlineSmall)
            Text("Unauthenticated local admin. These versions are stored on this device.")
            Text("Claim pricing still uses its current sources. Export JSON for integration.")
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RateTableKind.entries.forEach { kind ->
                    FilterChip(
                        selected = draft.kind == kind,
                        enabled = !state.busy,
                        onClick = { viewModel.edit(RateVersionDraft(kind = kind)) },
                        label = { Text(kind.label()) },
                    )
                }
            }
        }
        item {
            RateField("Effective date (YYYY-MM-DD)", draft.effectiveFrom, state.busy) { viewModel.edit(draft.copy(effectiveFrom = it)) }
            RateField(rateLabel(draft.kind), draft.rate, state.busy) { viewModel.edit(draft.copy(rate = it)) }
        }
        item {
            when (draft.kind) {
                RateTableKind.POLICY -> RateField("Vehicle key", draft.vehicle, state.busy) { viewModel.edit(draft.copy(vehicle = it)) }
                RateTableKind.PER_DIEM -> {
                    RateField("Region", draft.region, state.busy) { viewModel.edit(draft.copy(region = it)) }
                    RateField("Grade", draft.grade, state.busy) { viewModel.edit(draft.copy(grade = it)) }
                    RateField("Currency (INR, USD, GBP)", draft.currency, state.busy) { viewModel.edit(draft.copy(currency = it)) }
                }
                RateTableKind.IRS, RateTableKind.HMRC -> {
                    RateField("First band distance (miles, optional)", draft.bandDistance, state.busy) { viewModel.edit(draft.copy(bandDistance = it)) }
                    RateField("Above band rate (optional)", draft.aboveBandRate, state.busy) { viewModel.edit(draft.copy(aboveBandRate = it)) }
                    RateField("Local employer rate note", draft.note, state.busy) { viewModel.edit(draft.copy(note = it)) }
                }
            }
        }
        item {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it) }
            Button(onClick = viewModel::save, enabled = !state.busy && state.tables != null) { Text("Add dated version") }
            TextButton(onClick = viewModel::export, enabled = !state.busy && state.tables != null) { Text("Export rate JSON") }
            if (state.tables == null && !state.busy) TextButton(onClick = viewModel::load) { Text("Retry loading rates") }
        }
        item {
            state.tables?.let { RateHistory(it, draft.kind) }
        }
    }
}

private fun RateTableKind.label(): String =
    when (this) {
        RateTableKind.POLICY -> "Policy"
        RateTableKind.IRS -> "IRS mirror"
        RateTableKind.HMRC -> "HMRC mirror"
        RateTableKind.PER_DIEM -> "Per diem"
    }

private fun rateLabel(kind: RateTableKind): String =
    when (kind) {
        RateTableKind.POLICY -> "Rate per km (INR)"
        RateTableKind.IRS -> "Rate per mile (USD, up to 5 decimals)"
        RateTableKind.HMRC -> "Rate per mile (GBP, up to 5 decimals)"
        RateTableKind.PER_DIEM -> "Daily rate (currency units)"
    }

@Composable
private fun RateField(
    label: String,
    value: String,
    busy: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RateHistory(
    tables: RateTables,
    kind: RateTableKind,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Saved versions", style = MaterialTheme.typography.titleMedium)
            when (kind) {
                RateTableKind.POLICY -> {
                    if (tables.mileage.policy.isEmpty()) Text("No local policy versions yet")
                    tables.mileage.policy.forEach { version ->
                        Text(version.effectiveFrom)
                        version.ratesMinorPerKm.forEach { (vehicle, rate) -> Text("$vehicle: ${formatMinorCurrency(rate, "INR")} / km") }
                    }
                }
                RateTableKind.PER_DIEM -> {
                    if (tables.perDiem.isEmpty()) Text("No per-diem rate cards yet")
                    tables.perDiem.forEach {
                        Text(
                            "${it.effectiveFrom} · ${it.region}/${it.grade}: ${formatMinorCurrency(it.dailyRateMinor, it.currency)} / day",
                        )
                    }
                }
                RateTableKind.IRS, RateTableKind.HMRC -> {
                    val mirror = tables.mileage.mirrors.getValue(kind.name)
                    Text(mirror.sourceTitle)
                    Text(mirror.sourceUrl, style = MaterialTheme.typography.bodySmall)
                    Text("Retrieved ${mirror.retrievedOn}. Local versions retain their employer note.")
                    mirror.versions.forEach { version ->
                        Text("${version.effectiveFrom}: ${version.authority}")
                        Text("100 miles: ${formatMinorCurrency(version.schedule.amountMinor(100), mirror.currency)}")
                        version.schedule.firstBandDistanceUnits?.let { Text("Step down after $it annual miles") }
                    }
                    mirror.favrCostLimits.forEach {
                        Text(
                            "FAVR cap ${it.effectiveFrom}: ${formatMinorCurrency(it.maxStandardAutomobileCostMinor, mirror.currency)}",
                        )
                    }
                }
            }
        }
    }
}
