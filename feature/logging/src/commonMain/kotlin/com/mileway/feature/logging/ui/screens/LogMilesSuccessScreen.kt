package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_log_another
import com.mileway.core.ui.resources.logging_miles_logged_body
import com.mileway.core.ui.resources.logging_miles_logged_title
import com.mileway.core.ui.resources.logging_reimbursable_amount
import com.mileway.core.ui.resources.logging_transaction_id
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.logging.viewmodel.LogMilesAction
import com.mileway.feature.logging.viewmodel.LogMilesViewModel
import com.siddharth.kmp.common.formatDecimal
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Small success route shown after a successful Log Miles submission.
 *
 * Reads the reimbursable amount and transaction id from the last submission in
 * the shared ViewModel state and offers a single "Log Another" action that resets
 * the flow and returns to Step 1.
 *
 * Full-screen flow, so the pinned action carries [navigationBarsPadding].
 *
 * @param viewModel   shared flow ViewModel
 * @param onLogAnother reset the flow and pop back to Step 1
 */
@Composable
fun LogMilesSuccessScreen(
    viewModel: LogMilesViewModel = koinViewModel(),
    onLogAnother: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val result = uiState.submissionResult

    val amount = result?.reimbursableAmount ?: result?.amount ?: uiState.reimbursableAmount
    val transactionId =
        result?.transId
            ?: result?.transaction?.id?.takeIf { it.isNotBlank() }
            ?: result?.voucher?.transId

    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        viewModel.onAction(LogMilesAction.ResetSubmission)
                        onLogAnother()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(DesignTokens.Spacing.l)
                            .height(56.dp),
                    shape = DesignTokens.Shape.button,
                ) { Text(stringResource(Res.string.logging_log_another)) }
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = DesignTokens.Shape.button,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(DesignTokens.Spacing.l).size(72.dp),
                )
            }
            Spacer(Modifier.size(DesignTokens.Spacing.l))
            Text(
                stringResource(Res.string.logging_miles_logged_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Text(
                stringResource(Res.string.logging_miles_logged_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.size(DesignTokens.Spacing.xl))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                    SummaryRow(label = stringResource(Res.string.logging_reimbursable_amount), value = "₹${amount.formatDecimal(2)}")
                    if (!transactionId.isNullOrBlank()) {
                        Spacer(Modifier.size(DesignTokens.Spacing.m))
                        SummaryRow(label = stringResource(Res.string.logging_transaction_id), value = transactionId)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
