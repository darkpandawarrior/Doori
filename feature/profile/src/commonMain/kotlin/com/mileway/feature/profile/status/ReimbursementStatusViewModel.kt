package com.mileway.feature.profile.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.session.SessionSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Observes only the signed-in employee's reports, clearing old rows on an account switch. */
class ReimbursementStatusViewModel(
    private val observeEmployee: (String) -> Flow<List<Report>>,
    private val session: SessionSource,
) : ViewModel() {
    constructor(reports: ReportRepository, session: SessionSource) : this(reports::observeByEmployee, session)

    data class State(
        val loading: Boolean = true,
        val reports: List<Report> = emptyList(),
        val error: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    init {
        observe()
    }

    // Room and session IO failures are surfaced without swallowing cancellation.
    @Suppress("TooGenericExceptionCaught")
    private fun observe() {
        viewModelScope.launch {
            session.sessionState.map { it.employeeCode }.distinctUntilChanged().collectLatest { employee ->
                mutableState.value = State()
                if (employee.isNullOrBlank()) {
                    mutableState.value = State(loading = false, error = "Sign in to view reimbursements")
                } else {
                    try {
                        observeEmployee(employee).collect { rows ->
                            mutableState.value = State(loading = false, reports = rows)
                        }
                    } catch (failure: Exception) {
                        if (failure is CancellationException) throw failure
                        mutableState.value = State(loading = false, error = failure.message ?: "Unable to load reimbursements")
                    }
                }
            }
        }
    }
}
