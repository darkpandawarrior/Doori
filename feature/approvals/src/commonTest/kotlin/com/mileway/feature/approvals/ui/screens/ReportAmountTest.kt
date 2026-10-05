package com.mileway.feature.approvals.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class ReportAmountTest {
    @Test
    fun approvalTitleUsesTheReviewPeriodWithAHumanFallback() {
        assertEquals("Report for 2026-09", reportApprovalTitle("2026-09"))
        assertEquals("Expense report", reportApprovalTitle(null))
    }
}
