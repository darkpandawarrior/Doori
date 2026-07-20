package com.mileway.webpreview.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.terminalStyle
import com.mileway.webpreview.DemoExpenseStore
import com.mileway.webpreview.ExpenseStatus
import com.mileway.webpreview.formatInr

@Composable
fun ExpensesScreen(
    store: DemoExpenseStore,
    modifier: Modifier = Modifier,
) {
    val expenses by store.expenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val drafts = expenses.count { it.status == ExpenseStatus.DRAFT }

    Column(
        modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        Text(
            text = "EXPENSE LOG",
            style = MaterialTheme.typography.labelMedium.terminalStyle(),
            color = MaterialTheme.colorScheme.primary,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = DesignTokens.Shape.roundedMd,
            elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
        ) {
            Row(
                modifier = Modifier.padding(DesignTokens.Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL LOGGED",
                        style = MaterialTheme.typography.labelSmall.terminalStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatInr(expenses.sumOf { it.amountInr }),
                        style = MaterialTheme.typography.headlineSmall.terminalStyle(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${expenses.size} records",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "$drafts draft${if (drafts == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (drafts > 0) DesignTokens.StatusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            Button(
                onClick = { showAddDialog = true },
                shape = DesignTokens.Shape.button,
                modifier = Modifier.weight(1f),
            ) { Text("Add expense") }
            OutlinedButton(
                onClick = { store.submitDrafts() },
                shape = DesignTokens.Shape.button,
                enabled = drafts > 0,
                modifier = Modifier.weight(1f),
            ) { Text("Submit drafts") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            items(expenses, key = { it.id }) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedSm,
                    elevation = CardDefaults.cardElevation(defaultElevation = DesignTokens.Elevation.card),
                ) {
                    Row(
                        modifier = Modifier.padding(DesignTokens.Spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(expense.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${expense.category} · ${expense.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatInr(expense.amountInr),
                                style = MaterialTheme.typography.bodyMedium.terminalStyle(),
                            )
                            Spacer(Modifier.height(DesignTokens.Spacing.xs))
                            StatusChip(expense.status)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, category, amount ->
                store.add(title, category, amount)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun StatusChip(status: ExpenseStatus) {
    val (label, color) =
        when (status) {
            ExpenseStatus.DRAFT -> "DRAFT" to DesignTokens.StatusColors.neutral
            ExpenseStatus.SUBMITTED -> "SUBMITTED" to DesignTokens.StatusColors.warning
            ExpenseStatus.APPROVED -> "APPROVED" to DesignTokens.StatusColors.success
        }
    Surface(
        shape = DesignTokens.Shape.chip,
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.terminalStyle(),
            color = color,
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.s,
                    vertical = 2.dp,
                ),
        )
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, category: String, amount: Double) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fuel") }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title.trim(), category.trim(), amount ?: 0.0) },
                enabled = title.isNotBlank() && amount != null && amount > 0,
                shape = DesignTokens.Shape.button,
            ) { Text("Save draft") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
