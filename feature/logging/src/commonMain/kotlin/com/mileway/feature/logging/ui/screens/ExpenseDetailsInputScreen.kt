package com.mileway.feature.logging.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseEffect
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
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
                title = form.category?.label ?: "Expense Details",
                subtitle = "Fill in the expense details",
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.l),
                ) {
                    Button(
                        onClick = { viewModel.onAction(ExpenseAction.SubmitExpense) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = form.amountText.isNotBlank() && form.merchantName.isNotBlank(),
                    ) {
                        Text("Submit Expense")
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

            OutlinedTextField(
                value = form.amountText,
                onValueChange = { viewModel.onAction(ExpenseAction.SetAmount(it)) },
                label = { Text("Amount (₹)") },
                placeholder = { Text("0.00") },
                prefix = { Text("₹") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.merchantName,
                onValueChange = { viewModel.onAction(ExpenseAction.SetMerchant(it)) },
                label = { Text("Merchant / Vendor Name") },
                placeholder = { Text("e.g. Swiggy, Ola Cabs") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.note,
                onValueChange = { viewModel.onAction(ExpenseAction.SetNote(it)) },
                label = { Text("Note (optional)") },
                placeholder = { Text("Purpose of this expense…") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            if ((form.amountText.toDoubleOrNull() ?: 0.0) > 5000.0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = DesignTokens.Shape.roundedSm,
                ) {
                    Text(
                        text = "⚠ Expenses above ₹5,000 require manager approval before reimbursement.",
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
