package com.mileway.core.forms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.data.domain.claim.SplitTarget
import com.mileway.core.forms.ExpenseFieldContext
import com.mileway.core.forms.field.PercentageSplitInput
import com.mileway.core.forms.field.perHeadAmountMinor
import com.mileway.core.forms.field.percentageAllocations
import com.mileway.core.forms.itemization.ItemizedLineInput
import com.mileway.core.forms.itemization.hotelTemplate
import com.mileway.core.forms.itemization.reconciliationDelta
import com.siddharth.kmp.common.formatDecimal

/** Shared display uses the existing decimal formatter; calculation stays in Long minor units. */
private fun money(
    minor: Long,
    currency: String,
): String = "$currency ${(minor.toDouble() / 100).formatDecimal(2)}"

/** Controlled percentage editor, anchored to the host's card match when present. */
@Composable
fun PercentageSplitField(
    entries: List<PercentageSplitInput>,
    context: ExpenseFieldContext?,
    enabled: Boolean,
    onChange: (List<PercentageSplitInput>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        context?.let {
            val source = if (it.cardMatchedAmountMinor != null) "Card anchor" else "Expense anchor"
            Text("$source: ${money(it.anchorAmountMinor, it.currencyCode)}")
        }
        val allocations = context?.let { percentageAllocations(it.anchorAmountMinor, entries) }
        entries.forEachIndexed { index, entry ->
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SplitTarget.entries.forEach { target ->
                        FilterChip(
                            selected = entry.target == target,
                            enabled = enabled,
                            onClick = { onChange(entries.replaceAt(index, entry.copy(target = target))) },
                            label = { Text(target.label()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = entry.targetId,
                    onValueChange = { onChange(entries.replaceAt(index, entry.copy(targetId = it))) },
                    label = { Text("${entry.target.label()} name or ID") },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = entry.percentageText,
                    onValueChange = { onChange(entries.replaceAt(index, entry.copy(percentageText = it))) },
                    label = { Text("Percentage") },
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                allocations?.getOrNull(index)?.let { Text(money(it.amountMinor, context?.currencyCode.orEmpty())) }
                TextButton(enabled = enabled, onClick = { onChange(entries.filterIndexed { i, _ -> i != index }) }) {
                    Text("Remove split ${index + 1}")
                }
            }
        }
        TextButton(enabled = enabled && context != null, onClick = {
            onChange(entries + PercentageSplitInput(percentageText = if (entries.isEmpty()) "100" else ""))
        }) { Text("Add split") }
    }
}

/** Live named-head count and dated PolicyEngine warning; no policy is invented by the field. */
@Composable
fun AttendeeListField(
    names: List<String>,
    context: ExpenseFieldContext?,
    enabled: Boolean,
    onChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEachIndexed { index, name ->
            OutlinedTextField(
                value = name,
                onValueChange = { onChange(names.replaceAt(index, it)) },
                label = { Text("Attendee ${index + 1}") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(enabled = enabled, onClick = { onChange(names.filterIndexed { i, _ -> i != index }) }) {
                Text("Remove attendee ${index + 1}")
            }
        }
        val heads = names.count { it.isNotBlank() }
        context?.let {
            perHeadAmountMinor(it.anchorAmountMinor, heads)?.let { perHead -> Text("Per head: ${money(perHead, it.currencyCode)}") }
            val limit = it.policy?.versionFor(it.submittedAtMillis)?.perHeadLimitMinor
            if (limit != null) Text("Policy limit per head: ${money(limit, it.currencyCode)}")
            if (it.policy?.perHeadViolation(it.anchorAmountMinor, heads, it.submittedAtMillis) != null) {
                Text("Per-head expense exceeds the policy limit", color = MaterialTheme.colorScheme.error)
            }
        }
        TextButton(enabled = enabled, onClick = { onChange(names + "") }) { Text("Add attendee") }
    }
}

/** Receipt child-line host with an opt-in hotel label template and live reconciliation delta. */
@Composable
fun ItemizedLineHost(
    entries: List<ItemizedLineInput>,
    context: ExpenseFieldContext?,
    hotel: Boolean,
    enabled: Boolean,
    onChange: (List<ItemizedLineInput>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hotel && entries.isEmpty()) {
            TextButton(enabled = enabled, onClick = { onChange(hotelTemplate()) }) { Text("Suggest hotel lines: room, meals, tax") }
        }
        entries.forEachIndexed { index, entry ->
            OutlinedTextField(
                value = entry.description,
                onValueChange = { onChange(entries.replaceAt(index, entry.copy(description = it))) },
                label = { Text("Line ${index + 1} description") },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = entry.amountText,
                onValueChange = { onChange(entries.replaceAt(index, entry.copy(amountText = it))) },
                label = { Text("Line ${index + 1} amount (${context?.currencyCode.orEmpty()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = enabled,
            )
            TextButton(enabled = enabled, onClick = { onChange(entries.filterIndexed { i, _ -> i != index }) }) {
                Text("Remove item ${index + 1}")
            }
        }
        context?.let {
            Text("Receipt total: ${money(it.receiptAmountMinor, it.currencyCode)}")
            if (entries.isNotEmpty()) {
                val delta = reconciliationDelta(it.receiptAmountMinor, entries)
                Text(if (delta == null) "Complete all lines to reconcile" else "Difference from receipt: ${money(delta, it.currencyCode)}")
            }
        }
        TextButton(enabled = enabled && context != null, onClick = { onChange(entries + ItemizedLineInput()) }) { Text("Add itemized line") }
    }
}

private fun SplitTarget.label(): String =
    when (this) {
        SplitTarget.COST_CENTER -> "Cost centre"
        SplitTarget.PROJECT -> "Project"
        SplitTarget.PERSON -> "Person"
    }

private fun <T> List<T>.replaceAt(
    index: Int,
    value: T,
): List<T> = mapIndexed { i, old -> if (i == index) value else old }
