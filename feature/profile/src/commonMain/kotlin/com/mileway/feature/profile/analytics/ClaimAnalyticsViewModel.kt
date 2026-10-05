package com.mileway.feature.profile.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.dao.PolicyViolationDao
import com.mileway.core.data.dao.ReportDao
import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.PolicyViolationEntity
import com.mileway.core.data.model.db.ReportEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val DAY_MILLIS = 86_400_000L

enum class ClaimAnalyticsView { SPEND, VIOLATIONS, CYCLE_TIME }

enum class ClaimCategory { EXPENSE, MILEAGE, PER_DIEM, ADVANCE }

data class ClaimAnalyticsFilter(
    val days: Int = 30,
    val status: ReportLifecycleState? = null,
    val category: ClaimCategory? = null,
)

data class ClaimAnalyticsSummary(
    val reportCount: Int = 0,
    val spendByCurrency: Map<String, Long> = emptyMap(),
    val violationsByCode: Map<String, Int> = emptyMap(),
    val spendByCurrencyAndType: Map<String, Map<ClaimCategory, Long>> = emptyMap(),
    val completedCyclesMillis: List<Long> = emptyList(),
)

/** Pure aggregation from persisted reports. Money stays separated by currency; flags are deduplicated. */
fun summarizeClaims(
    reports: List<Report>,
    rows: List<ReportEntity>,
    violations: List<PolicyViolationEntity>,
    filter: ClaimAnalyticsFilter,
    nowMillis: Long,
): ClaimAnalyticsSummary {
    val metadata = rows.associateBy { it.id }
    val filtered =
        reports.filter { report ->
            val created = metadata[report.id]?.createdAtMs
            created != null &&
                created in (nowMillis - filter.days * DAY_MILLIS)..nowMillis &&
                (filter.status == null || report.state == filter.status) &&
                report.lines.any { filter.category == null || it.category() == filter.category }
        }
    val spend = mutableMapOf<String, Long>()
    val spendTypes = mutableMapOf<String, MutableMap<ClaimCategory, Long>>()
    val flags = mutableSetOf<Triple<String, String?, String>>()
    val cycles = mutableListOf<Long>()
    filtered.forEach { report ->
        val lines = report.lines.filter { filter.category == null || it.category() == filter.category }
        lines.forEach { line ->
            spend[line.currency] = (spend[line.currency] ?: 0L) + line.amountMinor
            val types = spendTypes.getOrPut(line.currency) { mutableMapOf() }
            types[line.category()] = (types[line.category()] ?: 0L) + line.amountMinor
            line.policyFlags.forEach { flags.add(Triple(report.id, line.id, it)) }
        }
        violations
            .filter { it.reportId == report.id && (it.claimLineId == null || lines.any { line -> line.id == it.claimLineId }) }
            .forEach { flags.add(Triple(report.id, it.claimLineId, it.code)) }
        val row = metadata.getValue(report.id)
        val submitted = row.submittedAtMs
        if (report.state == ReportLifecycleState.PAID && submitted != null && row.updatedAtMs >= submitted) {
            cycles.add(row.updatedAtMs - submitted)
        }
    }
    return ClaimAnalyticsSummary(
        reportCount = filtered.size,
        spendByCurrency = spend.toList().sortedBy { it.first }.toMap(),
        violationsByCode = flags.groupingBy { it.third }.eachCount(),
        spendByCurrencyAndType = spendTypes,
        completedCyclesMillis = cycles,
    )
}

private fun ClaimLine.category(): ClaimCategory =
    when (this) {
        is ExpenseLine -> ClaimCategory.EXPENSE
        is MileageLine -> ClaimCategory.MILEAGE
        is PerDiemLine -> ClaimCategory.PER_DIEM
        is AdvanceLine -> ClaimCategory.ADVANCE
    }

/** Observes report metadata and flags so the dashboard shares the same persisted lifecycle. */
class ClaimAnalyticsViewModel(
    private val reports: ReportRepository,
    private val reportDao: ReportDao,
    private val violations: PolicyViolationDao,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    data class State(
        val filter: ClaimAnalyticsFilter = ClaimAnalyticsFilter(),
        val view: ClaimAnalyticsView = ClaimAnalyticsView.SPEND,
        val summary: ClaimAnalyticsSummary = ClaimAnalyticsSummary(),
        val loading: Boolean = true,
        val error: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var reportsSnapshot: List<Report> = emptyList()
    private var rowsSnapshot: List<ReportEntity> = emptyList()
    private var violationsSnapshot: List<PolicyViolationEntity> = emptyList()

    init {
        observe()
    }

    // Room and session IO failures are surfaced without swallowing cancellation.
    @Suppress("TooGenericExceptionCaught")
    private fun observe() {
        viewModelScope.launch {
            try {
                combine(reports.observeAll(), reportDao.observeAll(), violations.observeAll()) { claims, rows, flags ->
                    Triple(claims, rows, flags)
                }.collect { (claims, rows, flags) ->
                    reportsSnapshot = claims
                    rowsSnapshot = rows
                    violationsSnapshot = flags
                    refresh(state.value.filter)
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.value = state.value.copy(loading = false, error = failure.message ?: "Unable to load report analytics")
            }
        }
    }

    fun selectView(view: ClaimAnalyticsView) {
        mutableState.value = state.value.copy(view = view)
    }

    fun filter(filter: ClaimAnalyticsFilter) = refresh(filter)

    private fun refresh(filter: ClaimAnalyticsFilter) {
        mutableState.value =
            state.value.copy(
                filter = filter,
                summary = summarizeClaims(reportsSnapshot, rowsSnapshot, violationsSnapshot, filter, clock.now().toEpochMilliseconds()),
                loading = false,
                error = null,
            )
    }
}
