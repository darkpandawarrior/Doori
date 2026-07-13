package com.mileway.feature.payments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.rememberMediaCaptureLauncher
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_cancel
import com.mileway.core.ui.resources.action_retry
import com.mileway.core.ui.resources.payments_attach_invoice
import com.mileway.core.ui.resources.payments_create_subtitle
import com.mileway.core.ui.resources.payments_create_title
import com.mileway.core.ui.resources.payments_field_amount
import com.mileway.core.ui.resources.payments_field_counterparty
import com.mileway.core.ui.resources.payments_field_note
import com.mileway.core.ui.resources.payments_invoice_attach_confirm
import com.mileway.core.ui.resources.payments_invoice_attached
import com.mileway.core.ui.resources.payments_invoice_duplicate_blocked
import com.mileway.core.ui.resources.payments_invoice_duplicate_message
import com.mileway.core.ui.resources.payments_invoice_duplicate_title
import com.mileway.core.ui.resources.payments_pin_confirm
import com.mileway.core.ui.resources.payments_pin_error
import com.mileway.core.ui.resources.payments_pin_subtitle
import com.mileway.core.ui.resources.payments_pin_title
import com.mileway.core.ui.resources.payments_section_details
import com.mileway.core.ui.resources.payments_section_mode
import com.mileway.core.ui.resources.payments_status_done
import com.mileway.core.ui.resources.payments_status_failed_title
import com.mileway.core.ui.resources.payments_status_polling
import com.mileway.core.ui.resources.payments_status_submitting
import com.mileway.core.ui.resources.payments_status_success_title
import com.mileway.core.ui.resources.payments_toast_failed
import com.mileway.core.ui.resources.payments_toast_successful
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.payments.model.PaymentDirection
import com.mileway.feature.payments.model.PaymentTransactionStatus
import com.mileway.feature.payments.viewmodel.CreatePaymentAction
import com.mileway.feature.payments.viewmodel.CreatePaymentEffect
import com.mileway.feature.payments.viewmodel.CreatePaymentUiState
import com.mileway.feature.payments.viewmodel.CreatePaymentViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * PM: QR/UPI Pay or Request, built on the shared F0.1 FormSubmissionScaffold + SectionCards.
 *
 * P29.C.6: three mutually-exclusive bodies driven by [CreatePaymentUiState] — the form, the PIN
 * keypad gate (amounts ≥ the PIN threshold), and the transaction-status screen
 * (submitting/polling/success/failed). No extra nav routes — same "step lives in ViewModel state"
 * pattern `CardKycScreen`'s wizard already uses.
 */
