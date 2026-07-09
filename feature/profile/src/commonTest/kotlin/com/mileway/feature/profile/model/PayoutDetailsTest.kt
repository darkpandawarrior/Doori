package com.mileway.feature.profile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** PLAN_V24 P8.2: pure coverage for the payout UPI validation + account masking + pay string. */
class PayoutDetailsTest {
    @Test
    fun `valid UPI handles match name at provider`() {
        assertTrue(PayoutDetails.isValidUpiHandle("siddharth@okhdfc"))
        assertTrue(PayoutDetails.isValidUpiHandle("user123@ybl"))
        assertTrue(PayoutDetails.isValidUpiHandle("  trimmed@paytm  "), "surrounding whitespace is trimmed")
    }

    @Test
    fun `invalid UPI handles are rejected`() {
        assertFalse(PayoutDetails.isValidUpiHandle(""))
        assertFalse(PayoutDetails.isValidUpiHandle("noatsign"))
        assertFalse(PayoutDetails.isValidUpiHandle("@bank"))
        assertFalse(PayoutDetails.isValidUpiHandle("name@"))
        assertFalse(PayoutDetails.isValidUpiHandle("two@parts@bad"))
        assertFalse(PayoutDetails.isValidUpiHandle("has space@bank"))
    }

    @Test
    fun `account is masked to the last four digits`() {
        assertEquals("•••• •••• 6789", PayoutDetails.maskedAccount)
    }

    @Test
    fun `upiPayString embeds the handle`() {
        assertEquals("upi://pay?pa=user@ybl&pn=Mileway", PayoutDetails.upiPayString("user@ybl"))
    }
}
