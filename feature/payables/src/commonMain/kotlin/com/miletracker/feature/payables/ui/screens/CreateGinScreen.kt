package com.miletracker.feature.payables.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.scaffold.FormSubmissionScaffold
import com.miletracker.core.ui.toast.ToastType
import com.miletracker.core.ui.toast.Toasts
import com.miletracker.feature.payables.viewmodel.CreateGinAction
import com.miletracker.feature.payables.viewmodel.CreateGinEffect
import com.miletracker.feature.payables.viewmodel.CreateGinViewModel
import org.koin.compose.viewmodel.koinViewModel

/** PB.2 — Create GIN (Goods Inward Note), built on the shared F0.1 FormSubmissionScaffold + F0.2 SectionCards. */
@Composable
fun CreateGinScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateGinViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateGinEffect.Success -> {
                    Toasts.show("Goods receipt logged", "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is CreateGinEffect.NeedsApproval -> {
                    Toasts.show("Sent for approval", "${effect.id} is awaiting your manager", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is CreateGinEffect.Violation ->
                    Toasts.show("Policy violations", effect.messages.joinToString(" · "), ToastType.Warning)
            }
        }
    }

    FormSubmissionScaffold(
        title = "Create GIN",
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateGinAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Log goods receipt",
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = "Receipt", leadingIcon = null) {
                Field("GIN number *", ui.ginNumber) { viewModel.onAction(CreateGinAction.SetGinNumber(it)) }
                Field("PO reference *", ui.poReference) { viewModel.onAction(CreateGinAction.SetPoReference(it)) }
                Field("Vendor", ui.vendor) { viewModel.onAction(CreateGinAction.SetVendor(it)) }
            }
            SectionCard(title = "Goods", leadingIcon = null) {
                Field("Warehouse", ui.warehouse) { viewModel.onAction(CreateGinAction.SetWarehouse(it)) }
                Field("Received quantity *", ui.receivedQtyText, KeyboardType.Number) {
                    viewModel.onAction(CreateGinAction.SetReceivedQty(it))
                }
                Field("Remarks", ui.remarks) { viewModel.onAction(CreateGinAction.SetRemarks(it)) }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
