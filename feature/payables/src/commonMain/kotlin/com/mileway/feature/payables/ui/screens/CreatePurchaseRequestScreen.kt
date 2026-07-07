@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.payables.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.common.formatDecimal
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.payables_back
import com.mileway.core.ui.resources.payables_decrease
import com.mileway.core.ui.resources.payables_increase
import com.mileway.core.ui.resources.payables_line_items
import com.mileway.core.ui.resources.payables_pr_add_item
import com.mileway.core.ui.resources.payables_pr_continue
import com.mileway.core.ui.resources.payables_pr_field_delivery_date
import com.mileway.core.ui.resources.payables_pr_field_delivery_date_hint
import com.mileway.core.ui.resources.payables_pr_field_description
import com.mileway.core.ui.resources.payables_pr_field_location
import com.mileway.core.ui.resources.payables_pr_field_unit_price
import com.mileway.core.ui.resources.payables_pr_field_vendor
import com.mileway.core.ui.resources.payables_pr_field_vendor_hint
import com.mileway.core.ui.resources.payables_pr_item_number
import com.mileway.core.ui.resources.payables_pr_section_request_details
import com.mileway.core.ui.resources.payables_pr_step
import com.mileway.core.ui.resources.payables_pr_submit
import com.mileway.core.ui.resources.payables_pr_title
import com.mileway.core.ui.resources.payables_remove
import com.mileway.core.ui.resources.payables_total_incl_gst
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.payables.model.NewLineItemDraft
import com.mileway.feature.payables.viewmodel.PayablesAction
import com.mileway.feature.payables.viewmodel.PayablesEffect
import com.mileway.feature.payables.viewmodel.PayablesViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseRequestScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayablesViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val form = ui.form

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PayablesEffect.NavigateToSuccess -> onSubmitted()
                PayablesEffect.NavigateBack -> onBack()
                is PayablesEffect.ShowToast -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.payables_pr_title),
                subtitle = stringResource(Res.string.payables_pr_step, form.step),
                depth = NavigationDepth.LEVEL_1,
                titleIcon = Icons.AutoMirrored.Filled.Assignment,
                navigationIcon = {
                    IconButton(onClick = {
                        if (form.step == 1) onBack() else viewModel.onAction(PayablesAction.GoToStep1)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payables_back))
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
                    if (form.step == 1) {
                        Button(
                            onClick = { viewModel.onAction(PayablesAction.GoToStep2) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = form.vendorName.isNotBlank() && form.deliveryDate.isNotBlank(),
                            shape = DesignTokens.Shape.button,
                        ) {
                            Text(stringResource(Res.string.payables_pr_continue))
                            Spacer(Modifier.size(DesignTokens.Spacing.s))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onAction(PayablesAction.SubmitPo) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled =
                                form.lineItems.isNotEmpty() &&
                                    form.lineItems.all {
                                        it.description.isNotBlank() && it.unitPrice.isNotBlank()
                                    },
                            shape = DesignTokens.Shape.button,
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(DesignTokens.Spacing.s))
                            Text(stringResource(Res.string.payables_pr_submit))
                        }
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

            if (form.step == 1) {
                Step1Fields(form = form, viewModel = viewModel)
            } else {
                Step2LineItems(form = form, viewModel = viewModel)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.xxl))
        }
    }
}

@Composable
private fun Step1Fields(
    form: com.mileway.feature.payables.viewmodel.CreatePoFormState,
    viewModel: PayablesViewModel,
) {
    Text(
        text = stringResource(Res.string.payables_pr_section_request_details),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    OutlinedTextField(
        value = form.vendorName,
        onValueChange = { viewModel.onAction(PayablesAction.SetVendorName(it)) },
        label = { Text(stringResource(Res.string.payables_pr_field_vendor)) },
        placeholder = { Text(stringResource(Res.string.payables_pr_field_vendor_hint)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OutlinedTextField(
        value = form.deliveryDate,
        onValueChange = { viewModel.onAction(PayablesAction.SetDeliveryDate(it)) },
        label = { Text(stringResource(Res.string.payables_pr_field_delivery_date)) },
        placeholder = { Text(stringResource(Res.string.payables_pr_field_delivery_date_hint)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    OfficeLocationDropdown(
        selected = form.officeLocation,
        options = viewModel.officeLocations,
        onSelect = { viewModel.onAction(PayablesAction.SetOfficeLocation(it)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficeLocationDropdown(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.payables_pr_field_location)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun Step2LineItems(
    form: com.mileway.feature.payables.viewmodel.CreatePoFormState,
    viewModel: PayablesViewModel,
) {
    Text(
        text = stringResource(Res.string.payables_line_items),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )

    form.lineItems.forEachIndexed { index, item ->
        LineItemEditor(
            item = item,
            index = index,
            canRemove = form.lineItems.size > 1,
            onUpdate = { viewModel.onAction(PayablesAction.UpdateLineItem(index, it)) },
            onRemove = { viewModel.onAction(PayablesAction.RemoveLineItem(index)) },
        )
        if (index < form.lineItems.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.xs))
        }
    }

    OutlinedButton(
        onClick = { viewModel.onAction(PayablesAction.AddLineItem) },
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.button,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(DesignTokens.Spacing.s))
        Text(stringResource(Res.string.payables_pr_add_item))
    }

    // Running total
    val total =
        form.lineItems.sumOf { item ->
            val unitPrice = item.unitPrice.toDoubleOrNull() ?: 0.0
            item.qty * unitPrice * (1 + item.gstPercent / 100.0)
        }
    if (total > 0) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = DesignTokens.Shape.roundedSm,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.l),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.payables_total_incl_gst), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "₹${total.formatDecimal(2)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LineItemEditor(
    item: NewLineItemDraft,
    index: Int,
    canRemove: Boolean,
    onUpdate: (NewLineItemDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(
                text = stringResource(Res.string.payables_pr_item_number, index + 1),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(Res.string.payables_remove),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        OutlinedTextField(
            value = item.description,
            onValueChange = { onUpdate(item.copy(description = it)) },
            label = { Text(stringResource(Res.string.payables_pr_field_description)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Qty stepper
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = DesignTokens.Shape.roundedSm,
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    IconButton(
                        onClick = { if (item.qty > 1) onUpdate(item.copy(qty = item.qty - 1)) },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Filled.Remove, contentDescription = stringResource(Res.string.payables_decrease), modifier = Modifier.size(16.dp)) }
                    Text("${item.qty}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { onUpdate(item.copy(qty = item.qty + 1)) },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.payables_increase), modifier = Modifier.size(16.dp)) }
                }
            }

            OutlinedTextField(
                value = item.unitPrice,
                onValueChange = { onUpdate(item.copy(unitPrice = it)) },
                label = { Text(stringResource(Res.string.payables_pr_field_unit_price)) },
                prefix = { Text("₹") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(2f),
            )
        }
    }
}
