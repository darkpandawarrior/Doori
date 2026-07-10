package com.mileway.core.ui.components.expense

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.common.UiText
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.expense_context_advance_title
import com.mileway.core.ui.resources.expense_context_card_title
import com.mileway.core.ui.resources.expense_context_edit_title
import com.mileway.core.ui.resources.expense_context_event_title
import com.mileway.core.ui.resources.expense_context_message_title
import com.mileway.core.ui.resources.expense_context_scanner_title
import com.mileway.core.ui.resources.expense_context_todo_slot
import com.mileway.core.ui.resources.expense_context_trip_advance_title
import com.mileway.core.ui.resources.expense_context_trip_title
import com.mileway.core.ui.text.text
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/**
 * P25.A5.2: does this [ExpenseSourceContext] have anything worth summarizing? `None` (no context
 * at all) and `Regular` (an explicit blank "Add Expense") both mean "no linkage" — pulled out as a
 * standalone pure function so the gate is unit-testable without composing anything.
 */
fun ExpenseSourceContext.hasSummary(): Boolean = this !is ExpenseSourceContext.None && this !is ExpenseSourceContext.Regular

/**
 * P25.A5.2: Mileway's own skin (DiCE's `ExpenseContextCard` is a structural blueprint only, never
 * copied visually — see project CLAUDE.md). Renders nothing for [ExpenseSourceContext.None] /
 * [ExpenseSourceContext.Regular] (see [hasSummary]); every other variant gets a locked, titled
 * placeholder card — V27 P27.E.2 fills in the real per-variant summary content (trip name, advance
 * balance, card transaction merchant, etc.) behind this same gate.
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
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Text(
                text = context.summaryTitle().text(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.expense_context_todo_slot),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Per-variant title — the one piece of content this shell locks in; V27 fills the rest. */
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
