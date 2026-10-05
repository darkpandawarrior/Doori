package com.mileway.feature.logging.report

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.feature.advances.reconcile.AdvanceReconciliation
import com.mileway.feature.advances.reconcile.UnreconciledLine
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportTotalsLabelTest {
    @Test
    fun totalsShowGroupedMajorUnitsWithTwoDecimals() {
        val report = Report("internal-id", "employee", listOf(ExpenseLine("line", 2785000, "INR", merchant = "Cafe", category = "FOOD")))
        assertEquals("1 items · ₹ 27,850.00", reportTotalsLabel(report))
    }

    @Test
    fun reconciliationLabelsUseSignedNetAndTheSharedFormatter() {
        val owed = AdvanceReconciliation("INR", 1_000, 500, 500, emptyList())
        assertEquals("Owed to employee · ₹ 5.00", reconciliationLabel(owed))
        assertEquals("Owed back · ₹ 5.00", reconciliationLabel(owed.copy(appliedMinor = 1_500, netMinor = -500)))
        assertEquals("Nothing owed · ₹ 0.00", reconciliationLabel(owed.copy(appliedMinor = 1_000, netMinor = 0)))
        assertEquals(
            "Owed to employee (reconcilable lines only) · ₹ 5.00",
            reconciliationLabel(owed.copy(excluded = listOf(UnreconciledLine("usd", "Missing pin")))),
        )
    }
}
