package com.mileway.feature.payables.ui.screens

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
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.payables_field_remarks
import com.mileway.core.ui.resources.payables_parking_field_driver_name
import com.mileway.core.ui.resources.payables_parking_field_gate
import com.mileway.core.ui.resources.payables_parking_field_po_reference
import com.mileway.core.ui.resources.payables_parking_field_vehicle_number
import com.mileway.core.ui.resources.payables_parking_section_direction
import com.mileway.core.ui.resources.payables_parking_section_reference
import com.mileway.core.ui.resources.payables_parking_section_vehicle
import com.mileway.core.ui.resources.payables_parking_submit
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.payables.repository.ParkMode
import com.mileway.feature.payables.viewmodel.CreateParkingAction
import com.mileway.feature.payables.viewmodel.CreateParkingEffect
import com.mileway.feature.payables.viewmodel.CreateParkingViewModel
import org.jetbrains.compose.resources.stringResource
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
        submitLabel = stringResource(Res.string.payables_parking_submit, ui.mode.label.lowercase()),
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = stringResource(Res.string.payables_parking_section_direction), leadingIcon = null) {
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
            SectionCard(title = stringResource(Res.string.payables_parking_section_vehicle), leadingIcon = null) {
                Field(
                    stringResource(Res.string.payables_parking_field_vehicle_number),
                    ui.vehicleNumber,
                ) { viewModel.onAction(CreateParkingAction.SetVehicleNumber(it)) }
                Field(
                    stringResource(Res.string.payables_parking_field_driver_name),
                    ui.driverName,
                ) { viewModel.onAction(CreateParkingAction.SetDriverName(it)) }
                Field(stringResource(Res.string.payables_parking_field_gate), ui.gate) { viewModel.onAction(CreateParkingAction.SetGate(it)) }
            }
            SectionCard(title = stringResource(Res.string.payables_parking_section_reference), leadingIcon = null) {
                Field(
                    stringResource(Res.string.payables_parking_field_po_reference),
                    ui.poReference,
                ) { viewModel.onAction(CreateParkingAction.SetPoReference(it)) }
                Field(stringResource(Res.string.payables_field_remarks), ui.remarks) { viewModel.onAction(CreateParkingAction.SetRemarks(it)) }
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
