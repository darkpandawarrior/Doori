package com.mileway.core.ui.components.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.common.UiText
import com.mileway.core.common.formatDecimal
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.expense_context_advance_title
import com.mileway.core.ui.resources.expense_context_card_title
import com.mileway.core.ui.resources.expense_context_edit_title
import com.mileway.core.ui.resources.expense_context_event_title
import com.mileway.core.ui.resources.expense_context_label_advance
import com.mileway.core.ui.resources.expense_context_label_advance_id
import com.mileway.core.ui.resources.expense_context_label_amount
import com.mileway.core.ui.resources.expense_context_label_amount_limit
import com.mileway.core.ui.resources.expense_context_label_attachment
import com.mileway.core.ui.resources.expense_context_label_card_id
import com.mileway.core.ui.resources.expense_context_label_confidence
import com.mileway.core.ui.resources.expense_context_label_event
import com.mileway.core.ui.resources.expense_context_label_event_id
import com.mileway.core.ui.resources.expense_context_label_expense_id
import com.mileway.core.ui.resources.expense_context_label_merchant
import com.mileway.core.ui.resources.expense_context_label_transaction_id
import com.mileway.core.ui.resources.expense_context_label_trip
import com.mileway.core.ui.resources.expense_context_label_trip_id
import com.mileway.core.ui.resources.expense_context_message_title
import com.mileway.core.ui.resources.expense_context_scanner_title
import com.mileway.core.ui.resources.expense_context_trip_advance_title
import com.mileway.core.ui.resources.expense_context_trip_title
import com.mileway.core.ui.text.text
import com.mileway.core.ui.theme.DesignTokens

/**
 * P25.A5.2: does this [ExpenseSourceContext] have anything worth summarizing? `None` (no context
 * at all) and `Regular` (an explicit blank "Add Expense") both mean "no linkage" — pulled out as a
 * standalone pure function so the gate is unit-testable without composing anything.
 */
fun ExpenseSourceContext.hasSummary(): Boolean = this !is ExpenseSourceContext.None && this !is ExpenseSourceContext.Regular

/**
 * P25.A5.2/P27.E.2: Mileway's own skin (DiCE's `ExpenseContextCard` is a structural blueprint
 * only, never copied visually — see project CLAUDE.md). Renders nothing for
 * [ExpenseSourceContext.None]/[ExpenseSourceContext.Regular] (see [hasSummary]); every other
 * variant renders a title ([summaryTitle]) plus its per-variant label/value rows ([summaryRows]).
 * Pure presentation of a context (+ whatever small display fields the context itself carries, see
 * [ExpenseSourceContext]'s kdoc) — no repository/ViewModel access, so this stays feature-agnostic
 * in `core:ui`. Not yet rendered on the expense entry screen itself — that wiring is E-STRUCT
 * (P27.E.1), still deferred.
 */
@Composable
fun ExpenseContextSummaryCard(
    context: ExpenseSourceContext,
    modifier: Modifier = Modifier,
) {
    if (!context.hasSummary()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(
                text = context.summaryTitle().text(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            context.summaryRows().forEach { (label, value) -> ContextSummaryRow(label.text(), value) }
        }
    }
}

@Composable
private fun ContextSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** Per-variant title — the one piece of content the P25 shell locked in; P27.E.2 fills [summaryRows]. */
internal fun ExpenseSourceContext.summaryTitle(): UiText =
    when (this) {
        is ExpenseSourceContext.None, is ExpenseSourceContext.Regular ->
            UiText.Empty // unreachable — gated by hasSummary() before this is ever called
        is ExpenseSourceContext.Edit -> UiText.Res(Res.string.expense_context_edit_title.key)
        is ExpenseSourceContext.Trip -> UiText.Res(Res.string.expense_context_trip_title.key)
        is ExpenseSourceContext.TripAdvance -> UiText.Res(Res.string.expense_context_trip_advance_title.key)
        is ExpenseSourceContext.Event -> UiText.Res(Res.string.expense_context_event_title.key)
        is ExpenseSourceContext.Card -> UiText.Res(Res.string.expense_context_card_title.key)
        is ExpenseSourceContext.Advance -> UiText.Res(Res.string.expense_context_advance_title.key)
        is ExpenseSourceContext.Message -> UiText.Res(Res.string.expense_context_message_title.key)
        is ExpenseSourceContext.Scanner -> UiText.Res(Res.string.expense_context_scanner_title.key)
    }

private fun label(key: String): UiText = UiText.Res(key)

/**
 * P27.E.2: label/value rows for [ExpenseContextSummaryCard]'s body — a plain function (no
 * [androidx.compose.runtime.Composable]) so per-variant content is unit-testable without composing
 * anything, mirroring [summaryTitle]'s precedent. Only renders the display fields the context
 * itself carries (see [ExpenseSourceContext] kdoc on why no repository lookup happens here) — a
 * caller-supplied label falls back to just the raw id when null.
 */
internal fun ExpenseSourceContext.summaryRows(): List<Pair<UiText, String>> =
    when (this) {
        is ExpenseSourceContext.None, is ExpenseSourceContext.Regular -> emptyList() // unreachable — gated by hasSummary()
        is ExpenseSourceContext.Edit ->
            listOf(label(Res.string.expense_context_label_expense_id.key) to expenseId)
        is ExpenseSourceContext.Trip ->
            listOfNotNull(
                tripLabel?.let { label(Res.string.expense_context_label_trip.key) to it },
                label(Res.string.expense_context_label_trip_id.key) to tripId,
            )
        is ExpenseSourceContext.TripAdvance ->
            listOfNotNull(
                tripLabel?.let { label(Res.string.expense_context_label_trip.key) to it },
                label(Res.string.expense_context_label_trip_id.key) to tripId,
                label(Res.string.expense_context_label_advance_id.key) to advanceId,
            )
        is ExpenseSourceContext.Event ->
            listOfNotNull(
                eventLabel?.let { label(Res.string.expense_context_label_event.key) to it },
                label(Res.string.expense_context_label_event_id.key) to eventId,
            )
        is ExpenseSourceContext.Card ->
            listOfNotNull(
                merchantName?.let { label(Res.string.expense_context_label_merchant.key) to it },
                transactionAmountRupees?.let { label(Res.string.expense_context_label_amount_limit.key) to "₹${it.formatDecimal(2)}" },
                label(Res.string.expense_context_label_card_id.key) to cardId,
                label(Res.string.expense_context_label_transaction_id.key) to transactionId,
            )
        is ExpenseSourceContext.Advance ->
            listOfNotNull(
                advanceLabel?.let { label(Res.string.expense_context_label_advance.key) to it },
                label(Res.string.expense_context_label_advance_id.key) to advanceId,
            )
        is ExpenseSourceContext.Message ->
            listOf(label(Res.string.expense_context_label_attachment.key) to attachmentUrl)
        is ExpenseSourceContext.Scanner ->
            listOfNotNull(
                prefill.merchant?.let { label(Res.string.expense_context_label_merchant.key) to it },
                prefill.amountText?.let { amount -> label(Res.string.expense_context_label_amount.key) to "₹$amount" },
                label(Res.string.expense_context_label_confidence.key) to "${(prefill.overallConfidence * 100).toInt()}%",
            )
    }
