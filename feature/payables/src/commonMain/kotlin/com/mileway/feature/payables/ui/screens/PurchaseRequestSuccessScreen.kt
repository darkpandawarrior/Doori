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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.payables_success_back
import com.mileway.core.ui.resources.payables_success_create_another
import com.mileway.core.ui.resources.payables_success_message
import com.mileway.core.ui.resources.payables_success_po_number
import com.mileway.core.ui.resources.payables_success_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.payables.viewmodel.PayablesAction
import com.mileway.feature.payables.viewmodel.PayablesViewModel
import org.jetbrains.compose.resources.stringResource
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
                text = stringResource(Res.string.payables_success_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = stringResource(Res.string.payables_success_po_number, ui.lastSubmittedId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = stringResource(Res.string.payables_success_message),
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
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(stringResource(Res.string.payables_success_create_another))
            }
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            OutlinedButton(
                onClick = onBackToPayables,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(stringResource(Res.string.payables_success_back))
            }
        }
    }
}
