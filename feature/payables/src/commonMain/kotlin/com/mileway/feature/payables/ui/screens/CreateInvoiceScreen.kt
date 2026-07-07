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
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.payables_invoice_field_amount
import com.mileway.core.ui.resources.payables_invoice_field_gl_code
import com.mileway.core.ui.resources.payables_invoice_field_number
import com.mileway.core.ui.resources.payables_invoice_field_tax
import com.mileway.core.ui.resources.payables_invoice_field_vendor
import com.mileway.core.ui.resources.payables_invoice_section_amount
import com.mileway.core.ui.resources.payables_invoice_section_invoice
import com.mileway.core.ui.resources.payables_invoice_submit
import com.mileway.core.ui.resources.payables_invoice_title
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.payables.viewmodel.CreateInvoiceAction
import com.mileway.feature.payables.viewmodel.CreateInvoiceEffect
import com.mileway.feature.payables.viewmodel.CreateInvoiceViewModel
import org.jetbrains.compose.resources.stringResource
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
        title = stringResource(Res.string.payables_invoice_title),
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateInvoiceAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = stringResource(Res.string.payables_invoice_submit),
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = stringResource(Res.string.payables_invoice_section_invoice), leadingIcon = null) {
                Field(
                    stringResource(Res.string.payables_invoice_field_number),
                    ui.invoiceNumber,
                ) { viewModel.onAction(CreateInvoiceAction.SetInvoiceNumber(it)) }
                Field(stringResource(Res.string.payables_invoice_field_vendor), ui.vendor) { viewModel.onAction(CreateInvoiceAction.SetVendor(it)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {}
            SectionCard(title = stringResource(Res.string.payables_invoice_section_amount), leadingIcon = null) {
                Field(
                    stringResource(Res.string.payables_invoice_field_amount),
                    ui.amountText,
                    KeyboardType.Decimal,
                ) { viewModel.onAction(CreateInvoiceAction.SetAmount(it)) }
                Field(
                    stringResource(Res.string.payables_invoice_field_tax),
                    ui.taxPercentText,
                    KeyboardType.Decimal,
                ) { viewModel.onAction(CreateInvoiceAction.SetTax(it)) }
                Field(stringResource(Res.string.payables_invoice_field_gl_code), ui.glCode) { viewModel.onAction(CreateInvoiceAction.SetGlCode(it)) }
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
