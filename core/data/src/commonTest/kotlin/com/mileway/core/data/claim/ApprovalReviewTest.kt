package com.mileway.core.data.claim

import com.mileway.core.data.model.db.PeriodLockEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class ApprovalReviewTest {
    @Test
    fun `locked December rolls into January without changing the submitted instant`() =
        runTest {
            val original = Instant.parse("2026-12-31T23:59:59Z").toEpochMilliseconds()
            val period = nextOpenPeriod(original) { key -> if (key == "2026-12") PeriodLockEntity(key, 1, "finance") else null }
            assertEquals("2027-01", period)
            assertEquals(Instant.parse("2026-12-31T23:59:59Z").toEpochMilliseconds(), original)
        }
}
