package com.mileway.feature.advances.viewmodel

import com.mileway.feature.advances.data.AdvancesRequestStore
import com.mileway.feature.advances.data.MockQrCardsRepository
import com.mileway.feature.advances.validation.QrRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QrRequestViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(mandatoryCardSelection: Boolean = true) = QrRequestViewModel(MockQrCardsRepository(AdvancesRequestStore()), mandatoryCardSelection)

    @Test
    fun `blank title and missing card selection are both flagged`() =
        runTest {
            val vm = newViewModel()
            advanceUntilIdle() // load the active-cards flow so `cardsExist` is true

            vm.onAction(QrRequestAction.SetAmount("300"))
            vm.onAction(QrRequestAction.SetDeclaration(true))
            vm.onAction(QrRequestAction.Submit)
            advanceUntilIdle()

            val state = vm.state.value
            assertTrue(QrRequestError.TITLE_BLANK in state.errors)
            assertTrue(QrRequestError.CARD_SELECTION_REQUIRED in state.errors)
            assertTrue(!state.isSuccess)
        }

    @Test
    fun `a fully valid submit with a selected card flips to the success state`() =
        runTest {
            val vm = newViewModel()
            advanceUntilIdle()
            val firstCardId = vm.state.value.cards.first().id

            vm.onAction(QrRequestAction.SelectType("Fuel QR"))
            vm.onAction(QrRequestAction.SetAmount("300"))
            vm.onAction(QrRequestAction.SetTitle("Fuel QR recharge"))
            vm.onAction(QrRequestAction.SetDescription("Recharge for the delivery fleet."))
            vm.onAction(QrRequestAction.SelectCard(firstCardId))
            vm.onAction(QrRequestAction.SetDeclaration(true))
            vm.onAction(QrRequestAction.Submit)
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals(emptyList(), state.errors)
            assertTrue(state.isSuccess)
            assertTrue((state.submittedPermissionId ?: 0L) > 0L)
        }

    @Test
    fun `card selection is not required once mandatoryCardSelection is off`() =
        runTest {
            val vm = newViewModel(mandatoryCardSelection = false)
            advanceUntilIdle()

            vm.onAction(QrRequestAction.SelectType("Fuel QR"))
            vm.onAction(QrRequestAction.SetAmount("300"))
            vm.onAction(QrRequestAction.SetTitle("Fuel QR recharge"))
            vm.onAction(QrRequestAction.SetDescription("Recharge for the delivery fleet."))
            vm.onAction(QrRequestAction.SetDeclaration(true))
            vm.onAction(QrRequestAction.Submit)
            advanceUntilIdle()

            assertTrue(QrRequestError.CARD_SELECTION_REQUIRED !in vm.state.value.errors)
            assertTrue(vm.state.value.isSuccess)
        }
}
