package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.forms.ui.FormRenderer
import com.mileway.core.network.model.Office
import com.mileway.core.network.model.PolicyViolation
import com.mileway.core.network.model.SubmissionStatus
import com.mileway.core.ui.components.expense.ExpenseContextSummaryCard
import com.mileway.core.ui.components.expense.hasSummary
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_add_expense_title
import com.mileway.core.ui.resources.logging_add_row
import com.mileway.core.ui.resources.logging_amount
import com.mileway.core.ui.resources.logging_amount_requires_approval
import com.mileway.core.ui.resources.logging_amount_rupees_label
import com.mileway.core.ui.resources.logging_apply_category_to_all
import com.mileway.core.ui.resources.logging_attach_receipt_cd
import com.mileway.core.ui.resources.logging_attach_receipt_optional
import com.mileway.core.ui.resources.logging_attached_receipt_photo_cd
import com.mileway.core.ui.resources.logging_back_cd
import com.mileway.core.ui.resources.logging_bulk_entry
import com.mileway.core.ui.resources.logging_bulk_prompt
import com.mileway.core.ui.resources.logging_category_accommodation
import com.mileway.core.ui.resources.logging_category_communication
import com.mileway.core.ui.resources.logging_category_food
import com.mileway.core.ui.resources.logging_category_medical
import com.mileway.core.ui.resources.logging_category_office_supplies
import com.mileway.core.ui.resources.logging_category_other
import com.mileway.core.ui.resources.logging_category_prompt
import com.mileway.core.ui.resources.logging_category_travel
import com.mileway.core.ui.resources.logging_currency_label
import com.mileway.core.ui.resources.logging_discard
import com.mileway.core.ui.resources.logging_duplicate_row_cd
import com.mileway.core.ui.resources.logging_expense_details_header
import com.mileway.core.ui.resources.logging_expense_input_subtitle
import com.mileway.core.ui.resources.logging_expense_step_of_2
import com.mileway.core.ui.resources.logging_import_csv
import com.mileway.core.ui.resources.logging_merchant
import com.mileway.core.ui.resources.logging_merchant_name_placeholder
import com.mileway.core.ui.resources.logging_merchant_vendor_label
import com.mileway.core.ui.resources.logging_next
import com.mileway.core.ui.resources.logging_note_optional_label
import com.mileway.core.ui.resources.logging_note_placeholder
import com.mileway.core.ui.resources.logging_project_cost_center_label
import com.mileway.core.ui.resources.logging_receipt_attached
import com.mileway.core.ui.resources.logging_receipt_attached_cd
import com.mileway.core.ui.resources.logging_remove_receipt_cd
import com.mileway.core.ui.resources.logging_remove_row_cd
import com.mileway.core.ui.resources.logging_resume
import com.mileway.core.ui.resources.logging_resume_draft_generic
import com.mileway.core.ui.resources.logging_resume_draft_named
import com.mileway.core.ui.resources.logging_resume_draft_title
import com.mileway.core.ui.resources.logging_row_needs_attention
import com.mileway.core.ui.resources.logging_save_draft
import com.mileway.core.ui.resources.logging_select_a_category
import com.mileway.core.ui.resources.logging_select_category
import com.mileway.core.ui.resources.logging_select_office_placeholder
import com.mileway.core.ui.resources.logging_submit_expense
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.logging.catalog.ExpenseCategoryCatalog
import com.mileway.feature.logging.catalog.ExpenseCustomFormCatalog
import com.mileway.feature.logging.currency.CurrencyConverter
import com.mileway.feature.logging.model.DraftStatus
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseDraftRow
import com.mileway.feature.logging.ui.sheets.ExpensePolicyViolationSheet
import com.mileway.feature.logging.validation.ExpenseFormValidator
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseEffect
import com.mileway.feature.logging.viewmodel.ExpenseFormState
import com.mileway.feature.logging.viewmodel.ExpenseUiState
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import com.mileway.stub.PolicyMockData
import com.siddharth.kmp.common.asString
import com.siddharth.kmp.common.formatDecimal
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * V27 P27.E.1 (E-STRUCT): the expense flow's single in-place 2-step wizard — replaces the old
 * disconnected entry (category picker)/details (amount+merchant+submit) nav-graph screens.
 * [com.mileway.feature.logging.viewmodel.ExpenseFormState.step] drives which step renders; step
 * transitions ([ExpenseAction.AdvanceStep]/[ExpenseAction.RetreatStep]) are in-place state changes,
 * not navigation — [onBack] only fires from step 1 (hardware/topbar back from step 2 retreats to
 * step 1 instead). [EXPENSE_SUCCESS][com.mileway.feature.logging.ui.navigation.LoggingRoutes
 * .EXPENSE_SUCCESS] stays its own nav destination, reached via [onSubmitted].
 *
 * Bulk entry (the CSV-import/multi-row grid) is orthogonal to the single-item wizard — its own
 * toggle, unaffected by [ExpenseFormState.step], preserved unchanged from before this task.
 *
 * Preserves Mileway's own pieces the reference app didn't have: the resume-draft card, the live
 * (non-blocking) policy preview banner on step 2, and the explicit Save Draft action — all folded
 * into the wizard rather than dropped. V27 P27.E.3 adds the second validation channel: a
 * [ExpensePolicyViolationSheet] `ModalBottomSheet` shown on submit for a tiered-policy outcome,
 * while per-field errors (category/amount/merchant/office/custom-form) stay inline as before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    var bulkMode by remember { mutableStateOf(false) }
    var policyViolations by remember { mutableStateOf<List<PolicyViolation>?>(null) }
    val form = ui.form

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExpenseEffect.NavigateToSuccess -> onSubmitted()
                ExpenseEffect.NavigateBack -> onBack()
                is ExpenseEffect.ShowToast -> Unit
                is ExpenseEffect.ShowPolicySheet -> policyViolations = effect.violations
            }
        }
    }

    val onBackOrRetreat: () -> Unit = {
        if (!bulkMode && form.step == 2) viewModel.onAction(ExpenseAction.RetreatStep) else onBack()
    }

    Scaffold(
        topBar = {
            Column {
                DepthAwareTopBar(
                    title =
                        if (form.step == 1) {
                            stringResource(Res.string.logging_add_expense_title)
                        } else {
                            form.category?.label ?: stringResource(Res.string.logging_expense_details_header)
                        },
                    subtitle =
                        when {
                            bulkMode -> stringResource(Res.string.logging_bulk_entry)
                            form.step == 1 -> stringResource(Res.string.logging_select_a_category)
                            else -> stringResource(Res.string.logging_expense_input_subtitle)
                        },
                    depth = NavigationDepth.LEVEL_1,
                    navigationIcon = {
                        IconButton(onClick = onBackOrRetreat) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.logging_back_cd))
                        }
                    },
                )
                if (!bulkMode) {
                    LinearProgressIndicator(progress = { form.step / 2f }, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(Res.string.logging_expense_step_of_2, form.step),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.xs),
                    )
                }
            }
        },
        bottomBar = {
            if (!bulkMode) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.l),
                    ) {
                        if (form.step == 1) {
                            Button(
                                onClick = { viewModel.onAction(ExpenseAction.AdvanceStep) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = DesignTokens.Shape.button,
                            ) {
                                Text(stringResource(Res.string.logging_next))
                            }
                        } else {
                            Button(
                                onClick = { viewModel.onAction(ExpenseAction.SubmitExpense) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = DesignTokens.Shape.button,
                            ) {
                                Text(stringResource(Res.string.logging_submit_expense))
                            }
                            Spacer(Modifier.height(DesignTokens.Spacing.s))
                            OutlinedButton(
                                onClick = { viewModel.onAction(ExpenseAction.SaveDraft) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = DesignTokens.Shape.button,
                            ) {
                                Text(stringResource(Res.string.logging_save_draft))
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        when {
            bulkMode ->
                BulkModeContent(
                    modifier = modifier.padding(innerPadding),
                    ui = ui,
                    viewModel = viewModel,
                    bulkMode = bulkMode,
                    onBulkModeChange = { bulkMode = it },
                )
            form.step == 1 ->
                Step1Content(
                    modifier = modifier.padding(innerPadding),
                    ui = ui,
                    viewModel = viewModel,
                    bulkMode = bulkMode,
                    onBulkModeChange = { bulkMode = it },
                )
            else ->
                Step2Content(
                    modifier = modifier.padding(innerPadding),
                    ui = ui,
                    viewModel = viewModel,
                )
        }
    }

    policyViolations?.let { violations ->
        ExpensePolicyViolationSheet(
            violations = violations,
            onSubmitAnyway = {
                policyViolations = null
                viewModel.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
            },
            onReview = { policyViolations = null },
            onDismiss = { policyViolations = null },
        )
    }
}

/** Step 1: category selection + context summary + attachments + the bulk-entry toggle. */
@Composable
private fun Step1Content(
    ui: ExpenseUiState,
    viewModel: ExpenseViewModel,
    bulkMode: Boolean,
    onBulkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = ui.form
    val categoryLocked = ExpenseFormValidator.FIELD_CATEGORY in ExpenseFormValidator.lockedFieldKeys(form.sourceContext)
    // Scoped to this composable (not hoisted to ExpenseScreen) so it's only composed while step 1
    // is actually on screen — mirrors how the bulk-grid's per-row launcher is scoped to each row.
    val launchReceiptPicker = rememberReceiptAttachmentLauncher { path -> viewModel.onAction(ExpenseAction.SetReceiptImage(path)) }

    // Mirrors the pre-P27.E.1 layout strategy: a plain (non-scrolling) fillMaxSize Column whose
    // fixed-size header content (context card/resume-draft/attachments/error) sits above the
    // category grid, which is the one child that scrolls internally to absorb any overflow.
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.Spacing.l)
                .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(DesignTokens.Spacing.l))

        // V27 P27.E.2/E.1: the context-summary shell (P25.A5) finally mounted — renders nothing
        // for None/Regular (a bare "Add Expense" tap), the per-variant summary otherwise.
        ExpenseContextSummaryCard(context = form.sourceContext)
        if (form.sourceContext.hasSummary()) Spacer(Modifier.height(DesignTokens.Spacing.l))

        ui.resumableDraft?.let { draft ->
            ResumeDraftCard(
                merchantName = draft.merchantName,
                onResume = { viewModel.onAction(ExpenseAction.ResumeDraft) },
                onDiscard = { viewModel.onAction(ExpenseAction.DiscardDraft) },
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))
        }

        ReceiptAttachmentRow(
            receiptImagePath = form.receiptImagePath,
            onAttach = launchReceiptPicker,
            onRemove = { viewModel.onAction(ExpenseAction.SetReceiptImage(null)) },
        )
        Spacer(Modifier.height(DesignTokens.Spacing.l))

        val categoryError = form.errors[ExpenseFormValidator.FIELD_CATEGORY]
        if (categoryError != null) {
            Text(
                text = categoryError.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.logging_category_prompt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FilterChip(
                selected = bulkMode,
                onClick = { onBulkModeChange(true) },
                label = { Text(stringResource(Res.string.logging_bulk_entry)) },
            )
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            contentPadding = PaddingValues(bottom = DesignTokens.Spacing.xl),
        ) {
            items(ExpenseCategoryCatalog.default()) { categoryDef ->
                CategoryTile(
                    category = categoryDef.category,
                    selected = form.category == categoryDef.category,
                    enabled = !categoryLocked,
                    onClick = { viewModel.onAction(ExpenseAction.SelectCategory(categoryDef.category)) },
                )
            }
        }
    }
}

/** Bulk-entry mode: CSV import + apply-category-to-all + the multi-row draft grid, unchanged from before P27.E.1. */
@Composable
private fun BulkModeContent(
    ui: ExpenseUiState,
    viewModel: ExpenseViewModel,
    bulkMode: Boolean,
    onBulkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.Spacing.l)
                .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(DesignTokens.Spacing.l))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.logging_bulk_prompt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FilterChip(
                selected = bulkMode,
                onClick = { onBulkModeChange(false) },
                label = { Text(stringResource(Res.string.logging_bulk_entry)) },
            )
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        // P2.4: local CSV/TSV bulk-import — parses the picked file's raw text into rows appended
        // alongside whatever the grid already has.
        val importCsv = rememberExpenseCsvImportLauncher { text -> viewModel.onAction(ExpenseAction.ImportCsv(text)) }
        OutlinedButton(onClick = importCsv, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
            Icon(Icons.Filled.UploadFile, contentDescription = null)
            Text(stringResource(Res.string.logging_import_csv))
        }
        Spacer(Modifier.height(DesignTokens.Spacing.m))

        // P27.E.11: apply one category to every still-PENDING row in a single tap, instead of
        // picking it row-by-row — wires the previously-unused ApplyCategoryToAll action.
        if (ui.rows.size > 1) {
            ApplyCategoryToAllRow(
                onCategorySelected = { category -> viewModel.onAction(ExpenseAction.ApplyCategoryToAll(category)) },
            )
            Spacer(Modifier.height(DesignTokens.Spacing.m))
        }

        BulkDraftGrid(
            rows = ui.rows,
            onAddRow = { viewModel.onAction(ExpenseAction.AddDraftRow) },
            onDuplicateRow = { id -> viewModel.onAction(ExpenseAction.DuplicateDraftRow(id)) },
            onRemoveRow = { id -> viewModel.onAction(ExpenseAction.RemoveDraftRow(id)) },
            onMerchantChange = { id, value ->
                viewModel.onAction(ExpenseAction.UpdateDraftRow(id) { it.copy(merchantName = value) })
            },
            onAmountChange = { id, value ->
                viewModel.onAction(ExpenseAction.UpdateDraftRow(id) { it.copy(amountText = value) })
            },
            onCategoryChange = { id, category ->
                viewModel.onAction(ExpenseAction.UpdateDraftRow(id) { it.copy(category = category) })
            },
            // P2.5: per-row receipt attachment — scans into that row's own receiptImagePath only.
            onReceiptScanned = { id, path ->
                viewModel.onAction(ExpenseAction.UpdateDraftRow(id) { it.copy(receiptImagePath = path) })
            },
        )
    }
}

