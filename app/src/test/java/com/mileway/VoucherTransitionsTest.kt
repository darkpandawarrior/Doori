package com.mileway

import com.mileway.core.data.model.db.VoucherStatus
import com.mileway.feature.tracking.repository.VoucherTransitions
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * P3.2: [VoucherTransitions] is Mileway's own simplified voucher status-lifecycle graph —
 * `DRAFT -> PENDING -> {APPROVED, REJECTED, SETTLED}`, with the three decided/paid states
 * terminal. Every legal single-step edge is accepted and the illegal `DRAFT -> SETTLED` jump
 * (skipping PENDING entirely) is rejected.
 */
class VoucherTransitionsTest {
    @Test
    fun `DRAFT allows only PENDING`() {
        assertEquals(setOf(VoucherStatus.PENDING), VoucherTransitions.allowed(VoucherStatus.DRAFT))
    }

    @Test
    fun `PENDING allows APPROVED, REJECTED and SETTLED`() {
        assertEquals(
            setOf(VoucherStatus.APPROVED, VoucherStatus.REJECTED, VoucherStatus.SETTLED),
            VoucherTransitions.allowed(VoucherStatus.PENDING),
        )
    }

    @Test
    fun `terminal statuses allow nothing`() {
        assertTrue(VoucherTransitions.allowed(VoucherStatus.APPROVED).isEmpty())
        assertTrue(VoucherTransitions.allowed(VoucherStatus.REJECTED).isEmpty())
        assertTrue(VoucherTransitions.allowed(VoucherStatus.SETTLED).isEmpty())
    }

    @Test
    fun `every legal edge is accepted`() {
        assertTrue(VoucherTransitions.isAllowed(VoucherStatus.DRAFT, VoucherStatus.PENDING))
        assertTrue(VoucherTransitions.isAllowed(VoucherStatus.PENDING, VoucherStatus.APPROVED))
        assertTrue(VoucherTransitions.isAllowed(VoucherStatus.PENDING, VoucherStatus.REJECTED))
        assertTrue(VoucherTransitions.isAllowed(VoucherStatus.PENDING, VoucherStatus.SETTLED))
    }

    @Test
    fun `an illegal jump straight from DRAFT to SETTLED is rejected`() {
        assertFalse(VoucherTransitions.isAllowed(VoucherStatus.DRAFT, VoucherStatus.SETTLED))
    }

    @Test
    fun `an illegal jump straight from DRAFT to APPROVED is rejected`() {
        assertFalse(VoucherTransitions.isAllowed(VoucherStatus.DRAFT, VoucherStatus.APPROVED))
    }

    @Test
    fun `a terminal status cannot move again`() {
        assertFalse(VoucherTransitions.isAllowed(VoucherStatus.APPROVED, VoucherStatus.SETTLED))
        assertFalse(VoucherTransitions.isAllowed(VoucherStatus.REJECTED, VoucherStatus.PENDING))
    }
}
