package com.mileway

import com.mileway.ui.home.quickActions
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * V29 P29.H.2: [quickActions] stays plugin-config-agnostic — the caller pre-resolves "Ask Advance"
 * (QR flow vs classic form) before building the list. Covers that each quick action fires the
 * lambda it was built with, and specifically that "Ask Advance" fires whichever `onAskAdvance` the
 * caller passed in (the actual QR-vs-classic decision is exercised by HomeScreen callers).
 */
class HomeQuickActionsTest {
    @Test
    fun `Add Expense fires its own callback only`() {
        var addExpense = false
        var askAdvance = false
        var addInvoice = false
        var illustrative = false
        val actions =
            quickActions(
                onAddExpense = { addExpense = true },
                onAskAdvance = { askAdvance = true },
                onAddInvoice = { addInvoice = true },
                onIllustrative = { illustrative = true },
            )

        actions.first { it.label == "Add Expense" }.onClick()

        assertTrue(addExpense)
        assertFalse(askAdvance)
        assertFalse(addInvoice)
        assertFalse(illustrative)
    }

    @Test
    fun `Ask Advance fires whichever callback the caller pre-resolved`() {
        var qrAdvanceCalled = false
        val actions =
            quickActions(
                onAddExpense = {},
                onAskAdvance = { qrAdvanceCalled = true },
                onAddInvoice = {},
                onIllustrative = {},
            )

        actions.first { it.label == "Ask Advance" }.onClick()

        assertTrue(qrAdvanceCalled)
    }

    @Test
    fun `Add Invoice fires its own real callback, not the illustrative one`() {
        var addInvoice = false
        var illustrative = false
        val actions =
            quickActions(
                onAddExpense = {},
                onAskAdvance = {},
                onAddInvoice = { addInvoice = true },
                onIllustrative = { illustrative = true },
            )

        actions.first { it.label == "Add Invoice" }.onClick()

        assertTrue(addInvoice)
        assertFalse(illustrative)
    }

    @Test
    fun `Create Voucher remains illustrative`() {
        var illustrative = false
        val actions =
            quickActions(
                onAddExpense = {},
                onAskAdvance = {},
                onAddInvoice = {},
                onIllustrative = { illustrative = true },
            )

        actions.first { it.label == "Create Voucher" }.onClick()

        assertTrue(illustrative)
    }

    @Test
    fun `exactly the four canonical actions, in order`() {
        val actions = quickActions(onAddExpense = {}, onAskAdvance = {}, onAddInvoice = {}, onIllustrative = {})
        assertEquals(listOf("Add Expense", "Create Voucher", "Ask Advance", "Add Invoice"), actions.map { it.label })
    }
}
