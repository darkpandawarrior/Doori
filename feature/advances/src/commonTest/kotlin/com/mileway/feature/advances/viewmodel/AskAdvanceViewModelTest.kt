package com.mileway.feature.advances.viewmodel

import com.mileway.feature.advances.data.AdvancesRequestStore
import com.mileway.feature.advances.data.MockAdvancesRepository
import com.mileway.feature.advances.validation.PettyRequestError
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
class AskAdvanceViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = AskAdvanceViewModel(MockAdvancesRepository(AdvancesRequestStore()))

    @Test
    fun `submitting a blank form surfaces every required-field error and does not submit`() =
        runTest {
            val vm = newViewModel()

            vm.onAction(AskAdvanceAction.Submit)
            advanceUntilIdle()

            val state = vm.state.value
            assertTrue(PettyRequestError.AMOUNT_INVALID in state.errors)
            assertTrue(PettyRequestError.TITLE_BLANK in state.errors)
            assertTrue(PettyRequestError.DESCRIPTION_BLANK in state.errors)
            assertTrue(PettyRequestError.DECLARATION_NOT_ACCEPTED in state.errors)
            assertTrue(!state.isSuccess)
        }

    @Test
    fun `a fully valid submit flips to the success state with a positive permission id`() =
        runTest {
            val vm = newViewModel()

            vm.onAction(AskAdvanceAction.SelectType("Travel Petty Cash"))
            vm.onAction(AskAdvanceAction.SetAmount("500"))
            vm.onAction(AskAdvanceAction.SetTitle("Client visit"))
            vm.onAction(AskAdvanceAction.SetDescription("Advance for the Pune client visit trip."))
            vm.onAction(AskAdvanceAction.SetDeclaration(true))
            vm.onAction(AskAdvanceAction.Submit)
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals(emptyList(), state.errors)
            assertTrue(state.isSuccess)
            assertTrue((state.submittedPermissionId ?: 0L) > 0L)
            assertTrue(!state.isSubmitting)
        }

    @Test
    fun `declaration not accepted blocks submit even when every other field is valid`() =
        runTest {
            val vm = newViewModel()

            vm.onAction(AskAdvanceAction.SelectType("Travel Petty Cash"))
            vm.onAction(AskAdvanceAction.SetAmount("500"))
            vm.onAction(AskAdvanceAction.SetTitle("Client visit"))
            vm.onAction(AskAdvanceAction.SetDescription("Advance for the Pune client visit trip."))
            vm.onAction(AskAdvanceAction.Submit)
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals(listOf(PettyRequestError.DECLARATION_NOT_ACCEPTED), state.errors)
            assertTrue(!state.isSuccess)
        }
}
