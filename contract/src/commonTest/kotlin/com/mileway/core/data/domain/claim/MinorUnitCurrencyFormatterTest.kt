package com.mileway.core.data.domain.claim

import kotlin.test.Test
import kotlin.test.assertEquals

class MinorUnitCurrencyFormatterTest {
    @Test
    fun groupsMoneyWithSymbolsAndTwoDecimals() {
        assertEquals("₹ 26,000.00", formatMinorCurrency(2600000, "INR"))
        assertEquals("$ 26,000.01", formatMinorCurrency(2600001, "USD"))
        assertEquals("£ 1.00", formatMinorCurrency(100, "GBP"))
        assertEquals("€ 1.00", formatMinorCurrency(100, "EUR"))
        assertEquals("AED 1.00", formatMinorCurrency(100, "AED"))
        assertEquals("₹ 0.00", formatMinorCurrency(0, "INR"))
        assertEquals("₹ -25.50", formatMinorCurrency(-2550, "INR"))
        assertEquals("₹ -0.01", formatMinorCurrency(-1, "INR"))
        assertEquals("₹ 92,233,720,368,547,758.07", formatMinorCurrency(Long.MAX_VALUE, "INR"))
        assertEquals("₹ -92,233,720,368,547,758.08", formatMinorCurrency(Long.MIN_VALUE, "INR"))
    }
}
