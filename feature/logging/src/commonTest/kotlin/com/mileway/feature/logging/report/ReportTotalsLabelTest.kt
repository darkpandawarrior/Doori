package com.mileway.feature.logging.report

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportTotalsLabelTest {
    @Test
    fun totalsShowGroupedMajorUnitsWithTwoDecimals() {
        val report = Report("internal-id", "employee", listOf(ExpenseLine("line", 2785000, "INR", merchant = "Cafe", category = "FOOD")))
        assertEquals("1 items · ₹ 27,850.00", reportTotalsLabel(report))
    }
}
