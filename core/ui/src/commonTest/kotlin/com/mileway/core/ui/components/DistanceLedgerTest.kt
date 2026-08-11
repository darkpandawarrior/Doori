package com.mileway.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DistanceLedgerTest {
    private val typical =
        DistanceLedger(
            rawKm = 14.9,
            cleanedKm = 12.4,
            claimedKm = 12.4,
            abnormalKm = 1.1,
            mockKm = 1.4,
            spikeKm = 1.4,
            odometerKm = 12.6,
        )

    @Test
    fun a_balanced_ledger_reports_balanced() {
        // cleaned == raw - abnormal - mock  =>  12.4 == 14.9 - 1.1 - 1.4
        assertTrue(typical.balances(), "expected the ledger to balance: $typical")
    }

    @Test
    fun spikes_are_excluded_from_raw_and_do_not_break_the_balance() {
        // A teleport was never travelled, so it is not "distance we then removed". It appears as a
        // row for transparency but must not participate in the raw-minus-deductions identity.
        val noSpike = typical.copy(spikeKm = 0.0)
        assertTrue(noSpike.balances(), "removing the spike figure must not unbalance the ledger")
    }

    @Test
    fun an_unbalanced_ledger_is_detected_rather_than_silently_shown() {
        val broken = typical.copy(cleanedKm = 9.0)
        assertFalse(broken.balances(), "a ledger that does not add up must not report as balanced")
    }

    @Test
    fun every_deduction_row_is_present_even_when_zero() {
        // "We checked and found none" is information. A row that appears only sometimes makes the
        // list look incomplete and invites the question the component exists to pre-empt.
        val clean = DistanceLedger(rawKm = 10.0, cleanedKm = 10.0, claimedKm = 10.0)
        assertEquals(3, clean.deductions().size)
        assertTrue(clean.deductions().all { it.km == 0.0 })
    }

    @Test
    fun mock_distance_is_listed_first_and_flagged_most_severely() {
        val first = typical.deductions().first()
        assertEquals("mock", first.label)
        assertEquals(StatusTone.Danger, first.tone, "a mock location implies intent, not instrument error")
    }

    @Test
    fun accessibility_summary_states_every_figure_a_reviewer_needs() {
        val s = typical.accessibilitySummary()
        listOf("Raw GPS", "Cleaned", "Odometer", "Claimed").forEach {
            assertTrue(s.contains(it), "summary missing '$it': $s")
        }
        assertTrue(s.contains("14.90"), "raw figure missing: $s")
        assertTrue(s.contains("12.40"), "claimed figure missing: $s")
    }

    @Test
    fun accessibility_summary_omits_deductions_that_did_not_happen() {
        val clean = DistanceLedger(rawKm = 10.0, cleanedKm = 10.0, claimedKm = 10.0)
        val s = clean.accessibilitySummary()
        assertFalse(s.contains("Minus"), "a clean trip should not announce empty deductions: $s")
    }

    @Test
    fun a_zero_distance_trip_does_not_divide_by_zero() {
        val empty = DistanceLedger(rawKm = 0.0, cleanedKm = 0.0, claimedKm = 0.0)
        assertTrue(empty.balances())
        assertTrue(empty.accessibilitySummary().isNotBlank())
    }
}