/**
 * Step 2: amount/currency/merchant/office + step-2's custom form ([ExpenseCustomFormCatalog]) +
 * the live policy preview + submit. Amount/currency/merchant/office/note/receipt fields are
 * unchanged from the old `ExpenseDetailsInputScreen`; the receipt-attachment row moved to step 1
 * (P27.E.1 spec) and the submit/save-draft buttons moved into [ExpenseScreen]'s shared bottom bar.
 */
@Composable
private fun Step2Content(
    ui: ExpenseUiState,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier,
) {
    val form = ui.form
    val catalogDef = ExpenseCategoryCatalog.default().firstOrNull { it.category == form.category }
    val lockedKeys = ExpenseFormValidator.lockedFieldKeys(form.sourceContext)
    val customFormSchema = ExpenseCustomFormCatalog.schemaFor(catalogDef)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.l)
                .imePadding(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        Spacer(Modifier.height(DesignTokens.Spacing.s))

        val amountError = form.errors[ExpenseFormValidator.FIELD_AMOUNT]
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            OutlinedTextField(
                value = form.amountText,
                onValueChange = { viewModel.onAction(ExpenseAction.SetAmount(it)) },
                label = { Text(stringResource(Res.string.logging_amount_rupees_label)) },
                placeholder = { Text("0.00") },
                prefix = { Text(CurrencyConverter.symbolFor(form.currencyCode)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it.asString()) } },
                modifier = Modifier.weight(1f),
            )
            CurrencyPickerField(
                selectedCode = form.currencyCode,
                onSelect = { code -> viewModel.onAction(ExpenseAction.SetCurrency(code)) },
                modifier = Modifier.width(110.dp),
            )
        }

        // P27.E.15: local, static-table conversion preview — informational only, never applied
        // to the amount actually stored/checked against policy (see ExpenseFormState.currencyCode).
        if (form.currencyCode != "INR") {
            val liveAmountForConversion = form.amountText.toDoubleOrNull() ?: 0.0
            val convertedRupees = CurrencyConverter.toRupees(liveAmountForConversion, form.currencyCode)
            Text(
                text = "≈ ₹${convertedRupees.formatDecimal(2)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val merchantError = form.errors[ExpenseFormValidator.FIELD_MERCHANT_NAME]
        OutlinedTextField(
            value = form.merchantName,
            onValueChange = { viewModel.onAction(ExpenseAction.SetMerchant(it)) },
            label = { Text(stringResource(Res.string.logging_merchant_vendor_label)) },
            placeholder = { Text(stringResource(Res.string.logging_merchant_name_placeholder)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            // P27.E.4/E.1: DiCE's `editableTypesForCard` — a Card-context expense pins merchant to
            // the actual transaction, finally wired read-only here (validator locked it since P27.E.4).
            enabled = ExpenseFormValidator.FIELD_MERCHANT_NAME !in lockedKeys,
            isError = merchantError != null,
            supportingText = merchantError?.let { { Text(it.asString()) } },
            modifier = Modifier.fillMaxWidth(),
        )

        // P1.7: project/cost-center tagging, gated on the category's catalog def — only
        // TRAVEL/ACCOMMODATION/OFFICE_SUPPLIES (requiresCostCenter) show this picker.
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

        // V27 P27.E.1: step-2's custom-form section, rendered through core:forms' shared
        // FormRenderer — empty schema (most categories) renders nothing.
        if (customFormSchema.isNotEmpty()) {
            FormRenderer(
                schema = customFormSchema,
                values = form.formValues,
                onValueChange = { key, value -> viewModel.onAction(ExpenseAction.SetFormValue(key, value)) },
            )
        }

        // P1.6: same tiered policy engine as Log Miles — live, non-blocking preview of the outcome
        // the amount would resolve to on submit. Preserved unchanged from before P27.E.1/E.3: the
        // submit-time policy-violation ModalBottomSheet (see ExpenseScreen) is a second, separate
        // channel, not a replacement for this preview.
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

/**
 * P27.E.11: one-tap "apply this category to every PENDING row" control for the bulk-entry grid —
 * the UI wiring for [ExpenseAction.ApplyCategoryToAll], which was defined (P2.2) but never
 * reachable from a screen until this task.
 */
@Composable
private fun ApplyCategoryToAllRow(
    onCategorySelected: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.logging_apply_category_to_all),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            ExpenseCategoryCatalog.default().forEach { categoryDef ->
                FilterChip(
                    selected = false,
                    onClick = { onCategorySelected(categoryDef.category) },
                    label = { Text(categoryDef.category.localizedLabel()) },
                )
            }
        }
    }
}

/**
 * P2.1: multi-row draft grid for bulk expense entry — the local, offline equivalent of the
 * reference app's bulk-entry screen structure (add/duplicate/remove rows), rendered with
 * Mileway's own card + [DesignTokens] styling rather than a port of the reference UI.
 */
@Composable
private fun BulkDraftGrid(
    rows: List<ExpenseDraftRow>,
    onAddRow: () -> Unit,
    onDuplicateRow: (String) -> Unit,
    onRemoveRow: (String) -> Unit,
    onMerchantChange: (String, String) -> Unit,
    onAmountChange: (String, String) -> Unit,
    onCategoryChange: (String, ExpenseCategory) -> Unit,
    onReceiptScanned: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        contentPadding = PaddingValues(bottom = DesignTokens.Spacing.xl),
    ) {
        items(rows, key = { it.id }) { row ->
            DraftRowCard(
                row = row,
                canRemove = rows.size > 1,
                onDuplicate = { onDuplicateRow(row.id) },
                onRemove = { onRemoveRow(row.id) },
                onMerchantChange = { value -> onMerchantChange(row.id, value) },
                onAmountChange = { value -> onAmountChange(row.id, value) },
                onCategoryChange = { category -> onCategoryChange(row.id, category) },
                onReceiptScanned = { path -> onReceiptScanned(row.id, path) },
            )
        }
        item {
            OutlinedButton(onClick = onAddRow, modifier = Modifier.fillMaxWidth(), shape = DesignTokens.Shape.button) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(Res.string.logging_add_row))
            }
        }
    }
}

