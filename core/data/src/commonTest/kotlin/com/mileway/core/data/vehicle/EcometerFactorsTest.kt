package com.mileway.core.data.vehicle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PLAN_V24 P11.4 — the Ecometer CO₂/fuel-saved aggregation, unit-tested once as a pure function. */
class EcometerFactorsTest {
    @Test
    fun empty_is_all_zero() {
        val t = computeEcometer(emptyList())
        assertEquals(EcometerTotals(), t)
    }

    @Test
    fun two_wheeler_saves_vs_car_baseline() {
        // twoWheeler = 55 g/km, 2.5 ₹/km; baseline 170 g/km, 7.0 ₹/km. 10 km:
        // CO2 saved = (170-55)*10 = 1150 g = 1.15 kg; fuel saved = (7-2.5)*10 = 45 ₹.
        val t = computeEcometer(listOf(EcoTrip("twoWheeler", 10.0)))
        assertEquals(1, t.trips)
        assertEquals(10.0, t.distanceKm)
        assertEquals(1.15, t.co2SavedKg, 1e-9)
        assertEquals(45.0, t.fuelSavedInr, 1e-9)
    }

    @Test
    fun car_trip_saves_nothing_but_still_counts_distance() {
        val t = computeEcometer(listOf(EcoTrip("fourWheelerPetrol", 12.0)))
        assertEquals(1, t.trips)
        assertEquals(12.0, t.distanceKm)
        assertEquals(0.0, t.co2SavedKg, 1e-9)
        assertEquals(0.0, t.fuelSavedInr, 1e-9)
    }

    @Test
    fun unknown_key_falls_back_to_baseline_never_negative() {
        val t = computeEcometer(listOf(EcoTrip("teleporter", 5.0)))
        assertTrue(t.co2SavedKg >= 0.0)
        assertTrue(t.fuelSavedInr >= 0.0)
        assertEquals(0.0, t.co2SavedKg, 1e-9)
    }

    @Test
    fun non_positive_distance_is_ignored() {
        val t = computeEcometer(listOf(EcoTrip("twoWheeler", 0.0), EcoTrip("electricBike", -3.0)))
        assertEquals(0, t.trips)
        assertEquals(0.0, t.distanceKm)
    }

    @Test
    fun totals_sum_across_trips() {
        val t = computeEcometer(listOf(EcoTrip("twoWheeler", 10.0), EcoTrip("electricBike", 10.0)))
        // electricBike = 20 g/km, 0.4 ₹/km. CO2 = 1.15 + (170-20)*10/1000=1.5 → 2.65 kg.
        assertEquals(2, t.trips)
        assertEquals(20.0, t.distanceKm)
        assertEquals(2.65, t.co2SavedKg, 1e-9)
        assertEquals(45.0 + 66.0, t.fuelSavedInr, 1e-9)
    }
}
