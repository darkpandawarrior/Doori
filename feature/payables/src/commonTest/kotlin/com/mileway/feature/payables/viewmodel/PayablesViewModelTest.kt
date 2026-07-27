package com.mileway.feature.payables.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.payables.model.NewLineItemDraft
import com.mileway.feature.payables.repository.PayablesRepository
import com.siddharth.kmp.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Covers [PayablesViewModel]: the PO/invoice home load, the create-PO step form, and PO detail lookup. */
@OptIn(ExperimentalCoroutinesApi::class)
class PayablesViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repository: PayablesRepository = PayablesRepository()) = PayablesViewModel(repository)

    @Test
    fun `initial home state is Content seeded from the repository's POs and invoices`() {
        val repository = PayablesRepository()
        val vm = newViewModel(repository)

        val home = vm.state.value.homeState
        assertIs<ScreenState.Content<*>>(home)
        assertEquals(repository.purchaseOrders, home.dataOrNull?.purchaseOrders)
        assertEquals(repository.invoices, home.dataOrNull?.invoices)
    }

    @Test
    fun `form starts on step 1 with a single blank line item`() {
        val vm = newViewModel()

        assertEquals(1, vm.state.value.form.step)
        assertEquals(listOf(NewLineItemDraft()), vm.state.value.form.lineItems)
    }

    @Test
    fun `step navigation moves between step 1 and step 2`() {
        val vm = newViewModel()

        vm.onAction(PayablesAction.GoToStep2)
        assertEquals(2, vm.state.value.form.step)

        vm.onAction(PayablesAction.GoToStep1)
        assertEquals(1, vm.state.value.form.step)
    }

    @Test
    fun `field setters update the create-PO form`() {
        val vm = newViewModel()

        vm.onAction(PayablesAction.SetVendorName("Acme Traders"))
        vm.onAction(PayablesAction.SetDeliveryDate("2024-03-01"))
        vm.onAction(PayablesAction.SetOfficeLocation("North Branch – Mumbai"))

        assertEquals("Acme Traders", vm.state.value.form.vendorName)
        assertEquals("2024-03-01", vm.state.value.form.deliveryDate)
        assertEquals("North Branch – Mumbai", vm.state.value.form.officeLocation)
    }

    @Test
    fun `AddLineItem appends a new blank line item`() {
        val vm = newViewModel()

        vm.onAction(PayablesAction.AddLineItem)

        assertEquals(2, vm.state.value.form.lineItems.size)
    }

    @Test
    fun `RemoveLineItem drops the item at the given index`() {
        val vm = newViewModel()
        vm.onAction(PayablesAction.AddLineItem)
        vm.onAction(PayablesAction.UpdateLineItem(1, NewLineItemDraft(description = "Pens")))

        vm.onAction(PayablesAction.RemoveLineItem(0))

        assertEquals(listOf(NewLineItemDraft(description = "Pens")), vm.state.value.form.lineItems)
    }

    @Test
    fun `UpdateLineItem replaces the item at the given index in place`() {
        val vm = newViewModel()
        val edited = NewLineItemDraft(description = "Stapler", qty = 3, unitPrice = "180", gstPercent = 18)

        vm.onAction(PayablesAction.UpdateLineItem(0, edited))

        assertEquals(listOf(edited), vm.state.value.form.lineItems)
    }

    @Test
    fun `SubmitPo mints an id from the vendor name and emits NavigateToSuccess`() =
        runTest {
            val vm = newViewModel()
            // Blank vendor name hashes to 0, so the id is deterministic: PO-NEW-1000.
            assertEquals("", vm.state.value.form.vendorName)

            vm.onAction(PayablesAction.SubmitPo)
            val effect = vm.effect.first()

            assertEquals("PO-NEW-1000", vm.state.value.lastSubmittedId)
            assertIs<PayablesEffect.NavigateToSuccess>(effect)
            assertEquals("PO-NEW-1000", effect.poId)
        }

    @Test
    fun `ResetForm clears the form and the last submitted id`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(PayablesAction.SetVendorName("Acme Traders"))
            vm.onAction(PayablesAction.GoToStep2)
            vm.onAction(PayablesAction.SubmitPo)
            vm.effect.first()

            vm.onAction(PayablesAction.ResetForm)

            assertEquals("", vm.state.value.form.vendorName)
            assertEquals(1, vm.state.value.form.step)
            assertEquals("", vm.state.value.lastSubmittedId)
        }

    @Test
    fun `OpenDetail with a known id populates detailState with Content`() {
        val repository = PayablesRepository()
        val vm = newViewModel(repository)
        val expected = repository.purchaseOrders.first()

        vm.onAction(PayablesAction.OpenDetail(expected.id))

        assertEquals(expected, vm.state.value.detailState.dataOrNull)
    }

    @Test
    fun `OpenDetail with an unknown id yields Empty, not stale data`() {
        val vm = newViewModel()
        vm.onAction(PayablesAction.OpenDetail("PO-DOES-NOT-EXIST"))

        assertIs<ScreenState.Empty>(vm.state.value.detailState)
    }

    @Test
    fun `ShowMessage emits a ShowToast effect carrying the message`() =
        runTest {
            val vm = newViewModel()

            vm.onAction(PayablesAction.ShowMessage("Saved offline"))
            val effect = vm.effect.first()

            assertIs<PayablesEffect.ShowToast>(effect)
            assertEquals(UiText.Dynamic("Saved offline"), effect.message)
        }
}
