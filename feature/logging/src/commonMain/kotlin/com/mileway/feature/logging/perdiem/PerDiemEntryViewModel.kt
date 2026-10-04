package com.mileway.feature.logging.perdiem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.PerDiemRateDao
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.policy.PerDiemRate
import com.mileway.core.data.domain.policy.PerDiemRateTable
import com.mileway.core.data.model.db.PerDiemRateEntity
import com.mileway.core.data.session.SessionSource
import com.mileway.feature.logging.report.ReportJourneyStore
import com.mileway.feature.logging.report.rethrowCancellation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Captures per-diem days from persisted rate cards into the existing report write path. */
class PerDiemEntryViewModel(
    private val rateDao: PerDiemRateDao,
    private val reports: ReportJourneyStore,
    private val session: SessionSource,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    data class RateCard(
        val region: String,
        val grade: String,
        val currency: String,
    )

    data class State(
        val loading: Boolean = true,
        val employeeId: String? = null,
        val cards: List<RateCard> = emptyList(),
        val selected: RateCard? = null,
        val start: String = "",
        val end: String = "",
        val preview: BatchDayRangeGenerator.Result = BatchDayRangeGenerator.Result(),
        val busy: Boolean = false,
        val createdReportId: String? = null,
        val error: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var rows: List<PerDiemRateEntity> = emptyList()
    private var observation: Job? = null

    init {
        load()
    }

    fun load() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, error = null) }
        observation =
            viewModelScope.launch {
                runCatching {
                    val employee = requireNotNull(session.sessionState.first().employeeCode) { "Sign in to enter per diem" }
                    mutableState.update { it.copy(employeeId = employee) }
                    rateDao.observeAll().collect { rates ->
                        rows = rates
                        val cards = rates.map { RateCard(it.region, it.grade, it.currency) }.distinct()
                        mutableState.update { current ->
                            val selected = current.selected?.takeIf { it in cards } ?: cards.firstOrNull()
                            current.copy(loading = false, cards = cards, selected = selected)
                        }
                        refreshPreview()
                    }
                }.onFailure { failure ->
                    rethrowCancellation(failure)
                    mutableState.update { it.copy(loading = false, error = failure.message ?: "Unable to load per-diem rates") }
                }
            }
    }

    fun select(card: RateCard) {
        if (state.value.busy || card !in state.value.cards) return
        mutableState.update { it.copy(selected = card, error = null) }
        refreshPreview()
    }

    fun dates(
        start: String,
        end: String,
    ) {
        if (state.value.busy) return
        mutableState.update { it.copy(start = start, end = end, error = null) }
        refreshPreview()
    }

    private fun generate(
        current: State,
        rates: List<PerDiemRateEntity>,
        batchId: String,
    ): BatchDayRangeGenerator.Result {
        val card = current.selected ?: return BatchDayRangeGenerator.Result(error = "No per-diem rate cards available")
        val start = runCatching { LocalDate.parse(current.start) }.getOrNull()
        val end = runCatching { LocalDate.parse(current.end) }.getOrNull()
        if (start == null || end == null) return BatchDayRangeGenerator.Result(error = "Enter dates as YYYY-MM-DD")
        val table =
            PerDiemRateTable(
                rates
                    .filter { it.region == card.region && it.grade == card.grade && it.currency == card.currency }
                    .map { PerDiemRate(it.effectiveFromMs, it.dailyRateMinor) },
            )
        return BatchDayRangeGenerator.generate(start, end, table, card.currency, timeZone, batchId)
    }

    private fun refreshPreview() {
        mutableState.update { it.copy(preview = generate(it, rows, "preview")) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun createReport() {
        val current = state.value
        if (current.busy || current.loading || current.createdReportId != null) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                runCatching {
                    val employee = requireNotNull(session.sessionState.first().employeeCode) { "Sign in to create a report" }
                    require(employee == current.employeeId) { "Account changed; reload before creating a report" }
                    val id = Uuid.random().toString()
                    val generated = generate(current, rateDao.observeAll().first(), id)
                    require(generated.error == null) { generated.error.orEmpty() }
                    require(generated.lines.isNotEmpty()) { "No eligible days in this range" }
                    require(
                        generated.lines == current.preview.lines.map { it.copy(id = "$id:${it.incurredOn}") } &&
                            generated.skipped == current.preview.skipped,
                    ) { "Rates changed; review the updated preview" }
                    val saved = reports.save(Report(id, employee, generated.lines))
                    mutableState.update { it.copy(createdReportId = saved.id) }
                }.onFailure { failure ->
                    rethrowCancellation(failure)
                    mutableState.update { it.copy(error = failure.message ?: "Unable to create per-diem report") }
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
