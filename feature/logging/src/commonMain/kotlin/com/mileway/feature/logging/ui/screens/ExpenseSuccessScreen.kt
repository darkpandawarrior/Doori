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
import com.mileway.core.network.model.SubmissionStatus
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_add_another_expense
import com.mileway.core.ui.resources.logging_approval_required
import com.mileway.core.ui.resources.logging_expense_id_value
import com.mileway.core.ui.resources.logging_expense_requires_approval
import com.mileway.core.ui.resources.logging_expense_submitted_title
import com.mileway.core.ui.resources.logging_view_expense_history
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import com.siddharth.kmp.common.formatDecimal
import org.jetbrains.compose.resources.stringResource
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
    // P1.6: same tiered policy engine as Log Miles — approval banner shows for any violation
    // status (POLICY_VIOLATION, NEEDS_APPROVAL, HARD_STOP), not just a raw amount check.
    val requiresApproval =
        ui.lastSubmissionStatus == SubmissionStatus.POLICY_VIOLATION ||
            ui.lastSubmissionStatus == SubmissionStatus.NEEDS_APPROVAL ||
            ui.lastSubmissionStatus == SubmissionStatus.HARD_STOP

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
                text = stringResource(Res.string.logging_expense_submitted_title),
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
                text = stringResource(Res.string.logging_expense_id_value, ui.lastSubmittedId),
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
                            text = stringResource(Res.string.logging_approval_required),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text =
                                ui.lastSubmissionViolations.firstOrNull()?.message
                                    ?: stringResource(Res.string.logging_expense_requires_approval),
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
                shape = DesignTokens.Shape.button,
            ) {
                Text(stringResource(Res.string.logging_add_another_expense))
            }

            Spacer(Modifier.height(DesignTokens.Spacing.s))

            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) {
                Text(stringResource(Res.string.logging_view_expense_history))
            }
        }
    }
}
