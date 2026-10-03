package com.mileway.core.data.claim

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mileway.core.data.database.MilewayDatabase
import com.mileway.core.data.model.db.ClaimLineEntity
import com.mileway.core.data.model.db.ReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs the generated Room transaction against SQLite, including concurrent UNIQUE-index collisions. */
class MileageDraftInsertTest {
    @Test
    fun `competing draft inserts preserve the winning trip claim without an empty report`() =
        runBlocking {
            val db =
                Room
                    .inMemoryDatabaseBuilder<MilewayDatabase>()
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.Default)
                    .build()
            try {
                val dao = db.claimLineDao()
                val attempts =
                    (1..8)
                        .map { n ->
                            async(Dispatchers.Default) {
                                val report = ReportEntity("r$n", "emp-1", "DRAFT", 1L, 0L, 0L)
                                dao.insertMileageDraft(report, line("l$n", report.id, "trip-1"))
                            }
                        }.awaitAll()
                assertEquals(1, attempts.count { it })
                val winner = requireNotNull(dao.getBySourceTripId("trip-1"))
                assertEquals(1, (1..8).count { db.reportDao().get("r$it") != null })
                assertTrue(db.reportDao().get(winner.reportId) != null)

                val legacy = line("legacy", "legacy-report", "old-trip")
                dao.insert(legacy)
                assertFalse(dao.insertMileageDraft(ReportEntity("new", "emp-1", "DRAFT", 1L, 0L, 0L), line("new-line", "new", "old-trip")))
                assertEquals(legacy, dao.getBySourceTripId("old-trip"))
                assertNull(db.reportDao().get("new"))

                // A report-id conflict rolls the trip reservation back too.
                assertFailsWith<Exception> {
                    dao.insertMileageDraft(requireNotNull(db.reportDao().get(winner.reportId)), line("other", winner.reportId, "other-trip"))
                }
                assertNull(dao.getBySourceTripId("other-trip"))
            } finally {
                db.close()
            }
        }

    private fun line(
        id: String,
        reportId: String,
        tripId: String,
    ) = ClaimLineEntity(id, reportId, "mileage", 12_000L, "INR", null, "", null, tripId, "{}", 0L)
}
