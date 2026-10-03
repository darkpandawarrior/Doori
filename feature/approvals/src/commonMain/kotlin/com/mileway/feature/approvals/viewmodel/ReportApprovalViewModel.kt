package com.mileway.feature.approvals.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.payout.ReportPaymentRunner
import com.mileway.core.data.session.SessionSource
import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.repository.ClarificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Report-level review reuses the existing clarification store and payments entry point. */
class ReportApprovalViewModel(
    private val reports: ReportRepository,
    private val session: SessionSource,
    private val clarification: ClarificationRepository,
    private val payments: ReportPaymentRunner,
) : ViewModel() {
    data class State(
        val report: Report? = null,
        val loading: Boolean = true,
        val busy: Boolean = false,
        val comment: String = "",
        val error: String? = null,
        val room: ClarificationRoom? = null,
        val thread: List<ClarificationMessage> = emptyList(),
        val draftMessage: String = "",
        val draftAttachmentUrl: String? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    val queue = reports.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    private var reportJob: Job? = null
    private var roomJob: Job? = null
    private var messageJob: Job? = null

    fun open(reportId: String) {
        reportJob?.cancel()
        roomJob?.cancel()
        messageJob?.cancel()
        mutableState.value = State()
        reportJob = viewModelScope.launch {
            reports.observe(reportId).collect { report ->
                mutableState.update { it.copy(report = report, loading = false) }
            }
        }
    }

    fun comment(value: String) { mutableState.update { it.copy(comment = value) } }

    fun act(action: ApprovalAction) = perform {
        val ui = state.value
        val report = requireNotNull(ui.report) { "Report not found" }
        val actor = requireNotNull(session.sessionState.first().employeeCode) { "Sign in before reviewing a report" }
        reports.act(report.id, report.recordVersion, actor, action, ui.comment)
        mutableState.update { it.copy(comment = "") }
        if (action == ApprovalAction.APPROVE) payments.pay(report.id)
    }

    fun retryPayout() = perform { payments.pay(requireNotNull(state.value.report).id) }

    fun openClarification() = perform {
        val report = requireNotNull(state.value.report)
        val actor = requireNotNull(session.sessionState.first().employeeCode) { "Sign in before commenting" }
        val room = clarification.getOrCreateRoom("report:${report.id}", listOf(report.employeeId, actor))
        roomJob?.cancel()
        messageJob?.cancel()
        roomJob = viewModelScope.launch {
            clarification.observeRoom("report:${report.id}").collect { room -> mutableState.update { it.copy(room = room) } }
        }
        messageJob = viewModelScope.launch {
            clarification.observeMessages(room.roomId).collect { thread -> mutableState.update { it.copy(thread = thread) } }
        }
    }

    fun draftMessage(value: String) { mutableState.update { it.copy(draftMessage = value) } }

    fun draftAttachment(value: String?) { mutableState.update { it.copy(draftAttachmentUrl = value) } }

    fun sendMessage() = perform {
        val ui = state.value
        val room = requireNotNull(ui.room)
        require(ui.draftMessage.isNotBlank() || ui.draftAttachmentUrl != null) { "Enter a message or attachment" }
        val actor = requireNotNull(session.sessionState.first().employeeCode)
        clarification.sendMessage(room.roomId, actor, false, ui.draftMessage.trim(), attachmentUrl = ui.draftAttachmentUrl)
        mutableState.update { it.copy(draftMessage = "", draftAttachmentUrl = null) }
    }

    fun closeRoom() = perform { clarification.closeRoom(requireNotNull(state.value.room).roomId) }

    private fun perform(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.update { it.copy(error = failure.message ?: "Action failed; retry") }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
