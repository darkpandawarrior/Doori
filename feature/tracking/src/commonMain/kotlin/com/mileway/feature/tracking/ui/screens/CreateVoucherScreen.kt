package com.mileway.feature.tracking.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.siddharth.kmp.common.formatDecimal
import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_plural_expenses_selected
import com.mileway.core.ui.resources.tracking_saved_journey_default
import com.mileway.core.ui.resources.tracking_submission_selected_count
import com.mileway.core.ui.resources.tracking_voucher_back_to_detail
import com.mileway.core.ui.resources.tracking_voucher_category_fuel
import com.mileway.core.ui.resources.tracking_voucher_category_label
import com.mileway.core.ui.resources.tracking_voucher_category_maintenance
import com.mileway.core.ui.resources.tracking_voucher_category_mileage
import com.mileway.core.ui.resources.tracking_voucher_category_other
import com.mileway.core.ui.resources.tracking_voucher_create_button
import com.mileway.core.ui.resources.tracking_voucher_created_title
import com.mileway.core.ui.resources.tracking_voucher_deselect_all
import com.mileway.core.ui.resources.tracking_voucher_empty_subtitle
import com.mileway.core.ui.resources.tracking_voucher_empty_title
import com.mileway.core.ui.resources.tracking_voucher_next_details
import com.mileway.core.ui.resources.tracking_voucher_next_review
import com.mileway.core.ui.resources.tracking_voucher_notes_label
import com.mileway.core.ui.resources.tracking_voucher_review_title
import com.mileway.core.ui.resources.tracking_voucher_saved_locally
import com.mileway.core.ui.resources.tracking_voucher_select_all
import com.mileway.core.ui.resources.tracking_voucher_step_confirm_subtitle
import com.mileway.core.ui.resources.tracking_voucher_step_confirm_title
import com.mileway.core.ui.resources.tracking_voucher_step_details_subtitle
import com.mileway.core.ui.resources.tracking_voucher_step_details_title
import com.mileway.core.ui.resources.tracking_voucher_step_select_subtitle
import com.mileway.core.ui.resources.tracking_voucher_step_select_title
import com.mileway.core.ui.resources.tracking_voucher_step_success_subtitle
import com.mileway.core.ui.resources.tracking_voucher_step_success_title
import com.mileway.core.ui.resources.tracking_voucher_summary_category
import com.mileway.core.ui.resources.tracking_voucher_summary_expenses
import com.mileway.core.ui.resources.tracking_voucher_summary_notes
import com.mileway.core.ui.resources.tracking_voucher_summary_title
import com.mileway.core.ui.resources.tracking_voucher_summary_total
import com.mileway.core.ui.resources.tracking_voucher_title_label
import com.mileway.core.ui.resources.tracking_voucher_total
import com.mileway.core.ui.resources.tracking_voucher_total_amount_label
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.tracking.viewmodel.CreateVoucherAction
import com.mileway.feature.tracking.viewmodel.CreateVoucherUiState
import com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel
import com.mileway.feature.tracking.viewmodel.VoucherDeclaration
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVoucherScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateVoucherViewModel = koinViewModel(),
) {
    val uiState by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            DepthAwareTopBar(
                title =
                    when (uiState.step) {
                        0 -> stringResource(Res.string.tracking_voucher_step_select_title)
                        1 -> stringResource(Res.string.tracking_voucher_step_details_title)
                        2 -> stringResource(Res.string.tracking_voucher_step_confirm_title)
                        else -> stringResource(Res.string.tracking_voucher_step_success_title)
                    },
                subtitle =
                    when (uiState.step) {
                        0 -> stringResource(Res.string.tracking_voucher_step_select_subtitle)
                        1 -> stringResource(Res.string.tracking_voucher_step_details_subtitle)
                        2 -> stringResource(Res.string.tracking_voucher_step_confirm_subtitle)
                        else -> stringResource(Res.string.tracking_voucher_step_success_subtitle)
                    },
                titleIcon = Icons.Default.Receipt,
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.step == 0 || uiState.step == 3) {
                            onBack()
                        } else {
                            viewModel.onAction(CreateVoucherAction.GoToStep(uiState.step - 1))
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tracking_cd_back),
                        )
                    }
                },
                actions = {
                    if (uiState.step == 0) {
                        TextButton(
                            shape = DesignTokens.Shape.button,
                            onClick = {
                                if (uiState.selectedTokens.size == uiState.expenses.size) {
                                    viewModel.onAction(CreateVoucherAction.DeselectAll)
                                } else {
                                    viewModel.onAction(CreateVoucherAction.SelectAll)
                                }
                            },
                        ) {
                            Text(
                                if (uiState.selectedTokens.size == uiState.expenses.size) {
                                    stringResource(Res.string.tracking_voucher_deselect_all)
                                } else {
                                    stringResource(Res.string.tracking_voucher_select_all)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (uiState.step < 3) {
                LinearProgressIndicator(
                    progress = { (uiState.step + 1) / 3f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AnimatedContent(targetState = uiState.step, label = "voucher_step") { step ->
                when (step) {
                    0 -> StepSelectExpenses(uiState, viewModel)
                    1 -> StepVoucherDetails(uiState, viewModel)
                    2 -> StepConfirmation(uiState, viewModel)
                    else -> StepSuccess(uiState, onBack)
                }
            }
        }
    }
}

@Composable
private fun StepSelectExpenses(
    uiState: CreateVoucherUiState,
    viewModel: CreateVoucherViewModel,
) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        if (uiState.expenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(Res.string.tracking_voucher_empty_title),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(Res.string.tracking_voucher_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.expenses, key = { it.token }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        selected = uiState.selectedTokens.contains(expense.token),
                        onToggle = { viewModel.onAction(CreateVoucherAction.ToggleSelection(expense.token)) },
                    )
                    HorizontalDivider()
                }
            }

            Column(Modifier.padding(16.dp)) {
                if (uiState.selectedTokens.isNotEmpty()) {
                    Text(
                        stringResource(Res.string.tracking_voucher_total, viewModel.totalAmount.formatDecimal(2)),
                        style = MaterialTheme.typography.titleMedium.dataStyle(),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    shape = DesignTokens.Shape.button,
                    onClick = { viewModel.onAction(CreateVoucherAction.GoToStep(1)) },
                    enabled = uiState.selectedTokens.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.tracking_voucher_next_details))
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: TrackDisplayData,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                expense.name
                    ?: "${stringResource(Res.string.tracking_saved_journey_default)} ${expense.token.take(6)}",
            )
        },
        supportingContent = {
            Text(
                run {
                    val ldt =
                        Instant.fromEpochMilliseconds(expense.startTime)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    val monthName =
                        ldt.month.name.lowercase()
                            .replaceFirstChar { it.uppercase() }.take(3)
                    "${ldt.dayOfMonth} $monthName ${ldt.year}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        },
        trailingContent = {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        "₹${expense.reimbursableAmount.formatDecimal(0)}",
                        style = MaterialTheme.typography.labelLarge.dataStyle(),
                    )
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepVoucherDetails(
    uiState: CreateVoucherUiState,
    viewModel: CreateVoucherViewModel,
) {
    var categoryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.onAction(CreateVoucherAction.SetTitle(it)) },
            label = { Text(stringResource(Res.string.tracking_voucher_title_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
        ) {
            OutlinedTextField(
                value = uiState.category.localizedLabel(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.tracking_voucher_category_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
            ) {
                VoucherCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.localizedLabel()) },
                        onClick = {
                            viewModel.onAction(CreateVoucherAction.SetCategory(cat))
                            categoryExpanded = false
                        },
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(Res.string.tracking_voucher_total_amount_label), style = MaterialTheme.typography.labelMedium)
                    Text(
                        "₹${viewModel.totalAmount.formatDecimal(2)}",
                        style = MaterialTheme.typography.headlineMedium.dataStyle(),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    pluralStringResource(
                        Res.plurals.tracking_plural_expenses_selected,
                        uiState.selectedTokens.size,
                        uiState.selectedTokens.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = { viewModel.onAction(CreateVoucherAction.SetNotes(it)) },
            label = { Text(stringResource(Res.string.tracking_voucher_notes_label)) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            shape = DesignTokens.Shape.button,
            onClick = { viewModel.onAction(CreateVoucherAction.GoToStep(2)) },
            enabled = uiState.title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.tracking_voucher_next_review))
        }
    }
}

@Composable
private fun StepConfirmation(
    uiState: CreateVoucherUiState,
    viewModel: CreateVoucherViewModel,
    declaration: VoucherDeclaration = VoucherDeclaration(),
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(Res.string.tracking_voucher_review_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow(stringResource(Res.string.tracking_voucher_summary_title), uiState.title)
                SummaryRow(stringResource(Res.string.tracking_voucher_summary_category), uiState.category.localizedLabel())
                SummaryRow(stringResource(Res.string.tracking_voucher_summary_total), "₹${viewModel.totalAmount.formatDecimal(2)}")
                SummaryRow(
                    stringResource(Res.string.tracking_voucher_summary_expenses),
                    stringResource(Res.string.tracking_submission_selected_count, uiState.selectedTokens.size),
                )
                if (uiState.notes.isNotBlank()) SummaryRow(stringResource(Res.string.tracking_voucher_summary_notes), uiState.notes)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.declarationAcknowledged,
                onCheckedChange = { viewModel.onAction(CreateVoucherAction.ToggleDeclaration(it)) },
            )
            Text(
                declaration.message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            shape = DesignTokens.Shape.button,
            onClick = { viewModel.onAction(CreateVoucherAction.Submit) },
            enabled = uiState.declarationAcknowledged && !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.tracking_voucher_create_button))
            }
        }
    }
}

@Composable
private fun StepSuccess(
    uiState: CreateVoucherUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MilewayColors.success,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(Res.string.tracking_voucher_created_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        uiState.submittedVoucherNumber?.let { number ->
            Text(
                number,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(Res.string.tracking_voucher_saved_locally),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            shape = DesignTokens.Shape.button,
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.tracking_voucher_back_to_detail))
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

/**
 * Localized display label for a voucher category. The enum's `label` stays
 * canonical English — it is the value persisted in the voucher DB column.
 */
@Composable
private fun VoucherCategory.localizedLabel(): String =
    when (this) {
        VoucherCategory.MILEAGE -> stringResource(Res.string.tracking_voucher_category_mileage)
        VoucherCategory.FUEL -> stringResource(Res.string.tracking_voucher_category_fuel)
        VoucherCategory.MAINTENANCE -> stringResource(Res.string.tracking_voucher_category_maintenance)
        VoucherCategory.OTHER -> stringResource(Res.string.tracking_voucher_category_other)
    }
