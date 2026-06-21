package com.miletracker.feature.tracking.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
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
import com.miletracker.core.common.formatDecimal
import com.miletracker.core.data.model.display.TrackDisplayData
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.core.ui.theme.MilewayColors
import com.miletracker.core.ui.theme.dataStyle
import com.miletracker.feature.tracking.viewmodel.CreateVoucherAction
import com.miletracker.feature.tracking.viewmodel.CreateVoucherUiState
import com.miletracker.feature.tracking.viewmodel.CreateVoucherViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

private val CATEGORIES = listOf("Travel", "Fuel", "Maintenance", "Other")

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
                        0 -> "Select Expenses"
                        1 -> "Voucher Details"
                        2 -> "Confirmation"
                        else -> "Voucher Created"
                    },
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.step == 0 || uiState.step == 3) {
                            onBack()
                        } else {
                            viewModel.onAction(CreateVoucherAction.GoToStep(uiState.step - 1))
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.step == 0) {
                        TextButton(onClick = {
                            if (uiState.selectedTokens.size == uiState.expenses.size) {
                                viewModel.onAction(CreateVoucherAction.DeselectAll)
                            } else {
                                viewModel.onAction(CreateVoucherAction.SelectAll)
                            }
                        }) {
                            Text(
                                if (uiState.selectedTokens.size == uiState.expenses.size) {
                                    "Deselect All"
                                } else {
                                    "Select All"
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
                        "No submitted expenses found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Submit a journey first to include it in a voucher.",
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
                        "Total: ₹${viewModel.totalAmount.formatDecimal(2)}",
                        style = MaterialTheme.typography.titleMedium.dataStyle(),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = { viewModel.onAction(CreateVoucherAction.GoToStep(1)) },
                    enabled = uiState.selectedTokens.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Next — Voucher Details")
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
        headlineContent = { Text(expense.name ?: "Journey ${expense.token.take(6)}") },
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
            label = { Text("Voucher Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
        ) {
            OutlinedTextField(
                value = uiState.category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Expense Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
            ) {
                CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
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
                    Text("Total Amount", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "₹${viewModel.totalAmount.formatDecimal(2)}",
                        style = MaterialTheme.typography.headlineMedium.dataStyle(),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "${uiState.selectedTokens.size} expense${if (uiState.selectedTokens.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = { viewModel.onAction(CreateVoucherAction.SetNotes(it)) },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.onAction(CreateVoucherAction.GoToStep(2)) },
            enabled = uiState.title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Next — Review")
        }
    }
}

@Composable
private fun StepConfirmation(
    uiState: CreateVoucherUiState,
    viewModel: CreateVoucherViewModel,
) {
    var declared by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Review & Submit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow("Title", uiState.title)
                SummaryRow("Category", uiState.category)
                SummaryRow("Total", "₹${viewModel.totalAmount.formatDecimal(2)}")
                SummaryRow("Expenses", "${uiState.selectedTokens.size} selected")
                if (uiState.notes.isNotBlank()) SummaryRow("Notes", uiState.notes)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = declared, onCheckedChange = { declared = it })
            Text(
                "I confirm these expenses are valid and complete.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { viewModel.onAction(CreateVoucherAction.Submit) },
            enabled = declared && !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Create Voucher")
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
        Text("Voucher Created!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
            "Your voucher has been saved locally.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Track Detail")
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
