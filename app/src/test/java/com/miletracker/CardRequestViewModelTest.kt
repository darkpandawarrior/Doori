package com.miletracker

import com.miletracker.feature.cards.data.CardsMockDataProviderFactory
import com.miletracker.feature.cards.viewmodel.CardRequestAction
import com.miletracker.feature.cards.viewmodel.CardRequestViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Q.3 — card request multi-step reducers. */
class CardRequestViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun vm() = CardRequestViewModel(CardsMockDataProviderFactory.provider("en"))

    @Test
    fun `default card type is preselected`() {
        assertNotNull(vm().state.value.selectedCardTypeId)
    }

    @Test
    fun `submit sets the request id once policies are agreed`() =
        runTest {
            val vm = vm()
            vm.onAction(CardRequestAction.SetAgree(true))
            vm.onAction(CardRequestAction.Submit)
            assertEquals(203L, vm.state.value.submittedRequestId)
        }

    @Test
    fun `submit is ignored without agreement`() {
        val vm = vm()
        vm.onAction(CardRequestAction.Submit)
        assertNull(vm.state.value.submittedRequestId)
    }

    @Test
    fun `back from first step stays at zero`() {
        val vm = vm()
        vm.onAction(CardRequestAction.Back)
        assertEquals(0, vm.state.value.step)
    }
}
