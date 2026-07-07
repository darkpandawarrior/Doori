@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

package com.mileway.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.profile_advance_amount_label
import com.mileway.core.ui.resources.profile_advance_amount_purpose
import com.mileway.core.ui.resources.profile_advance_approved_title
import com.mileway.core.ui.resources.profile_advance_auto_approved_note
import com.mileway.core.ui.resources.profile_advance_back
import com.mileway.core.ui.resources.profile_advance_back_to_advances
import com.mileway.core.ui.resources.profile_advance_continue
import com.mileway.core.ui.resources.profile_advance_declaration
import com.mileway.core.ui.resources.profile_advance_declaration_agree
import com.mileway.core.ui.resources.profile_advance_declaration_text
import com.mileway.core.ui.resources.profile_advance_manager_approval_note
import com.mileway.core.ui.resources.profile_advance_manager_review_note
import com.mileway.core.ui.resources.profile_advance_mode_card_desc
import com.mileway.core.ui.resources.profile_advance_mode_card_title
import com.mileway.core.ui.resources.profile_advance_mode_cash_desc
import com.mileway.core.ui.resources.profile_advance_mode_cash_title
import com.mileway.core.ui.resources.profile_advance_no_documents_note
import com.mileway.core.ui.resources.profile_advance_purpose_label
import com.mileway.core.ui.resources.profile_advance_purpose_placeholder
import com.mileway.core.ui.resources.profile_advance_receive_prompt
import com.mileway.core.ui.resources.profile_advance_request_advance
import com.mileway.core.ui.resources.profile_advance_request_id
import com.mileway.core.ui.resources.profile_advance_required_by_label
import com.mileway.core.ui.resources.profile_advance_required_by_placeholder
import com.mileway.core.ui.resources.profile_advance_select_linked_card
import com.mileway.core.ui.resources.profile_advance_step_of_4
import com.mileway.core.ui.resources.profile_advance_submit_request
import com.mileway.core.ui.resources.profile_advance_submitted_title
import com.mileway.core.ui.resources.profile_advance_supporting_documents
import com.mileway.core.ui.resources.profile_advance_type_optional
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.core.ui.theme.MilewayColors
import com.mileway.core.ui.theme.dataStyle
import com.mileway.feature.profile.model.AdvanceMode
import com.mileway.feature.profile.model.AdvanceType
import com.mileway.feature.profile.model.CorporateCard
import com.mileway.feature.profile.viewmodel.AdvanceAction
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                title = stringResource(Res.string.profile_advance_request_advance),
                subtitle = stringResource(Res.string.profile_advance_step_of_4, form.step + 1),
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = {
                        when (form.step) {
                            0 -> onBack()
                            else -> viewModel.onAction(AdvanceAction.GoToStep(form.step - 1))
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.profile_advance_back))
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
                            0 -> form.mode == AdvanceMode.CASH || form.selectedCardId != null
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
                        Text(
                            if (form.step == 3) {
                                stringResource(
                                    Res.string.profile_advance_submit_request,
                                )
                            } else {
                                stringResource(Res.string.profile_advance_continue)
                            },
                        )
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
                0 -> {
                    Text(
                        stringResource(Res.string.profile_advance_receive_prompt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    AdvanceModeOption(
                        icon = Icons.Filled.Payments,
                        title = stringResource(Res.string.profile_advance_mode_cash_title),
                        description = stringResource(Res.string.profile_advance_mode_cash_desc),
                        selected = form.mode == AdvanceMode.CASH,
                        onSelect = { viewModel.onAction(AdvanceAction.SetMode(AdvanceMode.CASH)) },
                    )

                    AdvanceModeOption(
                        icon = Icons.Filled.CreditCard,
                        title = stringResource(Res.string.profile_advance_mode_card_title),
                        description = stringResource(Res.string.profile_advance_mode_card_desc),
                        selected = form.mode == AdvanceMode.CARD_LINKED,
                        onSelect = { viewModel.onAction(AdvanceAction.SetMode(AdvanceMode.CARD_LINKED)) },
                    )

                    if (form.mode == AdvanceMode.CARD_LINKED) {
                        Text(
                            stringResource(Res.string.profile_advance_select_linked_card),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        ui.cards.forEach { card ->
                            AdvanceCardOption(
                                card = card,
                                selected = form.selectedCardId == card.id,
                                onSelect = { viewModel.onAction(AdvanceAction.SelectCard(card.id)) },
                            )
                        }
                    }
                }

                1 -> {
                    Text(
                        stringResource(Res.string.profile_advance_amount_purpose),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    OutlinedTextField(
                        value = form.amountText,
                        onValueChange = { viewModel.onAction(AdvanceAction.SetAmount(it)) },
                        label = { Text(stringResource(Res.string.profile_advance_amount_label)) },
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
                                stringResource(Res.string.profile_advance_manager_approval_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(DesignTokens.Spacing.m),
                            )
                        }
                    }

                    OutlinedTextField(
                        value = form.purpose,
                        onValueChange = { viewModel.onAction(AdvanceAction.SetPurpose(it)) },
                        label = { Text(stringResource(Res.string.profile_advance_purpose_label)) },
                        placeholder = { Text(stringResource(Res.string.profile_advance_purpose_placeholder)) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = form.requiredByDate,
                        onValueChange = { viewModel.onAction(AdvanceAction.SetRequiredByDate(it)) },
                        label = { Text(stringResource(Res.string.profile_advance_required_by_label)) },
                        placeholder = { Text(stringResource(Res.string.profile_advance_required_by_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        stringResource(Res.string.profile_advance_type_optional),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    ) {
                        AdvanceType.entries.forEach { type ->
                            FilterChip(
                                selected = form.type == type,
                                onClick = {
                                    viewModel.onAction(
                                        AdvanceAction.SetType(if (form.type == type) null else type),
                                    )
                                },
                                label = { Text(type.label()) },
                            )
                        }
                    }
                }

                2 -> {
                    Text(
                        stringResource(Res.string.profile_advance_supporting_documents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

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
                                stringResource(Res.string.profile_advance_no_documents_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                3 -> {
                    Text(stringResource(Res.string.profile_advance_declaration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = DesignTokens.Shape.roundedMd,
                    ) {
                        Text(
                            stringResource(Res.string.profile_advance_declaration_text),
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
                        Text(stringResource(Res.string.profile_advance_declaration_agree), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xxl))
        }
    }
}

@Composable
private fun AdvanceModeOption(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onSelect),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Icon(imageVector = icon, contentDescription = null)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AdvanceCardOption(
    card: CorporateCard,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onSelect),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${card.cardType.name.replaceFirstChar { it.uppercase() }} •••• ${card.lastFourDigits}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    card.holderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
            text = if (autoApproved) stringResource(Res.string.profile_advance_approved_title) else stringResource(Res.string.profile_advance_submitted_title),
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
            text = stringResource(Res.string.profile_advance_request_id, id),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text =
                if (autoApproved) {
                    stringResource(Res.string.profile_advance_auto_approved_note)
                } else {
                    stringResource(Res.string.profile_advance_manager_review_note)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.profile_advance_back_to_advances))
        }
    }
}
