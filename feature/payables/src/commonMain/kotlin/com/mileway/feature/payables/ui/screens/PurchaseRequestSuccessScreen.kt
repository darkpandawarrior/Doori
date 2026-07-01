package com.mileway.feature.payables.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.payables.viewmodel.PayablesAction
import com.mileway.feature.payables.viewmodel.PayablesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PurchaseRequestSuccessScreen(
    onCreateAnother: () -> Unit,
    onBackToPayables: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayablesViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
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
                text = "Purchase Request Submitted!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = "PO Number: ${ui.lastSubmittedId}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = "Your request has been sent to the procurement team for review. You'll be notified once it's approved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.xxl))
            Button(
                onClick = {
                    viewModel.onAction(PayablesAction.ResetForm)
                    onCreateAnother()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create Another Request")
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            OutlinedButton(
                onClick = onBackToPayables,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Back to Payables")
            }
        }
    }
}
