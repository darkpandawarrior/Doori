package com.mileway.core.ui.components.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalMaterial3Api::class)
class MilewayActionSheetTest {
    // --- dismiss-guard decision -------------------------------------------------------------

    @Test
    fun unguarded_sheet_never_vetoes_a_hidden_transition() {
        assertFalse(sheetShouldVetoDismiss(SheetValue.Hidden, guardDismissal = false))
    }

    @Test
    fun guarded_sheet_vetoes_the_hidden_transition() {
        // This is the rule the taxonomy names explicitly: losing a recorded drive to a swipe is
        // unacceptable, so a guarded sheet must not let the Hidden transition through unchallenged.
        assertTrue(sheetShouldVetoDismiss(SheetValue.Hidden, guardDismissal = true))
    }

    @Test
    fun guarded_sheet_does_not_veto_settling_into_expanded() {
        // Only the terminal (dismissing) target is a data-loss risk — the sheet settling open is not.
        assertFalse(sheetShouldVetoDismiss(SheetValue.Expanded, guardDismissal = true))
    }

    // --- action-count enforcement ------------------------------------------------------------

    @Test
    fun zero_one_or_two_actions_are_all_allowed() {
        validateActionSheetActions(emptyList())
        validateActionSheetActions(listOf(MilewaySheetAction("Confirm", {})))
        validateActionSheetActions(listOf(MilewaySheetAction("Confirm", {}), MilewaySheetAction("Cancel", {})))
    }

    @Test
    fun a_third_action_is_rejected() {
        // The taxonomy says MAX TWO for a reason: a blocking/destructive decision sheet is not a menu.
        assertFailsWith<IllegalArgumentException> {
            validateActionSheetActions(
                listOf(
                    MilewaySheetAction("Confirm", {}),
                    MilewaySheetAction("Cancel", {}),
                    MilewaySheetAction("Something else", {}),
                ),
            )
        }
    }
}
