package com.mileway.feature.tracking.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CounterReconcileTest {
    @Test
    fun `equal counts are in sync`() {
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 100L, dbCount = 100L)
        assertFalse(r.isDiverged)
        assertEquals(0L, r.delta)
        assertEquals(100L, r.dbCount)
    }

    @Test
    fun `both zero counts are in sync`() {
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 0L, dbCount = 0L)
        assertFalse(r.isDiverged)
        assertEquals(0L, r.delta)
    }

    @Test
    fun `DB has more points than DataStore triggers divergence`() {
        // Typical after a service kill: DB has the inserts but DataStore counter wasn't flushed.
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 80L, dbCount = 120L)
        assertTrue(r.isDiverged)
        assertEquals(120L, r.dbCount)
        assertEquals(80L, r.datastoreCount)
        assertEquals(40L, r.delta)
    }

    @Test
    fun `DataStore ahead of DB triggers divergence with negative delta`() {
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 50L, dbCount = 45L)
        assertTrue(r.isDiverged)
        assertEquals(-5L, r.delta)
        assertEquals(45L, r.dbCount)
    }

    @Test
    fun `DataStore zero DB nonzero is diverged`() {
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 0L, dbCount = 300L)
        assertTrue(r.isDiverged)
        assertEquals(300L, r.delta)
        assertEquals(300L, r.dbCount)
    }

    @Test
    fun `DB count is always the authoritative value`() {
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 999L, dbCount = 1L)
        assertTrue(r.isDiverged)
        assertEquals(1L, r.dbCount, "DB is authoritative, even when smaller")
    }

    @Test
    fun `single-point difference is diverged`() {
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 10L, dbCount = 11L)
        assertTrue(r.isDiverged)
        assertEquals(1L, r.delta)
    }

    @Test
    fun `large equal counts are in sync`() {
        val r = CounterReconcilePolicy.reconcile(datastoreCount = 100_000L, dbCount = 100_000L)
        assertFalse(r.isDiverged)
        assertEquals(0L, r.delta)
    }
}
