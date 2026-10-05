package com.mileway.core.forms

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.JustificationReason
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpenseExceptionValidationTest {
    private val line = ExpenseLine("line", 100, "INR", merchant = "Cafe", category = "FOOD")

    @Test
    fun affidavitNeedsConsentAndNonblankNote() {
        assertFalse(line.hasCompleteAffidavit())
        assertFalse(line.copy(affidavitNote = "Receipt lost").hasCompleteAffidavit())
        assertFalse(line.copy(affidavitAccepted = true, affidavitNote = " ").hasCompleteAffidavit())
        assertTrue(line.copy(affidavitAccepted = true, affidavitNote = "Receipt lost").hasCompleteAffidavit())
    }

    @Test
    fun offlineOtherFallbackNeedsANote() {
        assertTrue(line.hasValidJustification())
        assertTrue(line.copy(justificationReason = JustificationReason.CLIENT_REQUEST).hasValidJustification())
        assertFalse(line.copy(justificationReason = JustificationReason.OTHER).hasValidJustification())
        assertFalse(line.copy(justificationReason = JustificationReason.OTHER, justificationNote = " ").hasValidJustification())
        assertTrue(line.copy(justificationReason = JustificationReason.OTHER, justificationNote = "Urgent client visit").hasValidJustification())
    }
}