@Composable
fun CreatePaymentScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePaymentViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val toastSuccessful = stringResource(Res.string.payments_toast_successful)
    val toastFailed = stringResource(Res.string.payments_toast_failed)
    val toastInvoiceAttached = stringResource(Res.string.payments_invoice_attached)
    val toastInvoiceBlocked = stringResource(Res.string.payments_invoice_duplicate_blocked)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreatePaymentEffect.Completed -> Toasts.show(toastSuccessful, "Reference ${effect.id}", ToastType.Success)
                is CreatePaymentEffect.Failed -> Toasts.show(toastFailed, effect.reason, ToastType.Warning)
                CreatePaymentEffect.InvoiceAttached -> Toasts.show(toastInvoiceAttached, "", ToastType.Success)
                CreatePaymentEffect.InvoiceDuplicateBlocked -> Toasts.show(toastInvoiceBlocked, "", ToastType.Warning)
            }
        }
    }

    when {
        ui.awaitingPin -> PaymentPinScreen(ui, viewModel::onAction, modifier)
        ui.transactionStatus != PaymentTransactionStatus.IDLE ->
            PaymentStatusScreen(ui, viewModel::onAction, onDone = { onSubmitted(ui.resultId ?: "") }, modifier)
        else -> PaymentFormScreen(ui, viewModel::onAction, onBack, modifier)
    }

    ui.duplicatePrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(CreatePaymentAction.CancelDuplicateAttach) },
            title = { Text(stringResource(Res.string.payments_invoice_duplicate_title)) },
            text = { Text(stringResource(Res.string.payments_invoice_duplicate_message, prompt.reason)) },
            confirmButton = {
                Button(onClick = { viewModel.onAction(CreatePaymentAction.ConfirmDuplicateAttach) }) {
                    Text(stringResource(Res.string.payments_invoice_attach_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.onAction(CreatePaymentAction.CancelDuplicateAttach) }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PaymentFormScreen(
    ui: CreatePaymentUiState,
    onAction: (CreatePaymentAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    FormSubmissionScaffold(
        title = stringResource(Res.string.payments_create_title),
        subtitle = stringResource(Res.string.payments_create_subtitle),
        titleIcon = Icons.Filled.Payments,
        onBack = onBack,
        onSubmit = { onAction(CreatePaymentAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = ui.direction.localizedLabel(),
        submitIcon = Icons.AutoMirrored.Filled.Send,
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = stringResource(Res.string.payments_section_mode), leadingIcon = null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = ui.direction == dir,
                            onClick = { onAction(CreatePaymentAction.SetDirection(dir)) },
                            label = { Text(dir.localizedLabel()) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (dir == PaymentDirection.PAY) Icons.AutoMirrored.Filled.Send else Icons.Filled.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }
            SectionCard(title = stringResource(Res.string.payments_section_details), leadingIcon = null) {
                Field(stringResource(Res.string.payments_field_counterparty), ui.counterparty) { onAction(CreatePaymentAction.SetCounterparty(it)) }
                Field(
                    stringResource(Res.string.payments_field_amount),
                    ui.amountText,
                    KeyboardType.Decimal,
                ) { onAction(CreatePaymentAction.SetAmount(it)) }
                Field(stringResource(Res.string.payments_field_note), ui.note) { onAction(CreatePaymentAction.SetNote(it)) }
            }
        }
    }
}

/** P29.C.6: dedicated 4-digit PIN keypad gate for amounts at/above the PIN threshold. */
@Composable
private fun PaymentPinScreen(
    ui: CreatePaymentUiState,
    onAction: (CreatePaymentAction) -> Unit,
    modifier: Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(Res.string.payments_pin_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(Res.string.payments_pin_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignTokens.Spacing.s, bottom = DesignTokens.Spacing.l),
            )
            OutlinedTextField(
                value = ui.pinText,
                onValueChange = { onAction(CreatePaymentAction.SetPinDigits(it)) },
                isError = ui.pinError,
                supportingText =
                    if (ui.pinError) {
                        { Text(stringResource(Res.string.payments_pin_error)) }
                    } else {
                        null
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer()
            Button(
                onClick = { onAction(CreatePaymentAction.ConfirmPin) },
                enabled = ui.pinText.length == 4,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) { Text(stringResource(Res.string.payments_pin_confirm)) }
            OutlinedButton(
                onClick = { onAction(CreatePaymentAction.CancelPin) },
                modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.s),
                shape = DesignTokens.Shape.button,
            ) { Text(stringResource(Res.string.action_cancel)) }
        }
    }
}

/** P29.C.6/C.7: SUBMITTING/POLLING/SUCCESS/FAILED — the SUCCESS body also hosts the C.7 invoice attach. */
@Composable
private fun PaymentStatusScreen(
    ui: CreatePaymentUiState,
    onAction: (CreatePaymentAction) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (ui.transactionStatus) {
                PaymentTransactionStatus.SUBMITTING, PaymentTransactionStatus.POLLING -> {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = DesignTokens.Spacing.l))
                    Text(
                        stringResource(
                            if (ui.transactionStatus == PaymentTransactionStatus.SUBMITTING) {
                                Res.string.payments_status_submitting
                            } else {
                                Res.string.payments_status_polling
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                PaymentTransactionStatus.SUCCESS -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = DesignTokens.StatusColors.success,
                        modifier = Modifier.size(56.dp).padding(bottom = DesignTokens.Spacing.l),
                    )
                    Text(stringResource(Res.string.payments_status_success_title), style = MaterialTheme.typography.titleLarge)
                    InvoiceAttachSection(ui, onAction)
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.l),
                        shape = DesignTokens.Shape.button,
                    ) { Text(stringResource(Res.string.payments_status_done)) }
                }
                PaymentTransactionStatus.FAILED -> {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp).padding(bottom = DesignTokens.Spacing.l),
                    )
                    Text(stringResource(Res.string.payments_status_failed_title), style = MaterialTheme.typography.titleLarge)
                    ui.failureReason?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { onAction(CreatePaymentAction.Retry) },
                        modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.l),
                        shape = DesignTokens.Shape.button,
                    ) { Text(stringResource(Res.string.action_retry)) }
                }
                PaymentTransactionStatus.IDLE -> Unit
            }
        }
    }
}

/** P29.C.7: QR invoice attachment — reuses `core:media`'s shared OCR pipeline (same as receipts). */
@Composable
private fun InvoiceAttachSection(
    ui: CreatePaymentUiState,
    onAction: (CreatePaymentAction) -> Unit,
) {
    var pendingAnalysis by remember { mutableStateOf<DocumentAnalysis?>(null) }
    val launchInvoicePicker =
        rememberMediaCaptureLauncher(
            config = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Gallery), enableOcr = true, ocrDocType = DocType.INVOICE.name),
            onOcrAnalysis = { pendingAnalysis = it },
            onResult = { result ->
                val uri = (result as? MediaCaptureResult.Attachments)?.items?.firstOrNull()?.uri
                val analysis = pendingAnalysis
                if (uri != null && analysis != null) {
                    onAction(CreatePaymentAction.AttachInvoice(analysis, uri))
                }
                pendingAnalysis = null
            },
        )
    if (ui.attachmentUrl != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = DesignTokens.Spacing.m),
        ) {
            Icon(Icons.Filled.ReceiptLong, contentDescription = null)
            Spacer()
            Text(stringResource(Res.string.payments_invoice_attached), style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        OutlinedButton(
            onClick = launchInvoicePicker,
            modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.m),
            shape = DesignTokens.Shape.button,
        ) {
            Icon(Icons.Filled.ReceiptLong, contentDescription = null)
            Spacer()
            Text(stringResource(Res.string.payments_attach_invoice))
        }
    }
}

@Composable
private fun Spacer() = androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))

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
