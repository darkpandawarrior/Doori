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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_add_expense_title
import com.mileway.core.ui.resources.logging_add_row
import com.mileway.core.ui.resources.logging_amount
import com.mileway.core.ui.resources.logging_attach_receipt_cd
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
import com.mileway.core.ui.resources.logging_discard
import com.mileway.core.ui.resources.logging_duplicate_row_cd
import com.mileway.core.ui.resources.logging_import_csv
import com.mileway.core.ui.resources.logging_merchant
import com.mileway.core.ui.resources.logging_receipt_attached_cd
import com.mileway.core.ui.resources.logging_remove_row_cd
import com.mileway.core.ui.resources.logging_resume
import com.mileway.core.ui.resources.logging_resume_draft_generic
import com.mileway.core.ui.resources.logging_resume_draft_named
import com.mileway.core.ui.resources.logging_resume_draft_title
import com.mileway.core.ui.resources.logging_row_needs_attention
import com.mileway.core.ui.resources.logging_select_a_category
import com.mileway.core.ui.resources.logging_select_category
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.logging.catalog.ExpenseCategoryCatalog
import com.mileway.feature.logging.model.DraftStatus
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseDraftRow
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryScreen(
    onBack: () -> Unit,
    onCategorySelected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    var bulkMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.logging_add_expense_title),
                subtitle = if (bulkMode) stringResource(Res.string.logging_bulk_entry) else stringResource(Res.string.logging_select_a_category),
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.logging_back_cd))
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.l))

            ui.resumableDraft?.let { draft ->
                ResumeDraftCard(
                    merchantName = draft.merchantName,
                    onResume = {
                        viewModel.onAction(ExpenseAction.ResumeDraft)
                        onCategorySelected()
                    },
                    onDiscard = { viewModel.onAction(ExpenseAction.DiscardDraft) },
                )
                Spacer(Modifier.height(DesignTokens.Spacing.l))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (bulkMode) stringResource(Res.string.logging_bulk_prompt) else stringResource(Res.string.logging_category_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FilterChip(
                    selected = bulkMode,
                    onClick = { bulkMode = !bulkMode },
                    label = { Text(stringResource(Res.string.logging_bulk_entry)) },
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))

            if (bulkMode) {
                // P2.4: local CSV/TSV bulk-import — parses the picked file's raw text into rows
                // appended alongside whatever the grid already has.
                val importCsv = rememberExpenseCsvImportLauncher { text -> viewModel.onAction(ExpenseAction.ImportCsv(text)) }
                OutlinedButton(onClick = importCsv, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Text(stringResource(Res.string.logging_import_csv))
                }
                Spacer(Modifier.height(DesignTokens.Spacing.m))

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
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                    contentPadding = PaddingValues(bottom = DesignTokens.Spacing.xl),
                ) {
                    items(ExpenseCategoryCatalog.default()) { categoryDef ->
                        CategoryTile(
                            category = categoryDef.category,
                            onClick = {
                                viewModel.onAction(ExpenseAction.SelectCategory(categoryDef.category))
                                onCategorySelected()
                            },
                        )
                    }
                }
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
            OutlinedButton(onClick = onAddRow, modifier = Modifier.fillMaxWidth()) {
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
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
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
                OutlinedButton(onClick = onResume) { Text(stringResource(Res.string.logging_resume)) }
                TextButton(onClick = onDiscard) { Text(stringResource(Res.string.logging_discard)) }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: ExpenseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(DesignTokens.Shape.roundedMd)
                .clickable(onClick = onClick),
        shape = DesignTokens.Shape.roundedMd,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                            RoundedCornerShape(10.dp),
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
