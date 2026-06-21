@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

package com.miletracker.feature.profile.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miletracker.core.common.formatDecimal
import com.miletracker.core.ui.components.topbar.DepthAwareTopBar
import com.miletracker.core.ui.theme.DesignTokens
import com.miletracker.core.ui.theme.DesignTokens.NavigationDepth
import com.miletracker.core.ui.theme.MilewayColors
import com.miletracker.core.ui.theme.dataStyle
import com.miletracker.feature.profile.viewmodel.AdvanceAction
import com.miletracker.feature.profile.viewmodel.AdvanceViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAdvanceFormScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdvanceViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val form = ui.form

    if (ui.submitted) {
        AdvanceSuccessContent(
            id = ui.lastSubmittedId,
            amount = form.amountText.toDoubleOrNull() ?: 0.0,
            autoApproved = ui.lastAutoApproved,
            onDone = {
                viewModel.onAction(AdvanceAction.ResetForm)
                onBack()
            },
        )
        return
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = "Request Advance",
                subtitle = "Step ${form.step} of 3",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = {
                        when (form.step) {
                            1 -> onBack()
                            else -> viewModel.onAction(AdvanceAction.GoToStep(form.step - 1))
                        }
                    }) {
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
                    val enabled =
                        when (form.step) {
                            1 -> form.amountText.isNotBlank() && form.purpose.isNotBlank() && form.requiredByDate.isNotBlank()
                            2 -> true
                            3 -> form.declarationChecked
                            else -> false
                        }
                    Button(
                        onClick = {
                            if (form.step < 3) {
                                viewModel.onAction(AdvanceAction.GoToStep(form.step + 1))
                            } else {
                                viewModel.onAction(AdvanceAction.SubmitAdvance)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    ) {
                        Text(if (form.step == 3) "Submit Advance Request" else "Continue")
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
                    .imePadding()
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.s))

            when (form.step) {
                1 -> {
                    Text("Amount & Purpose", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = form.amountText,
                        onValueChange = { viewModel.onAction(AdvanceAction.SetAmount(it)) },
                        label = { Text("Amount (₹)") },
                        prefix = { Text("₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val amount = form.amountText.toDoubleOrNull() ?: 0.0
                    if (amount >= 10_000.0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = DesignTokens.Shape.roundedSm,
                        ) {
                            Text(
                                "Advances ≥ ₹10,000 require manager approval (1–2 business days).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(DesignTokens.Spacing.m),
                            )
                        }
                    }

                    OutlinedTextField(
                        value = form.purpose,
                        onValueChange = { viewModel.onAction(AdvanceAction.SetPurpose(it)) },
                        label = { Text("Purpose") },
                        placeholder = { Text("Brief description of use…") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = form.requiredByDate,
                        onValueChange = { viewModel.onAction(AdvanceAction.SetRequiredByDate(it)) },
                        label = { Text("Required By Date") },
                        placeholder = { Text("e.g. 2024-02-15") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                2 -> {
                    Text("Supporting Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = DesignTokens.Shape.roundedMd,
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(DesignTokens.Spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                "No documents required for amounts below ₹25,000 (illustrative)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                3 -> {
                    Text("Declaration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = DesignTokens.Shape.roundedMd,
                    ) {
                        Text(
                            "I hereby declare that the advance requested is for official purposes only, and I will submit proper receipts and expense claims within 7 working days of the expenditure.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(DesignTokens.Spacing.l),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        Checkbox(
                            checked = form.declarationChecked,
                            onCheckedChange = { viewModel.onAction(AdvanceAction.SetDeclaration(it)) },
                        )
                        Text("I agree to the above declaration", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xxl))
        }
    }
}

@Composable
private fun AdvanceSuccessContent(
    id: String,
    amount: Double,
    autoApproved: Boolean,
    onDone: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (autoApproved) Icons.Filled.CheckCircle else Icons.Filled.HourglassBottom,
            contentDescription = null,
            tint = if (autoApproved) MilewayColors.success else MilewayColors.warning,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (autoApproved) "Advance Approved!" else "Request Submitted",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "₹${amount.formatDecimal(2)}",
            style = MaterialTheme.typography.displaySmall.dataStyle(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Request ID: $id",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text =
                if (autoApproved) {
                    "Amount < ₹10,000 — auto-approved. Disbursement within 24 hours."
                } else {
                    "Amount ≥ ₹10,000 — under manager review. Expected decision in 1–2 business days."
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Back to My Advances")
        }
    }
}
