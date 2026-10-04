package com.mileway

import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.dao.ApprovalStepDao
import com.mileway.core.data.dao.ClaimLineDao
import com.mileway.core.data.dao.PerDiemRateDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.ClaimLineEntity
import com.mileway.core.data.model.db.PerDiemRateEntity
import com.mileway.core.data.model.db.ReportEntity
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.feature.logging.perdiem.PerDiemEntryViewModel
import com.mileway.feature.logging.report.LocalReportJourneyStore
import com.mileway.feature.logging.report.ReportSubmitViewModel
import com.siddharth.kmp.offlineoutbox.OpOutbox
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PerDiemEntryFlowTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rateRows = MutableStateFlow(listOf(rate("old", "2026-09-01", 10000), rate("new", "2026-09-03", 15000)))
    private val rateDao = mockk<PerDiemRateDao>()
    private val sessionRows = MutableStateFlow(SessionState(kind = SessionKind.GUEST, employeeCode = "employee"))
    private val session =
        object : SessionSource {
            override val sessionState = sessionRows
        }

    private fun rate(
        id: String,
        date: String,
        amount: Long,
    ) = PerDiemRateEntity(id, "Pune", "Standard", amount, "INR", LocalDate.parse(date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())

    private fun viewModel(store: com.mileway.feature.logging.report.ReportJourneyStore): PerDiemEntryViewModel {
        every { rateDao.observeAll() } returns rateRows
        return PerDiemEntryViewModel(rateDao, store, session, TimeZone.UTC)
    }

    @Test
    fun `generated dated lines round-trip through report write then shared submit path`() =
        runTest {
            val reportRows = MutableStateFlow<ReportEntity?>(null)
            val lineRows = MutableStateFlow<List<ClaimLineEntity>>(emptyList())
            val reportDao = mockk<ReportDao>()
            val lineDao = mockk<ClaimLineDao>()
            val approvalDao = mockk<ApprovalStepDao>()
            val outbox = mockk<OpOutbox>()
            coEvery { reportDao.get(any()) } answers { reportRows.value }
            every { reportDao.observe(any()) } returns reportRows
            every { reportDao.observeByEmployee(any()) } returns reportRows.map { listOfNotNull(it) }
            coEvery { reportDao.upsert(any()) } answers { reportRows.value = firstArg() }
            coEvery { lineDao.getByReport(any()) } answers { lineRows.value }
            every { lineDao.observeByReport(any()) } returns lineRows
            coEvery { lineDao.delete(any()) } answers { lineRows.value = lineRows.value.filterNot { it.id == firstArg<String>() } }
            coEvery { lineDao.upsert(any()) } answers {
                val row = firstArg<ClaimLineEntity>()
                lineRows.value = lineRows.value.filterNot { it.id == row.id } + row
            }
            coEvery { approvalDao.getByReport(any()) } returns emptyList()
            every { approvalDao.observeByReport(any()) } returns MutableStateFlow(emptyList())
            coEvery { outbox.enqueue(any(), any()) } returns "queued"
            val repository = ReportRepository(reportDao, lineDao, approvalDao, outbox, Json { ignoreUnknownKeys = true })
            val store = LocalReportJourneyStore(repository)
            val entry = viewModel(store)
            advanceUntilIdle()
            entry.dates("2026-08-31", "2026-09-03")
            assertEquals(1, entry.state.value.preview.skipped.size)
            entry.createReport()
            advanceUntilIdle()
            val id = assertNotNull(entry.state.value.createdReportId)
            val loaded = assertNotNull(repository.get(id))
            assertEquals(
                entry.state.value.preview.lines
                    .map { it.copy(id = "$id:${it.incurredOn}") },
                loaded.lines,
            )
            assertTrue(lineRows.value.all { it.type == "per_diem" })
            assertEquals(listOf(10000L, 10000L, 15000L), loaded.lines.map { it.amountMinor })
            assertEquals(listOf("2026-09-01", "2026-09-02", "2026-09-03"), loaded.lines.filterIsInstance<PerDiemLine>().map { it.incurredOn })
            val submit = ReportSubmitViewModel(store, session)
            submit.open(id)
            advanceUntilIdle()
            submit.submit()
            advanceUntilIdle()
            assertNull(submit.state.value.error)
            assertEquals(ReportLifecycleState.SUBMITTED, repository.get(id)?.state)
            coVerify(exactly = 2) { outbox.enqueue("report", any()) }
        }

    @Test
    fun `invalid dates missing rates and account switches cannot write reports`() =
        runTest {
            val store = mockk<com.mileway.feature.logging.report.ReportJourneyStore>()
            val entry = viewModel(store)
            advanceUntilIdle()
            entry.dates("bad", "2026-09-01")
            entry.createReport()
            advanceUntilIdle()
            assertEquals("Enter dates as YYYY-MM-DD", entry.state.value.error)
            entry.dates("2026-08-30", "2026-08-31")
            entry.createReport()
            advanceUntilIdle()
            assertEquals("No eligible days in this range", entry.state.value.error)
            entry.dates("2026-09-01", "2026-09-02")
            sessionRows.value = sessionRows.value.copy(employeeCode = "other")
            entry.createReport()
            advanceUntilIdle()
            assertEquals("Account changed; reload before creating a report", entry.state.value.error)
            coVerify(exactly = 0) { store.save(any<Report>()) }
        }

    @Test
    fun `changed rate snapshot requires preview review before saving`() =
        runTest {
            val store = mockk<com.mileway.feature.logging.report.ReportJourneyStore>()
            val entry = viewModel(store)
            advanceUntilIdle()
            entry.dates("2026-09-01", "2026-09-02")
            // The DAO read sees a new rate before its observation reaches the preview.
            every { rateDao.observeAll() } returns MutableStateFlow(listOf(rate("changed", "2026-09-01", 20000)))
            entry.createReport()
            advanceUntilIdle()
            assertEquals("Rates changed; review the updated preview", entry.state.value.error)
            coVerify(exactly = 0) { store.save(any<Report>()) }
        }
}
