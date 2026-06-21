package com.miletracker

import app.cash.turbine.test
import com.miletracker.feature.events.repository.EventsRepository
import com.miletracker.feature.events.viewmodel.CreateEventAction
import com.miletracker.feature.events.viewmodel.CreateEventEffect
import com.miletracker.feature.events.viewmodel.CreateEventViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * EV (V17): the Create-Event reducer, canSubmit gating (title + venue + positive capacity) + the rotating
 * success / approval / violation result paths through the FormSubmissionScaffold contract.
 */
class CreateEventViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = CreateEventViewModel(EventsRepository())

    @Test
    fun `canSubmit is false until title, venue and a positive capacity are set`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        vm.onAction(CreateEventAction.SetTitle("Town Hall"))
        vm.onAction(CreateEventAction.SetVenue("Auditorium"))
        assertFalse(vm.state.value.canSubmit) // capacity still missing
        vm.onAction(CreateEventAction.SetCapacity("100"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() = runTest {
        val vm = viewModel()
        vm.onAction(CreateEventAction.SetTitle("Town Hall"))
        vm.onAction(CreateEventAction.SetVenue("Auditorium"))
        vm.onAction(CreateEventAction.SetCapacity("100"))

        vm.effect.test {
            vm.onAction(CreateEventAction.Submit)
            assertTrue(awaitItem() is CreateEventEffect.Success)
            vm.onAction(CreateEventAction.Submit)
            assertTrue(awaitItem() is CreateEventEffect.NeedsApproval)
            vm.onAction(CreateEventAction.Submit)
            assertTrue(awaitItem() is CreateEventEffect.Violation)
        }
        assertFalse(vm.state.value.isSubmitting)
    }
}
