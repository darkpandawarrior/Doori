package com.mileway.feature.logging.viewmodel

import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.data.model.network.LogMilesService
import com.mileway.feature.logging.ui.model.LocationEntry
import com.mileway.feature.logging.ui.model.LocationStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [LogMilesUiState.toSubmitRequest] (P5.2) — asserts the built
 * [com.mileway.core.data.model.network.LogMilesSubmitRequestV2] carries every field the Step 2 UI
 * actually collects (tagged employees, note, selected service, invoice date), which previously
 * never reached the network payload.
 */
class LogMilesSubmitRequestMappingTest {
    private val originEntry = LocationEntry(name = "Origin", subtitle = "Start", lat = 18.5, lng = 73.8)
    private val destEntry = LocationEntry(name = "Destination", subtitle = "End", lat = 18.6, lng = 73.9)
    private val vehicle = ApprovedVehicle(vehicleKey = "car", vehicleName = "Car", vehiclePricing = 12.0)
    private val service = LogMilesService(id = 42L, name = "Sales", glCode = "GL-1")

    private fun baseState() =
        LogMilesUiState(
            selectedVehicle = vehicle,
            stops =
                listOf(
                    LocationStop(id = 1L, entry = originEntry),
                    LocationStop(id = 2L, entry = destEntry),
                ),
            distanceKm = 10.0,
        )

    @Test
    fun `request carries all four Step 2 fields when populated`() {
        val state =
            baseState().copy(
                taggedEmployees = listOf("alice", "bob"),
                logMilesNote = "Client visit",
                selectedService = service,
                invoiceDateMillis = 1_700_000_000_000L,
            )

        val request = state.toSubmitRequest()

        assertEquals(listOf("alice", "bob"), request.employees)
        assertEquals("Client visit", request.notes)
        assertEquals(42L, request.serviceId)
        assertEquals(1_700_000_000_000L, request.invoiceDate)
    }

    @Test
    fun `unset Step 2 fields map to empty-or-null, not garbage defaults`() {
        val request = baseState().toSubmitRequest()

        assertEquals(emptyList(), request.employees)
        assertNull(request.notes)
        assertNull(request.serviceId)
        assertNull(request.invoiceDate)
    }

    @Test
    fun `blank note maps to null notes rather than an empty string`() {
        val request = baseState().copy(logMilesNote = "   ").toSubmitRequest()

        assertNull(request.notes)
    }

    @Test
    fun `purpose of travel and cost center fold into notes instead of being dropped`() {
        // Regression guard for the data-loss bug: these two fields used to live only in the
        // screen's local Compose state, read by nothing — every submit silently discarded them.
        val request =
            baseState()
                .copy(purposeOfTravel = "Client visit", costCenter = "CC-4521", logMilesNote = "Bring the signed PO")
                .toSubmitRequest()

        val notes = requireNotNull(request.notes)
        assertEquals(true, notes.contains("Purpose: Client visit"))
        assertEquals(true, notes.contains("Cost center: CC-4521"))
        assertEquals(true, notes.contains("Bring the signed PO"))
    }

    @Test
    fun `purpose of travel and cost center still map to null notes when all three are blank`() {
        val request = baseState().copy(purposeOfTravel = "  ", costCenter = "  ").toSubmitRequest()

        assertNull(request.notes)
    }

    @Test
    fun `core fields are still mapped alongside the new Step 2 fields`() {
        val request = baseState().toSubmitRequest()

        assertEquals("car", request.vehicleType)
        assertEquals(10.0, request.distance)
        assertEquals(originEntry.name, request.origin?.name)
        assertEquals(destEntry.name, request.destination?.name)
    }
}
