package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.common.asString
import com.mileway.core.network.model.Office
import com.mileway.core.network.model.SubmissionStatus
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_amount_requires_approval
import com.mileway.core.ui.resources.logging_amount_rupees_label
import com.mileway.core.ui.resources.logging_attach_receipt_optional
import com.mileway.core.ui.resources.logging_attached_receipt_photo_cd
import com.mileway.core.ui.resources.logging_back_cd
import com.mileway.core.ui.resources.logging_expense_details_header
import com.mileway.core.ui.resources.logging_expense_input_subtitle
import com.mileway.core.ui.resources.logging_merchant_vendor_label
import com.mileway.core.ui.resources.logging_note_optional_label
import com.mileway.core.ui.resources.logging_note_placeholder
import com.mileway.core.ui.resources.logging_project_cost_center_label
import com.mileway.core.ui.resources.logging_receipt_attached
import com.mileway.core.ui.resources.logging_remove_receipt_cd
import com.mileway.core.ui.resources.logging_save_draft
import com.mileway.core.ui.resources.logging_select_office_placeholder
import com.mileway.core.ui.resources.logging_submit_expense
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.logging.catalog.ExpenseCategoryCatalog
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.validation.ExpenseFormValidator
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseEffect
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import com.mileway.stub.PolicyMockData
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsInputScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val form = ui.form
    val launchReceiptPicker =
        rememberReceiptAttachmentLauncher { path ->
            viewModel.onAction(ExpenseAction.SetReceiptImage(path))
        }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExpenseEffect.NavigateToSuccess -> onSubmitted()
                ExpenseEffect.NavigateBack -> onBack()
                is ExpenseEffect.ShowToast -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = form.category?.label ?: stringResource(Res.string.logging_expense_details_header),
                subtitle = stringResource(Res.string.logging_expense_input_subtitle),
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.logging_back_cd))
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.l),
                ) {
                    Button(
                        onClick = { viewModel.onAction(ExpenseAction.SubmitExpense) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.logging_submit_expense))
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    OutlinedButton(
                        onClick = { viewModel.onAction(ExpenseAction.SaveDraft) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.logging_save_draft))
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .imePadding(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.s))

            form.category?.let { cat ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = cat.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            val amountError = form.errors[ExpenseFormValidator.FIELD_AMOUNT]
            OutlinedTextField(
                value = form.amountText,
                onValueChange = { viewModel.onAction(ExpenseAction.SetAmount(it)) },
                label = { Text(stringResource(Res.string.logging_amount_rupees_label)) },
                placeholder = { Text("0.00") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it.asString()) } },
                modifier = Modifier.fillMaxWidth(),
            )

            val merchantError = form.errors[ExpenseFormValidator.FIELD_MERCHANT_NAME]
            OutlinedTextField(
                value = form.merchantName,
                onValueChange = { viewModel.onAction(ExpenseAction.SetMerchant(it)) },
                label = { Text(stringResource(Res.string.logging_merchant_vendor_label)) },
                placeholder = { Text("e.g. Swiggy, Ola Cabs") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                isError = merchantError != null,
                supportingText = merchantError?.let { { Text(it.asString()) } },
                modifier = Modifier.fillMaxWidth(),
            )

            // P1.7: project/cost-center tagging, gated on the category's catalog def — only
            // TRAVEL/ACCOMMODATION/OFFICE_SUPPLIES (requiresCostCenter) show this picker.
            val catalogDef = ExpenseCategoryCatalog.default().firstOrNull { it.category == form.category }
            if (catalogDef?.requiresCostCenter == true) {
                val officeError = form.errors[ExpenseFormValidator.FIELD_OFFICE_CODE]
                OfficePickerField(
                    selectedOfficeCode = form.officeCode,
                    isError = officeError != null,
                    supportingText = officeError?.asString(),
                    onSelect = { office -> viewModel.onAction(ExpenseAction.SetOfficeCode(office.code)) },
                )
            }

            OutlinedTextField(
                value = form.note,
                onValueChange = { viewModel.onAction(ExpenseAction.SetNote(it)) },
                label = { Text(stringResource(Res.string.logging_note_optional_label)) },
                placeholder = { Text(stringResource(Res.string.logging_note_placeholder)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            ReceiptAttachmentRow(
                receiptImagePath = form.receiptImagePath,
                onAttach = launchReceiptPicker,
                onRemove = { viewModel.onAction(ExpenseAction.SetReceiptImage(null)) },
            )

            // P1.6: same tiered policy engine as Log Miles — live preview of the outcome the
            // amount would resolve to on submit, instead of a single hardcoded literal check.
            val liveAmount = form.amountText.toDoubleOrNull() ?: 0.0
            val liveCategoryName = (form.category ?: ExpenseCategory.OTHER).name
            val liveOutcome = PolicyMockData.outcomeForExpenseAmount(liveAmount, liveCategoryName)
            if (liveOutcome != SubmissionStatus.SUCCESS) {
                val liveViolation = PolicyMockData.violationsForExpenseAmount(liveAmount, liveCategoryName).firstOrNull()
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = DesignTokens.Shape.roundedSm,
                ) {
                    Text(
                        text = "⚠ " + (liveViolation?.message ?: stringResource(Res.string.logging_amount_requires_approval)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(DesignTokens.Spacing.m),
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xxl))
        }
    }
}

/**
 * Project/cost-center picker (P1.7), sourced from [PolicyMockData.offices] — the same stub office
 * catalog the mileage submission flow bills against. Only rendered by the caller when the selected
 * category's [com.mileway.feature.logging.model.ExpenseCategoryDef.requiresCostCenter] is true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficePickerField(
    selectedOfficeCode: String?,
    isError: Boolean,
    supportingText: String?,
    onSelect: (Office) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val offices = remember { PolicyMockData.offices() }
    val selectedOffice = offices.firstOrNull { it.code == selectedOfficeCode }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedOffice?.let { "${it.name} (${it.code})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.logging_project_cost_center_label)) },
            placeholder = { Text(stringResource(Res.string.logging_select_office_placeholder)) },
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            offices.forEach { office ->
                DropdownMenuItem(
                    text = { Text("${office.name} (${office.code})") },
                    onClick = {
                        onSelect(office)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Optional local receipt attachment row (P1.4). Shows an "Attach Receipt" affordance when empty,
 * or a thumbnail + remove action once a photo has been picked. Never blocks Submit — the receipt
 * is optional.
 */
@Composable
private fun ReceiptAttachmentRow(
    receiptImagePath: String?,
    onAttach: () -> Unit,
    onRemove: () -> Unit,
) {
    if (receiptImagePath == null) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onAttach),
            shape = DesignTokens.Shape.roundedMd,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.logging_attach_receipt_optional),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(modifier = Modifier.size(56.dp)) {
                AsyncImage(
                    model = receiptImagePath,
                    contentDescription = stringResource(Res.string.logging_attached_receipt_photo_cd),
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(0.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, DesignTokens.Shape.roundedSm),
                )
            }
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(Res.string.logging_receipt_attached),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.logging_remove_receipt_cd))
            }
        }
    }
}
