package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSuccessScreen(
    onAddAnother: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val requiresApproval = ui.lastSubmittedAmount > 5000.0

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = DesignTokens.Spacing.xl)
                    .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp),
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl))

            Text(
                text = "Expense Submitted!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            Text(
                text = "₹${ui.lastSubmittedAmount.formatDecimal(2)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Expense ID: ${ui.lastSubmittedId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (requiresApproval) {
                Spacer(Modifier.height(DesignTokens.Spacing.l))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = DesignTokens.Shape.roundedMd,
                ) {
                    Column(
                        modifier = Modifier.padding(DesignTokens.Spacing.l),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "Approval Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "This expense exceeds ₹5,000 and has been sent to your manager for approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xxl))

            Button(
                onClick = {
                    viewModel.onAction(ExpenseAction.ResetForm)
                    onAddAnother()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add Another Expense")
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View Expense History")
            }
        }
    }
}
