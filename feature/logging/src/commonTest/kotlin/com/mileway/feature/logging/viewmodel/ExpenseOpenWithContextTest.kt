package com.mileway.feature.logging.viewmodel

import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.ScannerPrefill
import com.mileway.feature.logging.repository.ExpenseRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P27.E.4: [ExpenseViewModel.openWithContext] prefill per [ExpenseSourceContext] variant. Each
 * variant's own optional display fields (see [ExpenseSourceContext] kdoc) are what's asserted here
 * — feature:logging never resolves trip/advance/event/card data from another feature's repository.
 */
class ExpenseOpenWithContextTest {
    private fun buildVm() = ExpenseViewModel(ExpenseRepository())

    @Test
    fun `None resets to a blank form carrying the None context`() {
        val vm = buildVm()
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.None))
        val form = vm.state.value.form
        assertEquals(ExpenseSourceContext.None, form.sourceContext)
        assertTrue(form.merchantName.isEmpty())
        assertTrue(form.amountText.isEmpty())
    }

    @Test
    fun `Trip prefills merchantName from the caller-supplied trip label`() {
        val vm = buildVm()
        val ctx = ExpenseSourceContext.Trip("trip-1", tripLabel = "Mumbai to Pune")
        vm.onAction(ExpenseAction.OpenWithContext(ctx))
        val form = vm.state.value.form
        assertEquals(ctx, form.sourceContext)
        assertEquals("Mumbai to Pune", form.merchantName)
    }

    @Test
    fun `TripAdvance prefills merchantName from the trip label and carries both ids`() {
        val vm = buildVm()
        val ctx = ExpenseSourceContext.TripAdvance("trip-1", "adv-1", tripLabel = "Mumbai to Pune")
        vm.onAction(ExpenseAction.OpenWithContext(ctx))
        val form = vm.state.value.form
        assertEquals(ctx, form.sourceContext)
        assertEquals("Mumbai to Pune", form.merchantName)
        assertEquals("trip-1", (form.sourceContext as ExpenseSourceContext.TripAdvance).tripId)
        assertEquals("adv-1", (form.sourceContext as ExpenseSourceContext.TripAdvance).advanceId)
    }

    @Test
    fun `Event prefills merchantName from the event label`() {
        val vm = buildVm()
        val ctx = ExpenseSourceContext.Event("evt-1", eventLabel = "Annual Summit")
        vm.onAction(ExpenseAction.OpenWithContext(ctx))
        assertEquals("Annual Summit", vm.state.value.form.merchantName)
    }

    @Test
    fun `Advance prefills merchantName from the advance label`() {
        val vm = buildVm()
        val ctx = ExpenseSourceContext.Advance("adv-1", advanceLabel = "Field visit advance")
        vm.onAction(ExpenseAction.OpenWithContext(ctx))
        assertEquals("Field visit advance", vm.state.value.form.merchantName)
    }

    @Test
    fun `Card prefills merchant and amount from the transaction and carries the ceiling`() {
        val vm = buildVm()
        val ctx = ExpenseSourceContext.Card("card-1", "txn-1", merchantName = "Indigo Airlines", transactionAmountRupees = 4500.0)
        vm.onAction(ExpenseAction.OpenWithContext(ctx))
        val form = vm.state.value.form
        assertEquals("Indigo Airlines", form.merchantName)
        assertEquals("4500", form.amountText)
        assertEquals(4500.0, (form.sourceContext as ExpenseSourceContext.Card).transactionAmountRupees)
    }

    @Test
    fun `Message carries the clarification attachment into receiptImagePath`() {
        val vm = buildVm()
        val ctx = ExpenseSourceContext.Message("clar-1", "https://example/att.jpg")
        vm.onAction(ExpenseAction.OpenWithContext(ctx))
        assertEquals("https://example/att.jpg", vm.state.value.form.receiptImagePath)
    }

    @Test
    fun `Scanner prefills merchant, amount, category and date from the OCR result`() {
        val vm = buildVm()
        val prefill =
            ScannerPrefill(
                merchant = "Acme Fuel",
                amountText = "42.50",
                currency = "USD",
                dateEpochMs = 1_700_000_000_000L,
                category = "TRAVEL",
                overallConfidence = 0.87f,
                duplicateWarning = null,
            )
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Scanner(prefill)))
        val form = vm.state.value.form
        assertEquals("Acme Fuel", form.merchantName)
        assertEquals("42.50", form.amountText)
        assertEquals(com.mileway.feature.logging.model.ExpenseCategory.TRAVEL, form.category)
        assertEquals(1_700_000_000_000L, form.dateMs)
    }

    @Test
    fun `Scanner with an unrecognized category leaves category null`() {
        val vm = buildVm()
        val prefill =
            ScannerPrefill(
                merchant = null,
                amountText = null,
                currency = null,
                dateEpochMs = null,
                category = "NOT_A_REAL_CATEGORY",
                overallConfidence = 0f,
                duplicateWarning = null,
            )
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Scanner(prefill)))
        assertNull(vm.state.value.form.category)
    }

    @Test
    fun `Edit delegates to the existing load-by-id path`() {
        val vm = buildVm()
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Edit("EXP-001")))
        val form = vm.state.value.form
        assertTrue(form.isEditing)
        assertEquals("EXP-001", form.editingId)
        assertEquals(ExpenseSourceContext.Edit("EXP-001"), form.sourceContext)
    }

    @Test
    fun `Edit with an unknown id is a no-op, leaving the prior form untouched`() {
        val vm = buildVm()
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Trip("trip-1", tripLabel = "Kept")))
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Edit("EXP-DOES-NOT-EXIST")))
        assertEquals("Kept", vm.state.value.form.merchantName)
    }
}
