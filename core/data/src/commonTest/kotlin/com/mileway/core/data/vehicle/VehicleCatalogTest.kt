package com.mileway.core.data.vehicle

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P11.1: pure-function coverage for the reimbursable-amount rule and the catalog rate
 * lookup — the money math that drives every trip amount.
 */
class VehicleCatalogTest {
    @Test
    fun `amount is rate times gps distance by default`() {
        assertEquals(25.0, reimbursableAmount(ratePerKm = 5.0, gpsDistanceKm = 5.0))
    }

    @Test
    fun `odometer delta drives the amount when computing via odometer`() {
        // rate 5 × odometer 8 km = 40, not the 10 km GPS distance (=50).
        assertEquals(
            40.0,
            reimbursableAmount(ratePerKm = 5.0, gpsDistanceKm = 10.0, odometerDistanceKm = 8.0, viaOdometer = true),
        )
    }

    @Test
    fun `via-odometer falls back to gps when there is no positive odometer delta`() {
        assertEquals(
            50.0,
            reimbursableAmount(ratePerKm = 5.0, gpsDistanceKm = 10.0, odometerDistanceKm = 0.0, viaOdometer = true),
        )
    }

    @Test
    fun `negative inputs never produce a negative amount`() {
        assertEquals(0.0, reimbursableAmount(ratePerKm = -5.0, gpsDistanceKm = 10.0))
        assertEquals(0.0, reimbursableAmount(ratePerKm = 5.0, gpsDistanceKm = -10.0))
    }

    @Test
    fun `catalog rates match the policy table`() {
        assertEquals(2.50, VehicleCatalog.rateFor("twoWheeler"))
        assertEquals(5.00, VehicleCatalog.rateFor("fourWheelerPetrol"))
        assertEquals(8.00, VehicleCatalog.rateFor("autoRicshaw"))
        assertEquals(0.00, VehicleCatalog.rateFor("ownVehicle"))
        assertEquals(0.00, VehicleCatalog.rateFor("unknownKey"))
    }

    @Test
    fun `icon family maps by key with a car default`() {
        assertEquals(VehicleCatalog.ICON_BIKE, VehicleCatalog.iconKeyFor("twoWheeler"))
        assertEquals(VehicleCatalog.ICON_AUTO, VehicleCatalog.iconKeyFor("autoRicshaw"))
        assertEquals(VehicleCatalog.ICON_CAR, VehicleCatalog.iconKeyFor(null))
        assertEquals(VehicleCatalog.ICON_CAR, VehicleCatalog.iconKeyFor("unknownKey"))
    }
}
