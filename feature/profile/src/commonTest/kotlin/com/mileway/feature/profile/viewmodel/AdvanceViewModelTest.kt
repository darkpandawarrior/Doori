package com.mileway.feature.profile.viewmodel

import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.profile.model.AdvanceStatus
import com.mileway.feature.profile.repository.AdvanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AdvanceViewModelTest {
    private fun buildVm() = AdvanceViewModel(AdvanceRepository())

    @Test
    fun `detail state starts as Loading before any LoadDetail action`() {
        val vm = buildVm()
        assertIs<ScreenState.Loading>(vm.state.value.detail)
    }

    @Test
    fun `LoadDetail with a known id populates detail with that record`() {
        val vm = buildVm()
        vm.onAction(AdvanceAction.LoadDetail("ADV-001"))
        val detail = vm.state.value.detail
        assertIs<ScreenState.Content<*>>(detail)
        assertEquals("ADV-001", detail.dataOrNull?.id)
    }

    @Test
    fun `LoadDetail with an unknown id yields Empty`() {
        val vm = buildVm()
        vm.onAction(AdvanceAction.LoadDetail("ADV-DOES-NOT-EXIST"))
        assertIs<ScreenState.Empty>(vm.state.value.detail)
    }

    @Test
    fun `LoadDetail on a rejected advance exposes its decline reason and approver chain`() {
        val vm = buildVm()
        vm.onAction(AdvanceAction.LoadDetail("ADV-005"))
        val record = vm.state.value.detail.dataOrNull
        assertEquals(AdvanceStatus.REJECTED, record?.status)
        assertTrue(!record?.declineReason.isNullOrBlank())
        assertTrue(record!!.approverChain.isNotEmpty())
    }

    // P4.5: StartTripAgainstAdvance must emit a NavigateToTripStart effect carrying the
    // advance id, plus a freshly-minted trip id for feature/tracking's trip-start flow to use.
    @Test
    fun `StartTripAgainstAdvance emits NavigateToTripStart carrying the advance id`() =
        runTest {
            val vm = buildVm()
            vm.onAction(AdvanceAction.StartTripAgainstAdvance("ADV-001"))
            val effect = vm.effect.first()
            assertIs<AdvanceEffect.NavigateToTripStart>(effect)
            assertEquals("ADV-001", effect.advanceId)
            assertTrue(effect.tripId.isNotBlank())
        }

    @Test
    fun `StartTripAgainstAdvance mints a distinct trip id per invocation`() =
        runTest {
            val vm = buildVm()
            vm.onAction(AdvanceAction.StartTripAgainstAdvance("ADV-002"))
            val firstTripId = (vm.effect.first() as AdvanceEffect.NavigateToTripStart).tripId

            vm.onAction(AdvanceAction.StartTripAgainstAdvance("ADV-002"))
            val secondTripId = (vm.effect.first() as AdvanceEffect.NavigateToTripStart).tripId

            assertNotEquals(firstTripId, secondTripId)
        }

    // P27.E.8: LogExpenseAgainstAdvance must emit a NavigateToExpenseEntry effect carrying an
    // ExpenseSourceContext.Advance built from the repository record (id + purpose label).
    @Test
    fun `LogExpenseAgainstAdvance emits NavigateToExpenseEntry carrying an Advance context`() =
        runTest {
            val vm = buildVm()
            vm.onAction(AdvanceAction.LogExpenseAgainstAdvance("ADV-001"))

            val effect = vm.effect.first()
            assertIs<AdvanceEffect.NavigateToExpenseEntry>(effect)
            assertEquals(
                ExpenseSourceContext.Advance("ADV-001", "Field visit expenses – Nashik"),
                effect.context,
            )
        }

    @Test
    fun `LogExpenseAgainstAdvance with an unknown id still carries the id, with no label`() =
        runTest {
            val vm = buildVm()
            vm.onAction(AdvanceAction.LogExpenseAgainstAdvance("ADV-DOES-NOT-EXIST"))

            val effect = vm.effect.first() as AdvanceEffect.NavigateToExpenseEntry
            assertEquals(ExpenseSourceContext.Advance("ADV-DOES-NOT-EXIST", null), effect.context)
        }
}
