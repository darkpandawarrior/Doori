package com.mileway.feature.payables.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
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
import com.mileway.core.ui.resources.payables_field_remarks
import com.mileway.core.ui.resources.payables_field_vendor
import com.mileway.core.ui.resources.payables_gin_field_number
import com.mileway.core.ui.resources.payables_gin_field_po_reference
import com.mileway.core.ui.resources.payables_gin_field_received_qty
import com.mileway.core.ui.resources.payables_gin_field_warehouse
import com.mileway.core.ui.resources.payables_gin_section_goods
import com.mileway.core.ui.resources.payables_gin_section_receipt
import com.mileway.core.ui.resources.payables_gin_submit
import com.mileway.core.ui.resources.payables_gin_subtitle
import com.mileway.core.ui.resources.payables_gin_title
import com.mileway.core.ui.resources.payables_gin_toast_success_title
import com.mileway.core.ui.resources.payables_toast_policy_violations
import com.mileway.core.ui.resources.payables_toast_sent_for_approval
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.payables.viewmodel.CreateGinAction
import com.mileway.feature.payables.viewmodel.CreateGinEffect
import com.mileway.feature.payables.viewmodel.CreateGinViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** PB.2: Create GIN (Goods Inward Note), built on the shared F0.1 FormSubmissionScaffold + F0.2 SectionCards. */
@Composable
fun CreateGinScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateGinViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val toastSuccessTitle = stringResource(Res.string.payables_gin_toast_success_title)
    val toastSentApproval = stringResource(Res.string.payables_toast_sent_for_approval)
    val toastPolicyViolations = stringResource(Res.string.payables_toast_policy_violations)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateGinEffect.Success -> {
                    Toasts.show(toastSuccessTitle, "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is CreateGinEffect.NeedsApproval -> {
                    Toasts.show(toastSentApproval, "${effect.id} is awaiting your manager", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is CreateGinEffect.Violation ->
                    Toasts.show(toastPolicyViolations, effect.messages.joinToString(" · "), ToastType.Warning)
            }
        }
    }

    FormSubmissionScaffold(
        title = stringResource(Res.string.payables_gin_title),
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateGinAction.Submit) },
        modifier = modifier,
        subtitle = stringResource(Res.string.payables_gin_subtitle),
        titleIcon = Icons.Filled.LocalShipping,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = stringResource(Res.string.payables_gin_submit),
        submitIcon = Icons.Filled.Check,
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = stringResource(Res.string.payables_gin_section_receipt), leadingIcon = null) {
                Field(stringResource(Res.string.payables_gin_field_number), ui.ginNumber) { viewModel.onAction(CreateGinAction.SetGinNumber(it)) }
                Field(stringResource(Res.string.payables_gin_field_po_reference), ui.poReference) { viewModel.onAction(CreateGinAction.SetPoReference(it)) }
                Field(stringResource(Res.string.payables_field_vendor), ui.vendor) { viewModel.onAction(CreateGinAction.SetVendor(it)) }
            }
            SectionCard(title = stringResource(Res.string.payables_gin_section_goods), leadingIcon = null) {
                Field(stringResource(Res.string.payables_gin_field_warehouse), ui.warehouse) { viewModel.onAction(CreateGinAction.SetWarehouse(it)) }
                Field(stringResource(Res.string.payables_gin_field_received_qty), ui.receivedQtyText, KeyboardType.Number) {
                    viewModel.onAction(CreateGinAction.SetReceivedQty(it))
                }
                Field(stringResource(Res.string.payables_field_remarks), ui.remarks) { viewModel.onAction(CreateGinAction.SetRemarks(it)) }
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
