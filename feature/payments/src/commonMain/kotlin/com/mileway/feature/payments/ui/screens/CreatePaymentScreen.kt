package com.mileway.feature.payments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
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
import com.mileway.feature.payments.model.PaymentDirection
import com.mileway.feature.payments.viewmodel.CreatePaymentAction
import com.mileway.feature.payments.viewmodel.CreatePaymentEffect
import com.mileway.feature.payments.viewmodel.CreatePaymentViewModel
import org.koin.compose.viewmodel.koinViewModel

/** PM: QR/UPI Pay or Request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreatePaymentScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePaymentViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreatePaymentEffect.Completed -> {
                    Toasts.show("Payment successful", "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is CreatePaymentEffect.Pending -> {
                    Toasts.show("Collect request sent", "${effect.id} is awaiting the payer", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is CreatePaymentEffect.Failed ->
                    Toasts.show("Payment failed", effect.reason, ToastType.Warning)
            }
        }
    }

    FormSubmissionScaffold(
        title = "Pay / Request",
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreatePaymentAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = ui.direction.label,
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = "Mode", leadingIcon = null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = ui.direction == dir,
                            onClick = { viewModel.onAction(CreatePaymentAction.SetDirection(dir)) },
                            label = { Text(dir.label) },
                        )
                    }
                }
            }
            SectionCard(title = "Details", leadingIcon = null) {
                Field("UPI id / payee *", ui.counterparty) { viewModel.onAction(CreatePaymentAction.SetCounterparty(it)) }
                Field("Amount (₹) *", ui.amountText, KeyboardType.Decimal) { viewModel.onAction(CreatePaymentAction.SetAmount(it)) }
                Field("Note", ui.note) { viewModel.onAction(CreatePaymentAction.SetNote(it)) }
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
