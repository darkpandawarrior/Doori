package com.mileway.feature.logging.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.session.SessionSource
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.core.ui.mvi.errorState
import com.mileway.feature.logging.model.ExpenseCategory
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
        val editingReport: Report? = null,
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
    private var editingId: String? = null

    init {
        load()
    }

    fun load(reportId: String? = editingId) {
        editingId = reportId
        observation?.cancel()
        mutableState.value = State()
        observation =
            viewModelScope.launch {
                runCatching {
                    val employee = requireNotNull(session.sessionState.first().employeeCode) { "Sign in to group expenses" }
                    combine(expenses.recordsFlow, reports.observeByEmployee(employee)) { records, existing ->
                        buildGrouping(records, existing, employee, reportId)
                    }.collect { grouping ->
                        mutableState.update { current ->
                            val selected =
                                current.screen.dataOrNull
                                    ?.selectedIds
                                    ?: grouping.editingReport
                                        ?.lines
                                        ?.map { it.id }
                                        ?.toSet()
                                        .orEmpty()
                            val availableSelection = selected.intersect(grouping.loose.map { it.id }.toSet())
                            current.copy(
                                screen =
                                    if (grouping.loose.isEmpty() && grouping.reports.isEmpty()) {
                                        ScreenState.Empty
                                    } else {
                                        ScreenState.Content(grouping.copy(selectedIds = availableSelection))
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

    private fun buildGrouping(
        records: List<ExpenseRecord>,
        existing: List<Report>,
        employee: String,
        reportId: String?,
    ): Grouping {
        val editing = reportId?.let { id -> requireNotNull(existing.find { it.id == id }) { "Report not found" } }
        require(editing == null || (editing.isEditable && editing.lines.all { it is ExpenseLine })) { "Only editable expense reports can be regrouped" }
        val claimedIds = existing.flatMap { it.lines }.map { it.id }.toSet()
        val loose =
            records.filter {
                it.id !in claimedIds &&
                    it.id != PERSISTED_DRAFT_RECORD_ID &&
                    it.status in setOf(ExpenseStatus.DRAFT, ExpenseStatus.PENDING) &&
                    it.amountRupees.isFinite() &&
                    it.amountRupees > 0
            }
        val editedItems =
            editing?.lines.orEmpty().filterIsInstance<ExpenseLine>().map { line ->
                ExpenseRecord(
                    id = line.id,
                    category = ExpenseCategory.entries.find { it.name == line.category } ?: ExpenseCategory.OTHER,
                    merchantName = line.merchant,
                    amountRupees = line.amountMinor.toDouble() / MinorPerRupee,
                    status = ExpenseStatus.DRAFT,
                    dateMs = records.find { it.id == line.id }?.dateMs ?: 0,
                    currencyCode = line.currency,
                    fxRate = line.fxRate,
                    fxRatePinnedAt = line.fxRatePinnedAt,
                    amountMinor = line.amountMinor,
                    splits = line.splits,
                    attendees = line.attendees,
                    itemized = line.itemized,
                    cardMatchId = line.cardMatchId,
                )
            }
        val available = editedItems + loose
        return Grouping(
            loose = available,
            suggestions =
                available.groupBy { record ->
                    if (record.id in editedItems.map { it.id } && records.none { it.id == record.id }) {
                        "Current report (capture date unavailable)"
                    } else {
                        Instant
                            .fromEpochMilliseconds(record.dateMs)
                            .toLocalDateTime(timeZone)
                            .date
                            .toString()
                    }
                },
            reports = existing,
            employeeId = employee,
            editingReport = editing,
        )
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
                    val existing = reports.observeByEmployee(employee).first()
                    val editing = grouping.editingReport
                    if (editing != null) {
                        val fresh = requireNotNull(existing.find { it.id == editing.id }) { "Report was removed; reload" }
                        require(fresh.isEditable && fresh.recordVersion == editing.recordVersion) { "Report changed; reload before editing" }
                    }
                    val claimed =
                        existing
                            .filterNot { it.id == editing?.id }
                            .flatMap { it.lines }
                            .map { it.id }
                            .toSet()
                    require(selected.none { it.id in claimed }) { "An expense was already grouped; review the list" }
                    val lines = selected.map { record -> editing?.lines?.find { it.id == record.id } ?: record.toClaimLine() }
                    val draft = editing?.copy(lines = lines) ?: Report(id = Uuid.random().toString(), employeeId = employee, lines = lines)
                    val saved = reports.save(draft)
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

internal fun ExpenseRecord.toClaimLine(): ExpenseLine {
    val capturedMinor =
        amountMinor ?: run {
            val minor = amountRupees * MinorPerRupee
            require(minor.isFinite() && minor > 0 && minor < Long.MAX_VALUE.toDouble()) { "Expense amount is invalid" }
            minor.roundToLong()
        }
    require(capturedMinor > 0) { "Expense amount is invalid" }
    return ExpenseLine(
        id = id,
        amountMinor = capturedMinor,
        currency = currencyCode,
        incurredOn =
            Instant
                .fromEpochMilliseconds(dateMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString(),
        fxRate = fxRate,
        fxRatePinnedAt = fxRatePinnedAt,
        merchant = merchantName,
        category = category.name,
        receiptImagePath = receiptImagePath,
        splits = splits,
        attendees = attendees,
        itemized = itemized,
        cardMatchId = cardMatchId,
    )
}