@Composable
private fun DraftRowCard(
    row: ExpenseDraftRow,
    canRemove: Boolean,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onReceiptScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // P2.5: same per-row-scoped scanner launcher the single-entry form uses (P1.4) — this call
    // attaches whatever is scanned to this specific row's receiptImagePath only.
    val launchReceiptScan = rememberReceiptAttachmentLauncher(onPicked = onReceiptScanned)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.category?.label ?: stringResource(Res.string.logging_select_category),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    IconButton(onClick = launchReceiptScan) {
                        Icon(
                            imageVector = if (row.receiptImagePath != null) Icons.Filled.Receipt else Icons.Filled.AddAPhoto,
                            contentDescription =
                                if (row.receiptImagePath != null) {
                                    stringResource(
                                        Res.string.logging_receipt_attached_cd,
                                    )
                                } else {
                                    stringResource(Res.string.logging_attach_receipt_cd)
                                },
                            tint =
                                if (row.receiptImagePath != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(Res.string.logging_duplicate_row_cd))
                    }
                    if (canRemove) {
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.logging_remove_row_cd))
                        }
                    }
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                ExpenseCategoryCatalog.default().forEach { categoryDef ->
                    FilterChip(
                        selected = row.category == categoryDef.category,
                        onClick = { onCategoryChange(categoryDef.category) },
                        label = { Text(categoryDef.category.localizedLabel()) },
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.m))

            OutlinedTextField(
                value = row.merchantName,
                onValueChange = onMerchantChange,
                label = { Text(stringResource(Res.string.logging_merchant)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            OutlinedTextField(
                value = row.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(Res.string.logging_amount)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (row.status == DraftStatus.ERROR) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))
                Text(
                    text = stringResource(Res.string.logging_row_needs_attention),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * P1.5: offers to resume a Room-persisted draft found on entry (kill+relaunch survived). Never
 * blocks starting a fresh expense — dismissing keeps the draft persisted, discarding clears it.
 */
@Composable
private fun ResumeDraftCard(
    merchantName: String,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l)) {
            Text(
                text = stringResource(Res.string.logging_resume_draft_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text =
                    if (merchantName.isNotBlank()) {
                        stringResource(
                            Res.string.logging_resume_draft_named,
                            merchantName,
                        )
                    } else {
                        stringResource(Res.string.logging_resume_draft_generic)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.m))
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                OutlinedButton(onClick = onResume, shape = DesignTokens.Shape.button) { Text(stringResource(Res.string.logging_resume)) }
                TextButton(onClick = onDiscard, shape = DesignTokens.Shape.button) { Text(stringResource(Res.string.logging_discard)) }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: ExpenseCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(DesignTokens.Shape.button)
                .clickable(enabled = enabled, onClick = onClick),
        shape = DesignTokens.Shape.button,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            DesignTokens.Shape.button,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = category.localizedLabel(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
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
 * P27.E.15: currency picker for the amount field, offering [CurrencyConverter.supportedCurrencies]
 * (static local list — no live FX). Mirrors [OfficePickerField]'s dropdown shape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerField(
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedCode,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.logging_currency_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CurrencyConverter.supportedCurrencies.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Optional local receipt attachment row (P1.4), rendered on step 1 (V27 P27.E.1). Shows an
 * "Attach Receipt" affordance when empty, or a thumbnail + remove action once a photo has been
 * picked. Never blocks the step-1 -> step-2 advance — the receipt is optional.
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

/**
 * Localized display label for an expense category. The enum's `label` stays
 * canonical English — it doubles as the search/CSV-import lookup key.
 */
@Composable
internal fun ExpenseCategory.localizedLabel(): String =
    when (this) {
        ExpenseCategory.FOOD -> stringResource(Res.string.logging_category_food)
        ExpenseCategory.TRAVEL -> stringResource(Res.string.logging_category_travel)
        ExpenseCategory.ACCOMMODATION -> stringResource(Res.string.logging_category_accommodation)
        ExpenseCategory.OFFICE_SUPPLIES -> stringResource(Res.string.logging_category_office_supplies)
        ExpenseCategory.COMMUNICATION -> stringResource(Res.string.logging_category_communication)
        ExpenseCategory.MEDICAL -> stringResource(Res.string.logging_category_medical)
        ExpenseCategory.OTHER -> stringResource(Res.string.logging_category_other)
    }
