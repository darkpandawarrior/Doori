package com.mileway.feature.logging.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.session.SessionSource
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.mvi.errorState
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.repository.PERSISTED_DRAFT_RECORD_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Groups loose logged expenses by local trip date without moving already claimed lines. */
class ReportGroupingViewModel(
    private val expenses: ExpenseRepository,
    private val reports: ReportJourneyStore,
    private val session: SessionSource,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    data class Grouping(
        val loose: List<ExpenseRecord>,
        val suggestions: Map<String, List<ExpenseRecord>>,
        val reports: List<Report>,
        val employeeId: String,
        val selectedIds: Set<String> = emptySet(),
    )

    data class State(
        val screen: ScreenState<Grouping> = ScreenState.Loading,
        val busy: Boolean = false,
        val createdReportId: String? = null,
        val error: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        load()
    }

    fun load() {
        observation?.cancel()
        mutableState.value = State()
        observation =
            viewModelScope.launch {
                runCatching {
                    val employee = requireNotNull(session.sessionState.first().employeeCode) { "Sign in to group expenses" }
                    combine(expenses.recordsFlow, reports.observeByEmployee(employee)) { records, existing ->
                        val claimedIds = existing.flatMap { it.lines }.map { it.id }.toSet()
                        val loose =
                            records.filter {
                                it.id !in claimedIds &&
                                    it.id != PERSISTED_DRAFT_RECORD_ID &&
                                    it.status in setOf(ExpenseStatus.DRAFT, ExpenseStatus.PENDING) &&
                                    it.amountRupees.isFinite() &&
                                    it.amountRupees > 0
                            }
                        Grouping(
                            loose = loose,
                            suggestions =
                                loose.groupBy {
                                    Instant
                                        .fromEpochMilliseconds(it.dateMs)
                                        .toLocalDateTime(timeZone)
                                        .date
                                        .toString()
                                },
                            reports = existing,
                            employeeId = employee,
                        )
                    }.collect { grouping ->
                        mutableState.update { current ->
                            val selected =
                                current.screen.dataOrNull
                                    ?.selectedIds
                                    .orEmpty()
                                    .intersect(grouping.loose.map { it.id }.toSet())
                            current.copy(
                                screen =
                                    if (grouping.loose.isEmpty() && grouping.reports.isEmpty()) {
                                        ScreenState.Empty
                                    } else {
                                        ScreenState.Content(grouping.copy(selectedIds = selected))
                                    },
                            )
                        }
                    }
                }.onFailure { failure ->
                    rethrowCancellation(failure)
                    mutableState.update { it.copy(screen = errorState(failure.message ?: "Unable to load expenses")) }
                }
            }
    }

    fun toggle(id: String) {
        if (state.value.busy) return
        val grouping = state.value.screen.dataOrNull ?: return
        if (grouping.loose.none { it.id == id }) return
        val selected = if (id in grouping.selectedIds) grouping.selectedIds - id else grouping.selectedIds + id
        mutableState.update { it.copy(screen = ScreenState.Content(grouping.copy(selectedIds = selected)), error = null) }
    }

    fun selectDate(date: String) {
        if (state.value.busy) return
        val grouping = state.value.screen.dataOrNull ?: return
        val ids =
            grouping.suggestions[date]
                .orEmpty()
                .map { it.id }
                .toSet()
        mutableState.update { it.copy(screen = ScreenState.Content(grouping.copy(selectedIds = ids)), error = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun createReport() {
        if (state.value.busy) return
        val grouping = state.value.screen.dataOrNull ?: return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                runCatching {
                    val employee = requireNotNull(session.sessionState.first().employeeCode) { "Sign in to create a report" }
                    require(employee == grouping.employeeId) { "Account changed; reload before grouping expenses" }
                    val selected = grouping.loose.filter { it.id in grouping.selectedIds }
                    require(selected.size >= MinimumGroupedExpenses) { "Select at least two expenses" }
                    val claimed =
                        reports
                            .observeByEmployee(employee)
                            .first()
                            .flatMap { it.lines }
                            .map { it.id }
                            .toSet()
                    require(selected.none { it.id in claimed }) { "An expense was already grouped; review the list" }
                    val saved = reports.save(Report(id = Uuid.random().toString(), employeeId = employee, lines = selected.map { it.toClaimLine() }))
                    mutableState.update { it.copy(createdReportId = saved.id) }
                }.onFailure { failure ->
                    rethrowCancellation(failure)
                    mutableState.update { it.copy(error = failure.message ?: "Unable to group expenses; retry") }
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun consumeCreatedReport() {
        mutableState.update { it.copy(createdReportId = null) }
    }
}

internal const val MinimumGroupedExpenses = 2

private fun ExpenseRecord.toClaimLine(): ExpenseLine {
    val minor = amountRupees * MinorPerRupee
    require(minor.isFinite() && minor > 0 && minor < Long.MAX_VALUE.toDouble() && minor.roundToLong() > 0) { "Expense amount is invalid" }
    // Legacy records store the settled INR amount; currencyCode describes the original capture only.
    return ExpenseLine(id = id, amountMinor = minor.roundToLong(), currency = "INR", merchant = merchantName, category = category.name)
}
