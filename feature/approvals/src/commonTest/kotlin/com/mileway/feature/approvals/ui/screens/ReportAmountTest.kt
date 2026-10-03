package com.mileway.feature.approvals.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class ReportAmountTest {
    @Test
    fun amountsRetainAllDigitsWithoutFloatingPointRounding() {
        assertEquals("INR 50.00", formatReportAmount(5000, "INR"))
        assertEquals("INR -0.50", formatReportAmount(-50, "INR"))
        assertEquals("INR 92233720368547758.07", formatReportAmount(Long.MAX_VALUE, "INR"))
        assertEquals("INR -92233720368547758.08", formatReportAmount(Long.MIN_VALUE, "INR"))
    }
}
