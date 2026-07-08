package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.review.ReviewResult
import com.mileway.core.data.review.SimulatedReviewEngine
import com.mileway.core.data.verification.DocStatus
import com.mileway.core.data.verification.VerificationDocument
import com.mileway.feature.profile.repository.DocumentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PLAN_V24 P4.2: drives `VerificationCentreScreen` + `DocumentDetailScreen`. Seeds the document
 * catalogue once, exposes the aggregate counters, and — after "Submit for verification" flips
 * mandatory docs to APPROVAL_PENDING — resolves each pending doc through [SimulatedReviewEngine]
 * (P0.5): a doc whose info/reason payload contains the reject marker becomes REJECTED-with-reason,
 * everything else VERIFIED. Resolution runs both once on load (so a submit that survived process
 * death still resolves) and after a short delay following a fresh submit.
 */
data class VerificationUiState(
    val documents: List<VerificationDocument> = emptyList(),
    val submitError: String? = null,
    val submittedAtLeastOnce: Boolean = false,
) {
    val verifiedCount: Int get() = documents.count { it.status == DocStatus.VERIFIED }
    val pendingCount: Int get() = documents.count { it.status == DocStatus.APPROVAL_PENDING }
    val rejectedCount: Int get() = documents.count { it.status == DocStatus.REJECTED }
    val canSubmit: Boolean get() = documents.all { it.mandatoryFieldsFilled }
}

class VerificationCentreViewModel(
    private val repository: DocumentRepository,
    private val reviewEngine: SimulatedReviewEngine,
) : ViewModel() {
    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        repository.observeAll()
            .onEach { docs -> _state.update { it.copy(documents = docs) } }
            .launchIn(viewModelScope)
        // Resolve anything already reviewable (e.g. a submit from a previous, killed session).
        viewModelScope.launch { resolveReviewablePending() }
    }

    fun uploadSlot(
        docType: String,
        url: String,
    ) {
        viewModelScope.launch { repository.uploadSlot(docType, url) }
    }

    fun updateInfoField(
        docType: String,
        key: String,
        value: String,
    ) {
        viewModelScope.launch { repository.updateInfoField(docType, key, value) }
    }

    /**
     * Submits for verification behind the T&C agreement. On the mandatory-fields gate failing,
     * sets [VerificationUiState.submitError] to the first failing document's name and persists
     * nothing; on success schedules the simulated review pass.
     */
    fun submit() {
        viewModelScope.launch {
            val (ok, failing) = repository.submitForVerification()
            if (!ok) {
                _state.update { it.copy(submitError = failing) }
                return@launch
            }
            _state.update { it.copy(submitError = null, submittedAtLeastOnce = true) }
            // Let the review "complete" a beat later, then flip pending → verified/rejected.
            delay(SimulatedReviewEngine.DEFAULT_SIM_DELAY_MILLIS + REVIEW_BUFFER_MILLIS)
            resolveReviewablePending()
        }
    }

    fun clearSubmitError() {
        _state.update { it.copy(submitError = null) }
    }

    /** Flips every APPROVAL_PENDING doc that the review engine now considers reviewed. */
    private suspend fun resolveReviewablePending() {
        _state.value.documents
            .filter { it.status == DocStatus.APPROVAL_PENDING }
            .forEach { doc ->
                val payload = (doc.docInfo.joinToString(" ") { it.value } + " " + doc.reason).trim()
                when (val result = reviewEngine.resolve(doc.updatedAtMillis, payload)) {
                    is ReviewResult.Approved -> repository.setStatus(doc.docType, DocStatus.VERIFIED)
                    is ReviewResult.Rejected -> repository.setStatus(doc.docType, DocStatus.REJECTED, result.reason)
                    ReviewResult.Pending -> Unit
                }
            }
    }

    private companion object {
        const val REVIEW_BUFFER_MILLIS = 250L
    }
}
