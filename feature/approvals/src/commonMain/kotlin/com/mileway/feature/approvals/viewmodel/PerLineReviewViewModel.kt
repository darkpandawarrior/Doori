package com.mileway.feature.approvals.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.claim.ApprovalReview
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.session.SessionSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Loads trip evidence before allowing a scoped manager decision on a mileage line. */
class PerLineReviewViewModel(
    private val reports: ReportRepository,
    private val session: SessionSource,
) : ViewModel() {
    data class State(
        val review: ApprovalReview? = null,
        val lineId: String? = null,
        val route: List<LocationData> = emptyList(),
        val loading: Boolean = true,
        val comment: String = "",
        val busy: Boolean = false,
        val error: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun open(
        reportId: String,
        lineId: String,
    ) {
        job?.cancel()
        mutableState.value = State(lineId = lineId)
        job =
            viewModelScope.launch {
                runCatching {
                    reports.observe(reportId).collect {
                        val review = requireNotNull(reports.review(reportId)) { "Report not found" }
                        val line = review.report.lines.single { it.id == lineId }
                        val route = if (line is MileageLine) reports.mileageRoute(line) else emptyList()
                        mutableState.update { it.copy(review = review, route = route, loading = false) }
                    }
                }.onFailure { failure ->
                    if (failure is CancellationException || failure !is Exception) throw failure
                    mutableState.update { it.copy(error = failure.message, loading = false) }
                }
            }
    }

    fun comment(value: String) {
        mutableState.update { it.copy(comment = value) }
    }

    fun act(
        action: ApprovalAction,
        onBehalfOf: String?,
    ) {
        val ui = state.value
        if (ui.busy || ui.loading || ui.comment.isBlank()) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                runCatching {
                    val review = requireNotNull(ui.review)
                    val actor = requireNotNull(session.sessionState.first().employeeCode) { "Sign in before reviewing" }
                    reports.reviewLine(review.report.id, review.report.recordVersion, actor, action, ui.comment, requireNotNull(ui.lineId), onBehalfOf)
                    mutableState.update { it.copy(comment = "") }
                }.onFailure { failure ->
                    if (failure is CancellationException || failure !is Exception) throw failure
                    mutableState.update { it.copy(error = failure.message ?: "Review failed") }
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
