package com.mileway.feature.travel.viewmodel

import com.mileway.feature.travel.repository.TravelCreateRepository
import com.mileway.feature.travel.repository.TravelSubmissionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TR.2-TR.7: the shared TravelCreateContract reducer shape (`FormSubmissionScaffold` + rotating
 * [TravelCreateEffect]), exercised across three representative ViewModels rather than all six
 * near-identical create flows:
 * - [CreateFlightViewModel] — the plain "two cities + a date" field gate shared with Bus/Trip/Visa.
 * - [CreateHotelViewModel] — the one flow with a numeric-field parsing edge case (guests).
 * - [CreateMjpViewModel] — the one flow with a list-based (multi-leg) reducer.
 * CreateBusViewModel/CreateTripViewModel/CreateVisaViewModel differ from CreateFlightViewModel only
 * in field names, not in reducer shape, so they are not separately covered here.
 */
class TravelCreateEffectMappingTest {
    @Test
    fun `toEffect maps every TravelSubmissionResult branch to its TravelCreateEffect counterpart`() {
        assertEquals(
            TravelCreateEffect.Success("FLT-1"),
            TravelSubmissionResult.Submitted("FLT-1").toEffect(),
        )
        assertEquals(
            TravelCreateEffect.NeedsApproval("FLT-2"),
            TravelSubmissionResult.NeedsApproval("FLT-2").toEffect(),
        )
        assertEquals(
            TravelCreateEffect.Violation(listOf("a", "b")),
            TravelSubmissionResult.PolicyViolation(listOf("a", "b")).toEffect(),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CreateFlightViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CreateFlightViewModel(TravelCreateRepository())

    @Test
    fun `canSubmit is false until both cities and a travel date are non-blank`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateFlightAction.SetFromCity("Pune"))
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateFlightAction.SetToCity("   ")) // whitespace-only counts as blank
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateFlightAction.SetToCity("Goa"))
        assertFalse(vm.state.value.canSubmit) // date still missing

        vm.onAction(CreateFlightAction.SetTravelDate("01-08-2026"))
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `field setters update only their own field`() {
        val vm = viewModel()
        vm.onAction(CreateFlightAction.SetPreferredAirline("IndiGo"))
        vm.onAction(CreateFlightAction.SetCabinClass("Business"))

        val state = vm.state.value
        assertEquals("IndiGo", state.preferredAirline)
        assertEquals("Business", state.cabinClass)
        assertEquals("", state.fromCity)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `submit is a no-op while canSubmit is false`() =
        runTest {
            val vm = viewModel()
            vm.onAction(CreateFlightAction.Submit)

            assertNull(vm.state.value.lastResult)
            assertFalse(vm.state.value.isSubmitting)
        }

    @Test
    fun `submits rotate through success, approval and violation in order`() =
        runTest {
            val vm = viewModel()
            vm.onAction(CreateFlightAction.SetFromCity("Pune"))
            vm.onAction(CreateFlightAction.SetToCity("Goa"))
            vm.onAction(CreateFlightAction.SetTravelDate("01-08-2026"))

            vm.onAction(CreateFlightAction.Submit)
            assertIs<TravelSubmissionResult.Submitted>(vm.state.value.lastResult)
            assertFalse(vm.state.value.isSubmitting)

            vm.onAction(CreateFlightAction.Submit)
            assertIs<TravelSubmissionResult.NeedsApproval>(vm.state.value.lastResult)

            vm.onAction(CreateFlightAction.Submit)
            val violation = assertIs<TravelSubmissionResult.PolicyViolation>(vm.state.value.lastResult)
            assertTrue(violation.messages.isNotEmpty())
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CreateHotelViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CreateHotelViewModel(TravelCreateRepository())

    private fun fillCityAndDates(vm: CreateHotelViewModel) {
        vm.onAction(CreateHotelAction.SetCity("Pune"))
        vm.onAction(CreateHotelAction.SetCheckInDate("01-08-2026"))
        vm.onAction(CreateHotelAction.SetCheckOutDate("03-08-2026"))
    }

    @Test
    fun `canSubmit becomes true once city and dates are filled, using the default guest count`() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)

        fillCityAndDates(vm)

        assertEquals(1, vm.state.value.guests) // default guestsText "1"
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `a non-numeric or non-positive guests value fails the gate even though the field is non-blank`() {
        val vm = viewModel()
        fillCityAndDates(vm)
        assertTrue(vm.state.value.canSubmit)

        vm.onAction(CreateHotelAction.SetGuests("abc"))
        assertEquals(0, vm.state.value.guests) // toIntOrNull() ?: 0
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateHotelAction.SetGuests("0"))
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateHotelAction.SetGuests("-2"))
        assertEquals(-2, vm.state.value.guests)
        assertFalse(vm.state.value.canSubmit)

        vm.onAction(CreateHotelAction.SetGuests("4"))
        assertEquals(4, vm.state.value.guests)
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `submits rotate through success, approval and violation`() =
        runTest {
            val vm = viewModel()
            fillCityAndDates(vm)

            vm.onAction(CreateHotelAction.Submit)
            assertIs<TravelSubmissionResult.Submitted>(vm.state.value.lastResult)

            vm.onAction(CreateHotelAction.Submit)
            assertIs<TravelSubmissionResult.NeedsApproval>(vm.state.value.lastResult)

            vm.onAction(CreateHotelAction.Submit)
            assertIs<TravelSubmissionResult.PolicyViolation>(vm.state.value.lastResult)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CreateMjpViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CreateMjpViewModel(TravelCreateRepository())

    private fun completeLeg(
        vm: CreateMjpViewModel,
        index: Int,
        from: String,
        to: String,
        date: String,
    ) {
        vm.onAction(CreateMjpAction.SetLegFrom(index, from))
        vm.onAction(CreateMjpAction.SetLegTo(index, to))
        vm.onAction(CreateMjpAction.SetLegDate(index, date))
    }

    @Test
    fun `starts with one incomplete leg and requires a purpose plus every leg complete`() {
        val vm = viewModel()
        assertEquals(1, vm.state.value.legs.size)
        assertFalse(vm.state.value.canSubmit) // purpose blank, leg incomplete

        vm.onAction(CreateMjpAction.SetPurpose("Client visit"))
        assertFalse(vm.state.value.canSubmit) // leg still incomplete

        completeLeg(vm, 0, "Pune", "Delhi", "01-08-2026")
        assertTrue(vm.state.value.canSubmit)
    }

    @Test
    fun `adding a second leg reopens the gate until that leg is completed, without disturbing the first`() {
        val vm = viewModel()
        vm.onAction(CreateMjpAction.SetPurpose("Client visit"))
        completeLeg(vm, 0, "Pune", "Delhi", "01-08-2026")
        assertTrue(vm.state.value.canSubmit)

        vm.onAction(CreateMjpAction.AddLeg)
        assertEquals(2, vm.state.value.legs.size)
        assertFalse(vm.state.value.canSubmit) // new leg is incomplete

        completeLeg(vm, 1, "Delhi", "Mumbai", "03-08-2026")
        assertTrue(vm.state.value.canSubmit)
        assertEquals("Pune", vm.state.value.legs[0].fromCity) // untouched by editing leg 1
    }

    @Test
    fun `removeLeg is a guarded no-op while only one leg remains, but works once there are two`() {
        val vm = viewModel()
        assertEquals(1, vm.state.value.legs.size)

        vm.onAction(CreateMjpAction.RemoveLeg(0))
        assertEquals(1, vm.state.value.legs.size) // guarded: never below one leg

        vm.onAction(CreateMjpAction.AddLeg)
        assertEquals(2, vm.state.value.legs.size)

        vm.onAction(CreateMjpAction.RemoveLeg(0))
        assertEquals(1, vm.state.value.legs.size) // back down to one, allowed once size > 1
    }

    @Test
    fun `submits rotate through success, approval and violation`() =
        runTest {
            val vm = viewModel()
            vm.onAction(CreateMjpAction.SetPurpose("Client visit"))
            completeLeg(vm, 0, "Pune", "Delhi", "01-08-2026")

            vm.onAction(CreateMjpAction.Submit)
            assertIs<TravelSubmissionResult.Submitted>(vm.state.value.lastResult)

            vm.onAction(CreateMjpAction.Submit)
            assertIs<TravelSubmissionResult.NeedsApproval>(vm.state.value.lastResult)

            vm.onAction(CreateMjpAction.Submit)
            assertIs<TravelSubmissionResult.PolicyViolation>(vm.state.value.lastResult)
        }
}
