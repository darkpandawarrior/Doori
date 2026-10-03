package com.mileway.feature.logging.report

import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.ReportLifecycleStateMachine
import com.mileway.core.data.domain.notify.ReportLifecycleNotifier
import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.data.domain.policy.PolicyVersion
import com.mileway.core.data.ledger.PolicyRateTable
import com.mileway.core.data.model.db.NotificationEntity
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReportSubmitViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session =
        object : SessionSource {
            override val sessionState = MutableStateFlow(SessionState(kind = SessionKind.GUEST, employeeCode = "employee"))
        }
    private val clock =
        object : Clock {
            override fun now() = Instant.fromEpochMilliseconds(1_000)
        }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun policy(
        max: Long = 50_000,
        receipt: Long = 1_000,
    ) = PolicyEngine(listOf(PolicyVersion(0, PolicyRateTable(emptyMap()), max, receipt)))

    private fun report(amounts: List<Long> = listOf(500, 2_000)) =
        Report("report", "employee", amounts.mapIndexed { index, amount -> ExpenseLine("line-$index", amount, "INR", merchant = "Cafe", category = "FOOD") })

    @Test
    fun `group two logged expenses by date then submit and recall with inbox rows`() =
        runTest(dispatcher) {
            val expenses = ExpenseRepository()
            val one = ExpenseRecord("new-1", ExpenseCategory.FOOD, "Cafe", 5.0, ExpenseStatus.PENDING, 1_000)
            val two = one.copy(id = "new-2", merchantName = "Taxi", amountRupees = 20.0, dateMs = 2_000)
            expenses.insert(one)
            expenses.insert(two)
            val store = MemoryReports()
            val grouping = ReportGroupingViewModel(expenses, store, session, TimeZone.UTC)
            advanceUntilIdle()
            grouping.selectDate("1970-01-01")
            assertEquals(
                setOf(one.id, two.id),
                grouping.state.value.screen.dataOrNull
                    ?.selectedIds,
            )
            grouping.createReport()
            advanceUntilIdle()
            val id = assertNotNull(grouping.state.value.createdReportId)
            assertEquals(
                listOf(500L, 2_000L),
                store.rows.value
                    .single()
                    .lines
                    .map { it.amountMinor },
            )
            assertTrue(
                grouping.state.value.screen.dataOrNull!!
                    .loose
                    .none { it.id in setOf(one.id, two.id) },
            )
            assertTrue(store.inbox.isEmpty()) // Creating a draft is not a lifecycle transition.

            val vm = ReportSubmitViewModel(store, session, policy(), clock)
            vm.open(id)
            advanceUntilIdle()
            assertFalse(
                vm.state.value.screen.dataOrNull!!
                    .canSubmit,
            )
            vm.submit()
            advanceUntilIdle()
            assertEquals(
                ReportLifecycleState.DRAFT,
                store.rows.value
                    .single()
                    .state,
            )
            vm.acceptWarnings(true)
            vm.submit()
            vm.submit() // Duplicate taps are gated before the coroutine starts.
            advanceUntilIdle()
            assertEquals(
                ReportLifecycleState.SUBMITTED,
                store.rows.value
                    .single()
                    .state,
            )
            assertEquals(
                listOf("RECEIPT_RECOMMENDED"),
                store.rows.value
                    .single()
                    .lines
                    .last()
                    .policyFlags,
            )
            assertEquals(1, store.inbox.size)
            assertEquals("Report Submitted", store.inbox.single().title)
            vm.recall()
            advanceUntilIdle()
            assertEquals(
                ReportLifecycleState.DRAFT,
                store.rows.value
                    .single()
                    .state,
            )
            assertEquals(2, store.inbox.size)
            assertEquals("Report Recalled", store.inbox.last().title)
            assertTrue(store.inbox.all { it.isUnread })
        }

    @Test
    fun `draft and recalled reports can be regrouped and submitted without losing saved lines`() =
        runTest(dispatcher) {
            for (editableState in listOf(ReportLifecycleState.DRAFT, ReportLifecycleState.RECALLED)) {
                val store = MemoryReports(listOf(report().copy(state = editableState)))
                val expenses = ExpenseRepository()
                val grouping = ReportGroupingViewModel(expenses, store, session, TimeZone.UTC)
                grouping.load("report")
                advanceUntilIdle()
                assertEquals(
                    setOf("line-0", "line-1"),
                    grouping.state.value.screen.dataOrNull
                        ?.selectedIds,
                )
                grouping.toggle("line-0")
                grouping.toggle("EXP-004")
                grouping.createReport()
                advanceUntilIdle()
                assertEquals(
                    editableState,
                    store.rows.value
                        .single()
                        .state,
                )
                assertEquals(
                    setOf("line-1", "EXP-004"),
                    store.rows.value
                        .single()
                        .lines
                        .map { it.id }
                        .toSet(),
                )
                assertTrue(store.inbox.isEmpty())
                val vm = ReportSubmitViewModel(store, session, policy(), clock)
                vm.open("report")
                advanceUntilIdle()
                vm.acceptWarnings(true)
                vm.submit()
                advanceUntilIdle()
                assertEquals(
                    ReportLifecycleState.SUBMITTED,
                    store.rows.value
                        .single()
                        .state,
                )
                assertEquals(1, store.inbox.size)
            }
        }

    @Test
    fun `hard flag on any line blocks the entire report even with warnings accepted`() =
        runTest(dispatcher) {
            val store = MemoryReports(listOf(report(listOf(500, 60_000))))
            val vm = ReportSubmitViewModel(store, session, policy(), clock)
            vm.open("report")
            advanceUntilIdle()
            vm.acceptWarnings(true)
            val review = assertNotNull(vm.state.value.screen.dataOrNull)
            assertEquals(1, review.hardFlags.size)
            assertEquals(1, review.softFlags.size)
            vm.submit()
            advanceUntilIdle()
            assertEquals(
                ReportLifecycleState.DRAFT,
                store.rows.value
                    .single()
                    .state,
            )
            assertNotNull(vm.state.value.error)
            assertTrue(store.inbox.isEmpty())
        }

    @Test
    fun `submission rechecks the policy version in effect at submission time`() =
        runTest(dispatcher) {
            var now = 1_000L
            val datedClock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(now)
                }
            val datedPolicy =
                PolicyEngine(listOf(PolicyVersion(0, PolicyRateTable(emptyMap()), 50_000), PolicyVersion(2_000, PolicyRateTable(emptyMap()), 1_000)))
            val store = MemoryReports(listOf(report()))
            val vm = ReportSubmitViewModel(store, session, datedPolicy, datedClock)
            vm.open("report")
            advanceUntilIdle()
            assertTrue(
                vm.state.value.screen.dataOrNull!!
                    .canSubmit,
            )
            now = 2_000L
            vm.submit()
            advanceUntilIdle()
            assertEquals(
                ReportLifecycleState.DRAFT,
                store.rows.value
                    .single()
                    .state,
            )
            assertTrue(store.inbox.isEmpty())
        }

    @Test
    fun `recall is blocked when an action appears after the review was loaded`() =
        runTest(dispatcher) {
            val submitted = report().copy(state = ReportLifecycleState.SUBMITTED)
            val store = MemoryReports(listOf(submitted))
            val vm = ReportSubmitViewModel(store, session, policy(), clock)
            vm.open("report")
            advanceUntilIdle()
            assertTrue(
                vm.state.value.screen.dataOrNull!!
                    .canRecall,
            )
            store.beforeRecall = {
                store.rows.value =
                    listOf(
                        submitted.copy(
                            approvalChain = ApprovalChain(listOf(ApprovalStep(0, actedBy = "manager", action = ApprovalAction.APPROVE, actedAtMillis = 1_000))),
                        ),
                    )
            }
            vm.recall()
            advanceUntilIdle()
            assertEquals(
                ReportLifecycleState.SUBMITTED,
                store.rows.value
                    .single()
                    .state,
            )
            assertFalse(
                vm.state.value.screen.dataOrNull!!
                    .canRecall,
            )
            assertNotNull(vm.state.value.error)
            assertTrue(store.inbox.isEmpty())
        }

    @Test
    fun `single expense cannot be grouped and missing report is empty`() =
        runTest(dispatcher) {
            val store = MemoryReports()
            val grouping = ReportGroupingViewModel(ExpenseRepository(), store, session, TimeZone.UTC)
            advanceUntilIdle()
            grouping.toggle("EXP-004")
            grouping.createReport()
            advanceUntilIdle()
            assertTrue(store.rows.value.isEmpty())
            assertNotNull(grouping.state.value.error)
            val vm = ReportSubmitViewModel(store, session, policy(), clock)
            vm.open("missing")
            advanceUntilIdle()
            assertEquals(ScreenState.Empty, vm.state.value.screen)
        }

    @Test
    fun `foreign owner and mixed currency reports cannot be submitted`() =
        runTest(dispatcher) {
            val foreign = MemoryReports(listOf(report().copy(employeeId = "another")))
            val vm = ReportSubmitViewModel(foreign, session, policy(), clock)
            vm.open("report")
            advanceUntilIdle()
            assertTrue(vm.state.value.screen is ScreenState.Error)
            vm.submit()
            advanceUntilIdle()
            assertTrue(foreign.inbox.isEmpty())
            val mixed = report().let { it.copy(lines = it.lines + ExpenseLine("usd", 100, "USD", merchant = "Cafe", category = "FOOD")) }
            val store = MemoryReports(listOf(mixed))
            val mixedVm = ReportSubmitViewModel(store, session, policy(), clock)
            mixedVm.open("report")
            advanceUntilIdle()
            assertTrue(
                mixedVm.state.value.screen.dataOrNull!!
                    .hardFlags
                    .any { it.code == "INVALID_REPORT" },
            )
            assertNull(mixedVm.state.value.error)
        }

    /** Fake local persistence uses the same transition/notifier contracts as the Room adapter. */
    private class MemoryReports(
        initial: List<Report> = emptyList(),
    ) : ReportJourneyStore {
        val rows = MutableStateFlow(initial)
        val inbox = mutableListOf<NotificationEntity>()
        var beforeRecall: (() -> Unit)? = null

        override fun observe(id: String) = rows.map { all -> all.find { it.id == id } }

        override fun observeByEmployee(employeeId: String) = rows.map { all -> all.filter { it.employeeId == employeeId } }

        override suspend fun save(report: Report): Report {
            val previous = rows.value.find { it.id == report.id }
            require(report.recordVersion == (previous?.recordVersion ?: 0)) { "Report changed; reload" }
            val saved = report.copy(recordVersion = report.recordVersion + 1)
            ReportLifecycleNotifier.map(previous?.state ?: ReportLifecycleState.DRAFT, saved, 1_000)?.let { row ->
                inbox += NotificationEntity(row.id, row.title, row.body, "Just now", true, row.type, createdAtMs = row.createdAtMs, deeplink = row.deeplink)
            }
            rows.value = rows.value.filterNot { it.id == report.id } + saved
            return saved
        }

        override suspend fun recall(id: String): Report {
            beforeRecall?.invoke()
            val report = rows.value.single { it.id == id }
            require(report.approvalChain.steps.isEmpty()) { "An approval action prevents recall" }
            return save(report.copy(state = ReportLifecycleStateMachine.transition(report.state, ReportLifecycleEvent.RECALL)))
        }
    }
}
