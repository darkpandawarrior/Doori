package com.mileway.feature.payables.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.payables.viewmodel.CreateInvoiceAction
import com.mileway.feature.payables.viewmodel.CreateInvoiceEffect
import com.mileway.feature.payables.viewmodel.CreateInvoiceViewModel
import org.koin.compose.viewmodel.koinViewModel

/** PB.1: Create Invoice, built on the shared F0.1 FormSubmissionScaffold + F0.2 SectionCards. */
@Composable
fun CreateInvoiceScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateInvoiceViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateInvoiceEffect.Success -> {
                    Toasts.show("Invoice submitted", "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is CreateInvoiceEffect.NeedsApproval -> {
                    Toasts.show("Sent for approval", "${effect.id} is awaiting your manager", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is CreateInvoiceEffect.Violation ->
                    Toasts.show("Policy violations", effect.messages.joinToString(" · "), ToastType.Warning)
            }
        }
    }

    FormSubmissionScaffold(
        title = "Create Invoice",
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateInvoiceAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Submit invoice",
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = "Invoice", leadingIcon = null) {
                Field("Invoice number *", ui.invoiceNumber) { viewModel.onAction(CreateInvoiceAction.SetInvoiceNumber(it)) }
                Field("Vendor *", ui.vendor) { viewModel.onAction(CreateInvoiceAction.SetVendor(it)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {}
            SectionCard(title = "Amount & coding", leadingIcon = null) {
                Field("Amount (₹) *", ui.amountText, KeyboardType.Decimal) { viewModel.onAction(CreateInvoiceAction.SetAmount(it)) }
                Field("Tax %", ui.taxPercentText, KeyboardType.Decimal) { viewModel.onAction(CreateInvoiceAction.SetTax(it)) }
                Field("GL code", ui.glCode) { viewModel.onAction(CreateInvoiceAction.SetGlCode(it)) }
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
