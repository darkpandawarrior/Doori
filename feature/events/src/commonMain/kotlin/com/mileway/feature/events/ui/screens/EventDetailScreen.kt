package com.mileway.feature.events.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.util.CommonUtils
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.StatusChip
import com.mileway.core.ui.components.StatusTone
import com.mileway.core.ui.components.scaffold.DetailSection
import com.mileway.core.ui.components.scaffold.TransactionDetailScaffold
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.sheet.SortBottomSheet
import com.mileway.core.ui.components.sheet.SortOption
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.ScreenStateContent
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.action_cancel
import com.mileway.core.ui.resources.action_save
import com.mileway.core.ui.resources.events_action_approve
import com.mileway.core.ui.resources.events_action_create_expense
import com.mileway.core.ui.resources.events_action_delete_confirm
import com.mileway.core.ui.resources.events_action_link_expenses
import com.mileway.core.ui.resources.events_action_reject
import com.mileway.core.ui.resources.events_delete_description
import com.mileway.core.ui.resources.events_delete_title
import com.mileway.core.ui.resources.events_detail_actual_amount
import com.mileway.core.ui.resources.events_detail_budgeted_amount
import com.mileway.core.ui.resources.events_detail_per_head
import com.mileway.core.ui.resources.events_detail_section_linked_expenses
import com.mileway.core.ui.resources.events_detail_section_summary
import com.mileway.core.ui.resources.events_edit_budget_field
import com.mileway.core.ui.resources.events_edit_sheet_title
import com.mileway.core.ui.resources.events_field_category
import com.mileway.core.ui.resources.events_link_confirm
import com.mileway.core.ui.resources.events_link_sheet_title
import com.mileway.core.ui.resources.events_no_expenses_to_link
import com.mileway.core.ui.resources.events_no_linked_expenses
import com.mileway.core.ui.resources.events_toast_deleted
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.events.model.EventCategory
import com.mileway.feature.events.model.EventRecord
import com.mileway.feature.events.model.EventStatus
import com.mileway.feature.events.model.LinkedExpense
import com.mileway.feature.events.viewmodel.EventDetailAction
import com.mileway.feature.events.viewmodel.EventDetailEffect
import com.mileway.feature.events.viewmodel.EventDetailUiState
import com.mileway.feature.events.viewmodel.EventDetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * V29 P29.E.1: event detail — two [SectionCard]s (Summary, Linked expenses) on the shared
 * [TransactionDetailScaffold]. A single [DetailSection.Details] "tab" (tabs.size == 1 hides the
 * tab row) since the whole surface is two SectionCards, not a multi-tab layout.
 */
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onLogExpense: (ExpenseSourceContext) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = koinViewModel { parametersOf(eventId) },
) {
    val ui by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val toastDeleted = stringResource(Res.string.events_toast_deleted)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                EventDetailEffect.Deleted -> {
                    Toasts.show(toastDeleted, eventId, ToastType.Success)
                    onBack()
                }
                is EventDetailEffect.NavigateToExpenseEntry -> onLogExpense(effect.context)
            }
        }
    }

    val event = (ui.event as? ScreenState.Content)?.data

    TransactionDetailScaffold(
        title = event?.title ?: eventId,
        subtitle = event?.venue,
        titleIcon = Icons.Filled.Event,
        tabs = listOf(DetailSection.Details),
        selectedTab = DetailSection.Details,
        onSelectTab = {},
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        actions = {
            if (event != null) {
                IconButton(onClick = { viewModel.onAction(EventDetailAction.OpenEdit) }) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                }
                if (event.isDeletable) {
                    IconButton(onClick = { viewModel.onAction(EventDetailAction.RequestDelete) }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }
        },
    ) {
        ScreenStateContent(
            state = ui.event,
            modifier = Modifier.fillMaxWidth(),
            onRetry = { viewModel.onAction(EventDetailAction.Refresh) },
        ) { loaded ->
            EventDetailContent(loaded, viewModel)
        }
    }

    if (ui.showDeleteConfirm) {
        ActionConfirmationBottomSheet(
            title = stringResource(Res.string.events_delete_title),
            description = stringResource(Res.string.events_delete_description, event?.id.orEmpty()),
            confirmLabel = stringResource(Res.string.events_action_delete_confirm),
            dismissLabel = stringResource(Res.string.action_cancel),
            icon = Icons.Filled.Delete,
            tone = ActionConfirmationToneType.Danger,
            onConfirm = { viewModel.onAction(EventDetailAction.ConfirmDelete) },
            onDismiss = { viewModel.onAction(EventDetailAction.DismissDelete) },
        )
    }

    if (ui.showEditSheet) {
        EventEditSheet(ui, viewModel)
    }

    if (ui.showLinkSheet) {
        LinkExpensesSheet(ui, viewModel)
    }
}

@Composable
private fun EventDetailContent(
    event: EventRecord,
    viewModel: EventDetailViewModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SummarySection(event)
        ApprovalActions(event, viewModel)
        Button(
            onClick = { viewModel.onAction(EventDetailAction.LogExpense) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(Res.string.events_action_create_expense), modifier = Modifier.padding(start = 8.dp))
        }
        LinkedExpensesSection(event, viewModel)
    }
}

