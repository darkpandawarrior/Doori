package com.mileway.feature.payments.repository

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReportPayoutBoundaryTest {
    @Test
    fun unconfiguredReportPayoutFailsWithoutFallingThroughToQrOrUpi() =
        runTest {
            val repository = PaymentsRepository()
            assertFailsWith<IllegalArgumentException> { repository.payReport("report") }
        }
}
