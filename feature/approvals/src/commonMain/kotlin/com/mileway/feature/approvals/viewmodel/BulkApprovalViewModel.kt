package com.mileway.feature.approvals.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.claim.ApprovalReview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Selects only eligible persisted reports; each write rechecks policy and recordVersion. */
class BulkApprovalViewModel(
    queue: Flow<List<ApprovalReview>>,
    private val approve: suspend (ApprovalReview, String) -> Unit,
) : ViewModel() {
    data class State(
        val queue: List<ApprovalReview> = emptyList(),
        val selectedIds: Set<String> = emptySet(),
        val comment: String = "",
        val busy: Boolean = false,
        val failures: Map<String, String> = emptyMap(),
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            queue.collect { reviews ->
                val eligible = reviews.filter { it.canBulkApprove }.map { it.report.id }.toSet()
                mutableState.update { it.copy(queue = reviews, selectedIds = it.selectedIds.intersect(eligible)) }
            }
        }
    }

    fun select(id: String) {
        if (state.value.busy || state.value.queue.none { it.report.id == id && it.canBulkApprove }) return
        mutableState.update { it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id) }
    }

    fun comment(value: String) {
        mutableState.update { it.copy(comment = value) }
    }

    fun approveSelected() {
        val snapshot = state.value
        if (snapshot.busy || snapshot.comment.isBlank() || snapshot.selectedIds.isEmpty()) return
        mutableState.update { it.copy(busy = true, failures = emptyMap()) }
        viewModelScope.launch {
            try {
                for (review in snapshot.queue.filter { it.report.id in snapshot.selectedIds && it.canBulkApprove }) {
                    runCatching { approve(review, snapshot.comment.trim()) }
                        .onSuccess { mutableState.update { it.copy(selectedIds = it.selectedIds - review.report.id) } }
                        .onFailure { failure ->
                            if (failure is CancellationException || failure !is Exception) throw failure
                            mutableState.update { it.copy(failures = it.failures + (review.report.id to (failure.message ?: "Approval failed"))) }
                        }
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
