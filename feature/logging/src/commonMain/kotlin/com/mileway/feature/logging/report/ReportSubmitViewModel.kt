package com.mileway.feature.logging.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.ReportLifecycleStateMachine
import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.data.domain.policy.PolicySeverity
import com.mileway.core.data.domain.policy.PolicyVersion
import com.mileway.core.data.domain.policy.PolicyViolation
import com.mileway.core.data.ledger.PolicyRateTable
import com.mileway.core.data.session.SessionSource
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.mvi.errorState
import com.mileway.stub.PolicyMockData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Narrow persistence seam; the Room adapter retains atomic lifecycle notifications from L5. */
interface ReportJourneyStore {
    fun observe(id: String): Flow<Report?>

    fun observeByEmployee(employeeId: String): Flow<List<Report>>

    suspend fun save(report: Report): Report

    suspend fun recall(id: String): Report
}

class LocalReportJourneyStore(
    private val reports: ReportRepository,
) : ReportJourneyStore {
    override fun observe(id: String) = reports.observe(id)

    override fun observeByEmployee(employeeId: String) = reports.observeByEmployee(employeeId)

    override suspend fun save(report: Report) = reports.save(report)

    override suspend fun recall(id: String) = reports.transition(id, ReportLifecycleEvent.RECALL)
}

/** Local demo limits, shared with existing expense capture. No backend switch is changed. */
fun expenseReportPolicy(): PolicyEngine =
    PolicyEngine(
        listOf(
            PolicyVersion(
                effectiveFrom = 0L,
                rateTable = PolicyRateTable(emptyMap()),
                maxExpenseAmountMinor = (PolicyMockData.EXPENSE_HARD_STOP_RUPEES * MinorPerRupee).toLong(),
                receiptRequiredAboveMinor = ReceiptWarningMinor,
            ),
        ),
    )

internal val Report.isEditable: Boolean
    get() = state == ReportLifecycleState.DRAFT || state == ReportLifecycleState.RECALLED

internal const val MinorPerRupee = 100
private const val ReceiptWarningMinor = 100_000L

/** Policy is evaluated over all report lines; a hard flag anywhere blocks the whole report. */
class ReportSubmitViewModel(
    private val reports: ReportJourneyStore,
    private val session: SessionSource,
    private val policy: PolicyEngine = expenseReportPolicy(),
    private val clock: Clock = Clock.System,
) : ViewModel() {
    data class Review(
        val report: Report,
        val hardFlags: List<PolicyViolation>,
        val softFlags: List<PolicyViolation>,
        val warningsAccepted: Boolean = false,
    ) {
        val canSubmit: Boolean
            get() = report.isEditable && hardFlags.isEmpty() && (softFlags.isEmpty() || warningsAccepted)
        val canRecall: Boolean
            get() = report.state == ReportLifecycleState.SUBMITTED && report.approvalChain.steps.isEmpty()
    }

    data class State(
        val screen: ScreenState<Review> = ScreenState.Loading,
        val busy: Boolean = false,
        val error: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var openedId: String? = null

    fun open(id: String) {
        if (openedId == id && observation?.isActive == true) return
        openedId = id
        observation?.cancel()
        mutableState.value = State()
        observation =
            viewModelScope.launch {
                runCatching {
                    val employee = requireNotNull(session.sessionState.first().employeeCode) { "Sign in to review reports" }
                    reports.observe(id).collect { report ->
                        require(report == null || report.employeeId == employee) { "This report belongs to another employee" }
                        mutableState.update { current ->
                            val review = report?.let { review(it) }
                            current.copy(screen = review?.let { ScreenState.Content(it) } ?: ScreenState.Empty)
                        }
                    }
                }.onFailure { failure ->
                    rethrowCancellation(failure)
                    mutableState.update { it.copy(screen = errorState(failure.message ?: "Unable to load report")) }
                }
            }
    }

    fun retry() {
        openedId?.let { id ->
            observation?.cancel()
            open(id)
        }
    }

    fun acceptWarnings(accepted: Boolean) {
        val review = state.value.screen.dataOrNull ?: return
        mutableState.update { it.copy(screen = ScreenState.Content(review.copy(warningsAccepted = accepted)), error = null) }
    }

    fun submit() =
        perform { previous ->
            val atMillis = clock.now().toEpochMilliseconds()
            val fresh = review(previous.report, atMillis).copy(warningsAccepted = previous.warningsAccepted)
            mutableState.update { it.copy(screen = ScreenState.Content(fresh)) }
            require(fresh.canSubmit) { "Resolve hard policy flags and acknowledge warnings before submitting" }
            val flags = policy.evaluate(fresh.report.lines, atMillis)
            val evaluated =
                fresh.report.copy(
                    lines =
                        fresh.report.lines.map { line ->
                            // Journey A groups expenses only; other claim kinds retain their existing flags.
                            if (line is ExpenseLine) line.copy(policyFlags = flags[line.id].orEmpty().map { it.code }) else line
                        },
                    state = ReportLifecycleStateMachine.transition(fresh.report.state, ReportLifecycleEvent.SUBMIT),
                )
            reports.save(evaluated)
        }

    fun recall() =
        perform { review ->
            require(review.canRecall) { "A report with an approval action cannot be recalled" }
            reports.recall(review.report.id)
        }

    private fun review(
        report: Report,
        atMillis: Long = clock.now().toEpochMilliseconds(),
    ): Review {
        val flags =
            policy
                .evaluate(report.lines, atMillis)
                .values
                .flatten()
                .toMutableList()
        if (report.lines.isEmpty() ||
            report.lines.any { it.amountMinor <= 0 } ||
            report.lines
                .map { it.currency }
                .distinct()
                .size != 1
        ) {
            flags += PolicyViolation("INVALID_REPORT", PolicySeverity.HARD_BLOCK, "Use positive amounts in one currency")
        }
        var total = 0L
        for (line in report.lines) {
            if (line.amountMinor < 0 || total > Long.MAX_VALUE - line.amountMinor) {
                flags += PolicyViolation("INVALID_TOTAL", PolicySeverity.HARD_BLOCK, "Report total exceeds the supported amount")
                break
            }
            total += line.amountMinor
        }
        return Review(
            report = report,
            hardFlags = flags.filter { it.severity == PolicySeverity.HARD_BLOCK },
            softFlags = flags.filter { it.severity == PolicySeverity.SOFT_WARN },
        )
    }

    private fun perform(action: suspend (Review) -> Report) {
        if (state.value.busy) return
        val previous = state.value.screen.dataOrNull ?: return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                runCatching {
                    require(session.sessionState.first().employeeCode == previous.report.employeeId) { "Sign in as the report owner" }
                    val saved = action(previous)
                    mutableState.update { it.copy(screen = ScreenState.Content(review(saved))) }
                }.onFailure { failure ->
                    rethrowCancellation(failure)
                    mutableState.update { it.copy(error = failure.message ?: "Report action failed; retry") }
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}

internal fun rethrowCancellation(failure: Throwable) {
    if (failure is CancellationException || failure !is Exception) throw failure
}
