package com.miletracker.feature.payables.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.scaffold.FormSubmissionScaffold
import com.miletracker.core.ui.toast.ToastType
import com.miletracker.core.ui.toast.Toasts
import com.miletracker.feature.payables.repository.ParkMode
import com.miletracker.feature.payables.viewmodel.CreateParkingAction
import com.miletracker.feature.payables.viewmodel.CreateParkingEffect
import com.miletracker.feature.payables.viewmodel.CreateParkingViewModel
import org.koin.compose.viewmodel.koinViewModel

/** PB.3: Create Park In / Park Out gate event, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreateParkingScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateParkingViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateParkingEffect.Success -> {
                    Toasts.show("Gate event logged", "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is CreateParkingEffect.NeedsApproval -> {
                    Toasts.show("Sent for approval", "${effect.id} is awaiting clearance", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is CreateParkingEffect.Violation ->
                    Toasts.show("Policy violations", effect.messages.joinToString(" · "), ToastType.Warning)
            }
        }
    }

    FormSubmissionScaffold(
        title = ui.mode.label,
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateParkingAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Log ${ui.mode.label.lowercase()}",
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = "Direction", leadingIcon = null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ParkMode.entries.forEach { mode ->
                        FilterChip(
                            selected = ui.mode == mode,
                            onClick = { viewModel.onAction(CreateParkingAction.SetMode(mode)) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
            SectionCard(title = "Vehicle", leadingIcon = null) {
                Field("Vehicle number *", ui.vehicleNumber) { viewModel.onAction(CreateParkingAction.SetVehicleNumber(it)) }
                Field("Driver name", ui.driverName) { viewModel.onAction(CreateParkingAction.SetDriverName(it)) }
                Field("Gate / dock *", ui.gate) { viewModel.onAction(CreateParkingAction.SetGate(it)) }
            }
            SectionCard(title = "Reference", leadingIcon = null) {
                Field("PO reference", ui.poReference) { viewModel.onAction(CreateParkingAction.SetPoReference(it)) }
                Field("Remarks", ui.remarks) { viewModel.onAction(CreateParkingAction.SetRemarks(it)) }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
