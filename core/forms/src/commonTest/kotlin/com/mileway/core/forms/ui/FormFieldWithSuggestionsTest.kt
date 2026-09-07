package com.mileway.core.forms.ui

import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.suggestions.FieldSuggestion
import com.mileway.core.forms.suggestions.SuggestionConfidence
import com.mileway.core.forms.suggestions.highConfidenceSuggestions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the four interactions `FormFieldWithSuggestions`' chips offer — accept, dismiss, undo,
 * accept-all-high-confidence — at the level of the pure logic that actually decides each one:
 * [SuggestionDismissalState] for dismiss/undo, and [highConfidenceSuggestions] (already exercised
 * on its own selection logic by `FieldSuggestionsTest`) for what accept-all applies. "Accept" itself
 * is a direct `onValueChange(fieldKey, value)` call with no state of its own — nothing to unit-test
 * beyond the value each fixture carries, asserted inline below.
 *
 * No Compose test harness is used: no `core:*`/`feature:*` KMP library module in this repo has one
 * wired up yet (only `:app`, a classic Android application module, does — see
 * `WelcomeDisclaimerSheetTest`), and standing one up from scratch for this module's `androidHostTest`
 * target is a real, unverified Gradle-wiring risk this lane didn't take on. See
 * [SuggestionDismissalState]'s KDoc for why extracting the state this way makes that an honest
 * trade rather than a coverage gap.
 */
class FormFieldWithSuggestionsTest {
    private fun suggestion(
        fieldKey: String,
        confidence: Float,
        value: String = "value-$fieldKey",
    ) = FieldSuggestion(
        fieldKey = fieldKey,
        docField = DocField.MERCHANT,
        label = fieldKey,
        value = FormFieldValue.Text(value),
        displayValue = value,
        confidence = confidence,
        tier = if (confidence >= 0.85f) SuggestionConfidence.HIGH else SuggestionConfidence.MEDIUM,
        source = AnalyzerSource.ON_DEVICE_AI,
    )

    // ---- dismiss ----

    @Test
    fun dismiss_addsTheFieldKeyAndRemembersItAsTheUndoTarget() {
        val merchant = suggestion("merchant", 0.9f)

        val state = SuggestionDismissalState().dismiss(merchant)

        assertTrue("merchant" in state.dismissedKeys)
        assertEquals(merchant, state.undoableSuggestion)
    }

    @Test
    fun dismissingASecondField_bothStayDismissed_undoOffersOnlyTheLatest() {
        val merchant = suggestion("merchant", 0.9f)
        val total = suggestion("total", 0.9f)

        val state = SuggestionDismissalState().dismiss(merchant).dismiss(total)

        assertTrue("merchant" in state.dismissedKeys)
        assertTrue("total" in state.dismissedKeys)
        assertEquals(total, state.undoableSuggestion)
    }

    // ---- undo ----

    @Test
    fun undo_restoresTheDismissedField() {
        val merchant = suggestion("merchant", 0.9f)
        val dismissed = SuggestionDismissalState().dismiss(merchant)

        val restored = dismissed.undo()

        assertTrue("merchant" !in restored.dismissedKeys)
        assertNull(restored.undoableSuggestion)
    }

    @Test
    fun undo_withNothingDismissed_isANoOp() {
        val state = SuggestionDismissalState()

        assertEquals(state, state.undo())
    }

    @Test
    fun undo_afterASecondDismiss_leavesTheFirstFieldStillDismissed() {
        val merchant = suggestion("merchant", 0.9f)
        val total = suggestion("total", 0.9f)
        val state = SuggestionDismissalState().dismiss(merchant).dismiss(total)

        val afterUndo = state.undo()

        // Only `total` (the latest dismiss) is undoable — `merchant` was never offered an undo row
        // once `total` was dismissed over it, so it stays dismissed rather than silently reappearing.
        assertTrue("merchant" in afterUndo.dismissedKeys)
        assertTrue("total" !in afterUndo.dismissedKeys)
        assertNull(afterUndo.undoableSuggestion)
    }

    // ---- accept-all-high-confidence ----

    @Test
    fun acceptAllHighConfidence_appliesOnlyTheHighTierSuggestions() {
        val merchant = suggestion("merchant", 0.95f)
        val total = suggestion("total", 0.9f)
        val note = suggestion("note", 0.65f) // MEDIUM — accept-all must leave this one alone.

        val applied = mutableListOf<String>()
        highConfidenceSuggestions(listOf(merchant, total, note)).forEach { applied += it.fieldKey }

        assertEquals(listOf("merchant", "total"), applied)
    }

    // ---- accept ----

    @Test
    fun accept_isTheSuggestionsOwnValueUnchanged() {
        val merchant = suggestion("merchant", 0.9f, value = "Chai Stall")

        assertEquals(FormFieldValue.Text("Chai Stall"), merchant.value)
    }
}
