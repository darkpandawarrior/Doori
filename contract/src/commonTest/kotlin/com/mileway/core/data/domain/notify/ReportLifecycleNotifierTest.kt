package com.mileway.core.data.domain.notify

import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.ReportLifecycleStateMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReportLifecycleNotifierTest {
    @Test
    fun everyLegalTransitionMapsToExactlyOneReplaySafeRow() {
        var version = 0L
        val rows = mutableMapOf<String, ReportLifecycleNotification>()
        for (from in ReportLifecycleState.entries) {
            for (event in ReportLifecycleEvent.entries) {
                val to = runCatching { ReportLifecycleStateMachine.transition(from, event) }.getOrNull() ?: continue
                val report = Report("r1", "employee", state = to, recordVersion = ++version)
                val row = assertNotNull(ReportLifecycleNotifier.map(from, report, 100))
                rows[row.id] = row
                rows[assertNotNull(ReportLifecycleNotifier.map(from, report, 100)).id] = row
                assertEquals(version.toInt(), rows.size)
                assertEquals("mileway://approvals/detail/report:r1", row.deeplink)
            }
        }
        assertNull(ReportLifecycleNotifier.map(ReportLifecycleState.DRAFT, Report("r1", "employee"), 100))
    }
}