@Composable
private fun SummarySection(event: EventRecord) {
    SectionCard(title = stringResource(Res.string.events_detail_section_summary), leadingIcon = Icons.Filled.Event) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(event.category.label, style = MaterialTheme.typography.bodyMedium)
            StatusChip(label = event.status.localizedLabel(), tone = toneFor(event.status))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // P29.E.2: capacity vs actual-attendance + variance chip.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Capacity ${event.capacity} · Actual ${event.actualAttendance}", style = MaterialTheme.typography.bodyMedium)
            AttendanceVarianceChip(event.capacity, event.actualAttendance)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // P29.E.3: budget vs actual + per-head auto-calc.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(Res.string.events_detail_budgeted_amount, CommonUtils.formatCurrencyAmount(event.budgetedAmountMinor / 100.0)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                BudgetVarianceChip(event.budgetedAmountMinor, event.actualAmountMinor)
            }
            Text(
                stringResource(Res.string.events_detail_actual_amount, CommonUtils.formatCurrencyAmount(event.actualAmountMinor / 100.0)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val budgetPerHead = if (event.capacity > 0) event.budgetedAmountMinor / 100.0 / event.capacity else 0.0
            val actualPerHead = if (event.actualAttendance > 0) event.actualAmountMinor / 100.0 / event.actualAttendance else 0.0
            Text(
                stringResource(Res.string.events_detail_per_head, CommonUtils.formatCurrencyAmount(budgetPerHead)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.events_detail_per_head, CommonUtils.formatCurrencyAmount(actualPerHead)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AttendanceVarianceChip(
    capacity: Int,
    actual: Int,
) {
    if (capacity <= 0) return
    val pct = ((actual - capacity) * 100) / capacity
    val tone = if (actual > capacity || pct < -20) StatusTone.Warning else StatusTone.Success
    StatusChip(label = if (pct >= 0) "+$pct%" else "$pct%", tone = tone)
}

@Composable
private fun BudgetVarianceChip(
    budgetedMinor: Long,
    actualMinor: Long,
) {
    if (budgetedMinor <= 0) return
    val pct = ((actualMinor - budgetedMinor) * 100 / budgetedMinor).toInt()
    val tone = if (actualMinor > budgetedMinor) StatusTone.Danger else StatusTone.Success
    StatusChip(label = if (pct >= 0) "+$pct%" else "$pct%", tone = tone)
}

@Composable
private fun ApprovalActions(
    event: EventRecord,
    viewModel: EventDetailViewModel,
) {
    if (event.status != EventStatus.PENDING_APPROVAL) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { viewModel.onAction(EventDetailAction.Approve) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Text(stringResource(Res.string.events_action_approve), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = { viewModel.onAction(EventDetailAction.Reject) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Close, contentDescription = null)
            Text(stringResource(Res.string.events_action_reject), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun LinkedExpensesSection(
    event: EventRecord,
    viewModel: EventDetailViewModel,
) {
    SectionCard(
        title = stringResource(Res.string.events_detail_section_linked_expenses),
        leadingIcon = Icons.Filled.Link,
        trailingAction = {
            IconButton(onClick = { viewModel.onAction(EventDetailAction.OpenLinkSheet) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.events_action_link_expenses))
            }
        },
    ) {
        if (event.linkedExpenses.isEmpty()) {
            Text(
                stringResource(Res.string.events_no_linked_expenses),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                event.linkedExpenses.forEach { expense ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(expense.description, style = MaterialTheme.typography.bodyMedium)
                        Text(CommonUtils.formatCurrencyAmount(expense.amountMinor / 100.0), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditSheet(
    ui: EventDetailUiState,
    viewModel: EventDetailViewModel,
) {
    var showCategorySheet by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { viewModel.onAction(EventDetailAction.DismissEdit) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.events_edit_sheet_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = ui.editCategory.label,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.events_field_category)) },
                trailingIcon = {
                    IconButton(onClick = { showCategorySheet = true }) {
                        Icon(Icons.Filled.Category, contentDescription = null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.editBudgetText,
                onValueChange = { viewModel.onAction(EventDetailAction.SetEditBudgetText(it)) },
                label = { Text(stringResource(Res.string.events_edit_budget_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { viewModel.onAction(EventDetailAction.SaveEdit) }) {
                    Text(stringResource(Res.string.action_save))
                }
            }
        }
    }

    if (showCategorySheet) {
        SortBottomSheet(
            title = stringResource(Res.string.events_field_category),
            options = EventCategory.entries.map { SortOption(it, it.label, Icons.Filled.Category) },
            selected = ui.editCategory,
            onSelect = {
                viewModel.onAction(EventDetailAction.SetEditCategory(it))
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkExpensesSheet(
    ui: EventDetailUiState,
    viewModel: EventDetailViewModel,
) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.onAction(EventDetailAction.DismissLinkSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(Res.string.events_link_sheet_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (ui.availableToLink.isEmpty()) {
                Text(
                    stringResource(Res.string.events_no_expenses_to_link),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    items(ui.availableToLink, key = { it.id }) { expense ->
                        LinkExpenseRow(expense, expense.id in ui.selectedToLink) {
                            viewModel.onAction(EventDetailAction.ToggleLinkSelection(expense.id))
                        }
                    }
                }
                Button(
                    onClick = { viewModel.onAction(EventDetailAction.ConfirmLink) },
                    enabled = ui.selectedToLink.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(Res.string.events_link_confirm, ui.selectedToLink.size))
                }
            }
        }
    }
}

@Composable
private fun LinkExpenseRow(
    expense: LinkedExpense,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, onValueChange = { onToggle() }).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Text(expense.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Text(CommonUtils.formatCurrencyAmount(expense.amountMinor / 100.0), style = MaterialTheme.typography.bodyMedium)
    }
}
