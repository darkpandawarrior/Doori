package com.mileway.feature.logging.report

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ApprovalChain
import com.mileway.core.data.domain.claim.ApprovalStep
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.JustificationReason
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
import com.mileway.feature.advances.di.advancesModule
import com.mileway.feature.advances.reconcile.AdvanceReconciliationUseCase
import com.mileway.feature.logging.justification.JustificationReasonSuggester
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.siddharth.kmp.result.Result
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
import org.koin.dsl.koinApplication
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
        Report("report", "employee", amounts.mapIndexed { index, amount -> ExpenseLine("line-$index", amount, "INR", merchant = "Cafe", category = "FOOD", receiptImagePath = "receipt-$index") })

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
            vm.saveExceptionDetails("new-2", true, "Receipt was lost", JustificationReason.BUSINESS_NECESSITY, "")
            advanceUntilIdle()
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
                ReportLifecycleState.RECALLED,
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

    @Test
    fun appliedAdvanceReviewRecomputesWhenAnotherReportReservesIt() =
        runTest(dispatcher) {
            val graph = koinApplication { modules(advancesModule) }
            try {
                val current = report(listOf(600_000)).let { it.copy(lines = it.lines + AdvanceLine("advance", 500_000, "INR", advanceId = "1")) }
                val store = MemoryReports(listOf(current))
                val vm = ReportSubmitViewModel(store, session, policy(max = 1_000_000), clock, graph.koin.get<AdvanceReconciliationUseCase>())
                vm.open("report")
                advanceUntilIdle()
                assertEquals(
                    100_000L,
                    vm.state.value.screen.dataOrNull
                        ?.reconciliation
                        ?.netMinor,
                )
                store.save(Report("other", "employee", listOf(AdvanceLine("other-advance", 500_000, "INR", advanceId = "1", reconciled = true))))
                advanceUntilIdle()
                val result =
                    assertNotNull(
                        vm.state.value.screen.dataOrNull
                            ?.reconciliation,
                    )
                assertEquals(0L, result.appliedMinor)
                assertEquals(600_000L, result.netMinor)
                assertTrue(
                    result.excluded
                        .single()
                        .reason
                        .contains("another report"),
                )
            } finally {
                graph.close()
            }
        }

    @Test
    fun everyMissingRequiredReceiptNeedsConsentAndNoteThenSubmitBlockClears() = runTest(dispatcher) {
        val categoryRequired = ExpenseLine("category", 500, "INR", merchant = "Taxi", category = "TRAVEL")
        val amountRequired = categoryRequired.copy(id = "amount", category = "FOOD", amountMinor = 2_000)
        val store = MemoryReports(listOf(Report("report", "employee", listOf(categoryRequired, amountRequired))))
        val vm = ReportSubmitViewModel(store, session, policy(), clock)
        vm.open("report")
        advanceUntilIdle()
        assertEquals(setOf("category", "amount"), vm.state.value.screen.dataOrNull!!.requiredAffidavitIds)
        vm.acceptWarnings(true)
        vm.submit()
        advanceUntilIdle()
        assertEquals(ReportLifecycleState.DRAFT, store.rows.value.single().state)
        vm.saveExceptionDetails("category", false, "Lost", null, "")
        advanceUntilIdle()
        assertEquals(2, vm.state.value.screen.dataOrNull!!.hardFlags.size)
        vm.saveExceptionDetails("category", true, " ", null, "")
        advanceUntilIdle()
        assertEquals(2, vm.state.value.screen.dataOrNull!!.hardFlags.size)
        vm.saveExceptionDetails("category", true, "Lost", JustificationReason.OTHER, "Client visit")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.screen.dataOrNull!!.hardFlags.size)
        vm.saveExceptionDetails("amount", true, "Vendor gave no receipt", JustificationReason.CLIENT_REQUEST, "")
        advanceUntilIdle()
        vm.acceptWarnings(true)
        assertTrue(vm.state.value.screen.dataOrNull!!.canSubmit)
        vm.submit()
        advanceUntilIdle()
        assertEquals(ReportLifecycleState.SUBMITTED, store.rows.value.single().state)
        assertTrue(store.rows.value.single().lines.filterIsInstance<ExpenseLine>().all { it.affidavitAccepted })
    }

    @Test
    fun receiptOptionalAndAttachedLinesNeedNoAffidavitButOtherStillNeedsText() = runTest(dispatcher) {
        val optional = ExpenseLine("optional", 500, "INR", merchant = "Cafe", category = "FOOD")
        val attached = optional.copy(id = "attached", category = "TRAVEL", amountMinor = 2_000, receiptImagePath = "receipt.jpg")
        val store = MemoryReports(listOf(Report("report", "employee", listOf(optional, attached))))
        val vm = ReportSubmitViewModel(store, session, policy(), clock)
        vm.open("report")
        advanceUntilIdle()
        assertTrue(vm.state.value.screen.dataOrNull!!.requiredAffidavitIds.isEmpty())
        vm.saveExceptionDetails("optional", false, "", JustificationReason.OTHER, " ")
        advanceUntilIdle()
        assertEquals(listOf("JUSTIFICATION_REQUIRED"), vm.state.value.screen.dataOrNull!!.hardFlags.map { it.code })
        vm.saveExceptionDetails("optional", false, "", JustificationReason.OTHER, "Client visit")
        advanceUntilIdle()
        vm.acceptWarnings(true)
        assertTrue(vm.state.value.screen.dataOrNull!!.canSubmit)
        vm.markExceptionChanged("optional")
        vm.submit()
        advanceUntilIdle()
        assertEquals(ReportLifecycleState.DRAFT, store.rows.value.single().state)
    }

    @Test
    fun suggestionIsOnlyAHintAndCanBeOverriddenWithoutWeakeningHardPolicy() = runTest(dispatcher) {
        val line = ExpenseLine("line", 60_000, "INR", merchant = "Private merchant", category = "FOOD", affidavitNote = "Private note")
        val store = MemoryReports(listOf(Report("report", "employee", listOf(line))))
        var calls = 0
        val suggester = JustificationReasonSuggester({ "fake-key" }) { calls++; Result.Success("client_request") }
        val vm = ReportSubmitViewModel(store, session, policy(), clock, reasonSuggester = suggester)
        vm.open("report")
        advanceUntilIdle()
        vm.suggestReasons()
        vm.suggestReasons()
        advanceUntilIdle()
        assertEquals(1, calls)
        assertEquals(JustificationReason.CLIENT_REQUEST, vm.state.value.suggestions["line"])
        assertNull((vm.state.value.screen.dataOrNull!!.report.lines.single() as ExpenseLine).justificationReason)
        vm.saveExceptionDetails("line", true, "Receipt lost", JustificationReason.OTHER, "Urgent business visit")
        advanceUntilIdle()
        vm.acceptWarnings(true)
        assertEquals(listOf("EXPENSE_OVER_MAX"), vm.state.value.screen.dataOrNull!!.hardFlags.map { it.code })
        assertFalse(vm.state.value.screen.dataOrNull!!.canSubmit)
        assertEquals(JustificationReason.OTHER, (store.rows.value.single().lines.single() as ExpenseLine).justificationReason)
    }

    @Test
    fun capturedReceiptFlagAlsoRequiresAffidavitWhenCurrentPolicyDoesNotFlagIt() = runTest(dispatcher) {
        val line = ExpenseLine("line", 500, "INR", merchant = "Cafe", category = "FOOD", policyFlags = listOf("RECEIPT_RECOMMENDED"))
        val store = MemoryReports(listOf(Report("report", "employee", listOf(line))))
        val vm = ReportSubmitViewModel(store, session, policy(), clock)
        vm.open("report")
        advanceUntilIdle()
        assertEquals(setOf("line"), vm.state.value.screen.dataOrNull!!.requiredAffidavitIds)
    }

    @Test
    fun originalCaptureReceiptUpdatesReevaluateOlderReportJson() = runTest(dispatcher) {
        val record = ExpenseRecord("receipt", ExpenseCategory.TRAVEL, "Taxi", 5.0, ExpenseStatus.PENDING, 1_000, receiptImagePath = "receipt.jpg")
        assertEquals("receipt.jpg", record.toClaimLine().receiptImagePath)
        val line = record.toClaimLine().copy(receiptImagePath = null)
        val expenses = ExpenseRepository()
        expenses.insert(record)
        val store = MemoryReports(listOf(Report("report", "employee", listOf(line))))
        val vm = ReportSubmitViewModel(store, session, policy(), clock, expenses = expenses)
        vm.open("report")
        advanceUntilIdle()
        assertTrue(vm.state.value.screen.dataOrNull!!.requiredAffidavitIds.isEmpty())
        expenses.update(record.copy(receiptImagePath = null))
        advanceUntilIdle()
        assertEquals(setOf("receipt"), vm.state.value.screen.dataOrNull!!.requiredAffidavitIds)
    }

    @Test
    fun absentKeyAndGarbageSuggestionLeaveNoErrorOrSelectedReason() = runTest(dispatcher) {
        val line = ExpenseLine("line", 2_000, "INR", merchant = "Cafe", category = "FOOD")
        val suggesters = listOf(
            JustificationReasonSuggester({ null }) { error("Must not call a provider") },
            JustificationReasonSuggester({ "fake-key" }) { Result.Success("garbage") },
        )
        for (suggester in suggesters) {
            val store = MemoryReports(listOf(Report("report", "employee", listOf(line))))
            val vm = ReportSubmitViewModel(store, session, policy(), clock, reasonSuggester = suggester)
            vm.open("report")
            advanceUntilIdle()
            vm.suggestReasons()
            advanceUntilIdle()
            assertTrue(vm.state.value.suggestions.isEmpty())
            assertNull(vm.state.value.error)
            assertNull((vm.state.value.screen.dataOrNull!!.report.lines.single() as ExpenseLine).justificationReason)
        }
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
