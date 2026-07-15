package com.mileway.feature.approvals.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.ledger.ApprovalTransitions
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.approvals_toast_edit_distance_unavailable
import com.mileway.core.ui.resources.approvals_toast_request_approved
import com.mileway.core.ui.resources.approvals_toast_request_rejected
import com.mileway.core.ui.resources.approvals_toast_request_withdrawn
import com.mileway.feature.approvals.model.ApprovalComment
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.model.ClarificationRoomMeta
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import com.mileway.feature.approvals.repository.APPROVER_SENDER_ID
import com.mileway.feature.approvals.repository.ApprovalCommentRepository
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.approvals.repository.ClarificationRepository
import com.siddharth.kmp.common.UiText
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.mileway.core.data.ledger.ApprovalStatus as FsmStatus

enum class ApprovalTabFilter { ALL, PENDING, APPROVED, REJECTED }

sealed interface ApprovalsAction {
    data object Refresh : ApprovalsAction

    data class SetTab(val tab: ApprovalTabFilter) : ApprovalsAction

    data class OpenDetail(val id: String) : ApprovalsAction

    data object Approve : ApprovalsAction

    data object Reject : ApprovalsAction

    /** P28.9: withdraws the current (own, still-PENDING) request — gated by `DetailActionFlags.canWithdraw`. */
    data object Withdraw : ApprovalsAction

    /** P28.9: MILEAGE-only, gated by `DetailActionFlags.canEditDistance` — no in-approvals edit flow exists yet. */
    data object RequestEditDistance : ApprovalsAction

    data object OpenClarificationSheet : ApprovalsAction

    data object CloseClarificationSheet : ApprovalsAction

    data class UpdateDraftMessage(val text: String) : ApprovalsAction

    /** P28.6: a picked core:media attachment's local URI for the in-flight draft, or null to clear it. */
    data class UpdateDraftAttachment(val url: String?) : ApprovalsAction

    data object SendClarification : ApprovalsAction

    /** P28.3: opens the "close this room?" confirmation gate. */
    data object RequestCloseRoom : ApprovalsAction

    data object ConfirmCloseRoom : ApprovalsAction

    data object DismissCloseRoomConfirmation : ApprovalsAction

    /** P28.4: toggles the current room's save/pin flag from the detail sheet's meta-chip row. */
    data object ToggleRoomSaved : ApprovalsAction

    data object ToggleRoomPinned : ApprovalsAction

    /** P28.4: the approvals list's SAVED filter chip — shows only approvals with a saved room. */
    data object ToggleSavedFilter : ApprovalsAction

    /** P28.7: the permanent comments tab's composer draft + post action. */
    data class UpdateCommentDraft(val text: String) : ApprovalsAction

    data object PostComment : ApprovalsAction

    /** P28.9: toggles the acknowledgement checkbox gating Approve/Reject when `requiresAck` is true. */
    data object ToggleAcknowledged : ApprovalsAction
}

sealed interface ApprovalsEffect {
    data class ShowToast(val message: UiText) : ApprovalsEffect

    data class NavigateToDetail(val id: String) : ApprovalsEffect

    data object NavigateBack : ApprovalsEffect
}

data class ApprovalDetailState(
    val item: ApprovalItem,
    val room: ClarificationRoom? = null,
    val thread: List<ClarificationMessage> = emptyList(),
    val roomMeta: ClarificationRoomMeta? = null,
    val draftMessage: String = "",
    /** P28.6: a picked core:media attachment's local URI, cleared once the draft is sent. */
    val draftAttachmentUrl: String? = null,
    val localStatus: ApprovalStatus? = null,
    val showClarificationSheet: Boolean = false,
    val showCloseRoomConfirmation: Boolean = false,
    // P28.7: the permanent comments tab.
    val comments: List<ApprovalComment> = emptyList(),
    val commentDraft: String = "",
    // P28.9: local acknowledgement of a flagged violation — gates Approve/Reject when `requiresAck`.
    val acknowledged: Boolean = false,
)

data class ApprovalsUiState(
    val listState: ScreenState<List<ApprovalItem>> = ScreenState.Content(ApprovalsRepository.all),
    val detailState: ScreenState<ApprovalDetailState> = ScreenState.Empty,
    val activeTab: ApprovalTabFilter = ApprovalTabFilter.ALL,
    /** P28.4: approvalIds whose room is saved — drives the SAVED filter chip. */
    val savedApprovalIds: Set<String> = emptySet(),
    val savedFilterOn: Boolean = false,
)

/** Maps the UI-local [ApprovalStatus] onto the pure FSM's [FsmStatus] for [ApprovalTransitions] checks. */
private fun ApprovalStatus.toFsmStatus(): FsmStatus =
    when (this) {
        ApprovalStatus.PENDING -> FsmStatus.PENDING
        ApprovalStatus.APPROVED -> FsmStatus.APPROVED
        ApprovalStatus.REJECTED -> FsmStatus.REJECTED
    }

class ApprovalsViewModel(
    private val clarificationRepository: ClarificationRepository,
    private val commentRepository: ApprovalCommentRepository,
) : BaseViewModel<ApprovalsUiState, ApprovalsEffect, ApprovalsAction>(ApprovalsUiState()) {
    // ponytail: in-flight id guard debounces a double-tapped approve/reject/pay; cleared once the
    // resolve completes (synchronous today — this local mock data has no suspend/network hop).
    private val inFlightIds = mutableSetOf<String>()

    // P28.2: cancelled + relaunched on every OpenDetail so a stale room's Flow collection never
    // leaks into the next-opened approval's detail state.
    private var clarificationJob: Job? = null

    // P28.7: same relaunch-per-OpenDetail guard as clarificationJob, for the comments Flow.
    private var commentsJob: Job? = null

    init {
        // P28.4: keeps the SAVED filter chip's candidate set live as rooms get saved/unsaved.
        viewModelScope.launch {
            clarificationRepository.observeSavedApprovalIds().collect { ids ->
                setState { copy(savedApprovalIds = ids) }
            }
        }
    }

    override fun onAction(action: ApprovalsAction) {
        when (action) {
            ApprovalsAction.Refresh ->
                setState { copy(listState = ScreenState.Content(ApprovalsRepository.all)) }
            is ApprovalsAction.SetTab -> setState { copy(activeTab = action.tab) }
            is ApprovalsAction.OpenDetail -> openDetail(action.id)
            ApprovalsAction.Approve ->
                resolve(ApprovalStatus.APPROVED, UiText.Res(Res.string.approvals_toast_request_approved.key))
            ApprovalsAction.Reject ->
                resolve(ApprovalStatus.REJECTED, UiText.Res(Res.string.approvals_toast_request_rejected.key))
            ApprovalsAction.Withdraw ->
                resolve(ApprovalStatus.REJECTED, UiText.Res(Res.string.approvals_toast_request_withdrawn.key))
            ApprovalsAction.RequestEditDistance ->
                emitEffect(ApprovalsEffect.ShowToast(UiText.Res(Res.string.approvals_toast_edit_distance_unavailable.key)))
            ApprovalsAction.OpenClarificationSheet -> updateDetail { copy(showClarificationSheet = true) }
            ApprovalsAction.CloseClarificationSheet -> updateDetail { copy(showClarificationSheet = false) }
            is ApprovalsAction.UpdateDraftMessage -> updateDetail { copy(draftMessage = action.text) }
            is ApprovalsAction.UpdateDraftAttachment -> updateDetail { copy(draftAttachmentUrl = action.url) }
            ApprovalsAction.SendClarification -> sendClarification()
            ApprovalsAction.RequestCloseRoom -> updateDetail { copy(showCloseRoomConfirmation = true) }
            ApprovalsAction.DismissCloseRoomConfirmation -> updateDetail { copy(showCloseRoomConfirmation = false) }
            ApprovalsAction.ConfirmCloseRoom -> confirmCloseRoom()
            ApprovalsAction.ToggleRoomSaved -> toggleRoomMeta { room, meta -> clarificationRepository.setSaved(room.roomId, !meta.isSaved) }
            ApprovalsAction.ToggleRoomPinned -> toggleRoomMeta { room, meta -> clarificationRepository.setPinned(room.roomId, !meta.isPinned) }
            ApprovalsAction.ToggleSavedFilter -> setState { copy(savedFilterOn = !savedFilterOn) }
            is ApprovalsAction.UpdateCommentDraft -> updateDetail { copy(commentDraft = action.text) }
            ApprovalsAction.PostComment -> postComment()
            ApprovalsAction.ToggleAcknowledged -> updateDetail { copy(acknowledged = !acknowledged) }
        }
    }

    private fun openDetail(id: String) {
        val item = ApprovalsRepository.getById(id) ?: return
        setState { copy(detailState = ScreenState.Content(ApprovalDetailState(item = item))) }

        clarificationJob?.cancel()
        clarificationJob =
            viewModelScope.launch {
                // P28.2: creates the room (seeded) on first open only; every later open reads the
                // same persisted room — this is what fixes the reset-to-seed-every-open bug.
                val room = clarificationRepository.getOrCreateRoom(id, participants = listOf(item.requesterName, APPROVER_SENDER_ID))
                combine(
                    clarificationRepository.observeRoom(id),
                    clarificationRepository.observeMessages(room.roomId),
                    clarificationRepository.observeMeta(room.roomId),
                ) { observedRoom, messages, meta -> Triple(observedRoom ?: room, messages, meta) }
                    .collect { (roomState, messages, meta) ->
                        updateDetail { copy(room = roomState, thread = messages, roomMeta = meta) }
                    }
            }

        commentsJob?.cancel()
        commentsJob =
            viewModelScope.launch {
                commentRepository.observeComments(id).collect { comments -> updateDetail { copy(comments = comments) } }
            }
    }

    private fun postComment() {
        val detail = currentDetail() ?: return
        val text = detail.commentDraft.trim()
        if (text.isBlank()) return
        updateDetail { copy(commentDraft = "") }
        viewModelScope.launch {
            commentRepository.addComment(detail.item.id, authorName = "You", designation = "Approver", message = text)
        }
    }

    /** P28.4: shared guard for the two meta toggles — both need a live room to act on. */
    private fun toggleRoomMeta(update: suspend (ClarificationRoom, ClarificationRoomMeta) -> Unit) {
        val detail = currentDetail() ?: return
        val room = detail.room ?: return
        val meta = detail.roomMeta ?: ClarificationRoomMeta(room.roomId)
        viewModelScope.launch { update(room, meta) }
    }

    private fun currentDetail(): ApprovalDetailState? = (currentState.detailState as? ScreenState.Content)?.data

    private fun updateDetail(reducer: ApprovalDetailState.() -> ApprovalDetailState) {
        val detail = currentDetail() ?: return
        setState { copy(detailState = ScreenState.Content(detail.reducer())) }
    }

    private fun resolve(
        status: ApprovalStatus,
        toast: UiText,
    ) {
        val detail = currentDetail() ?: return
        val id = detail.item.id
        // Debounce: ignore a second tap for the same approval while the first is still applying.
        if (id in inFlightIds) return
        val from = (detail.localStatus ?: detail.item.status).toFsmStatus()
        if (!ApprovalTransitions.isAllowed(from, status.toFsmStatus())) return

        inFlightIds += id
        val updatedList =
            if (status == ApprovalStatus.APPROVED) {
                ApprovalsRepository.approve(id)
            } else {
                ApprovalsRepository.reject(id)
            }
        setState {
            copy(
                listState = ScreenState.Content(updatedList),
                detailState = ScreenState.Content(detail.copy(localStatus = status)),
            )
        }
        emitEffect(ApprovalsEffect.ShowToast(toast))
        inFlightIds -= id
    }

    private fun sendClarification() {
        val detail = currentDetail() ?: return
        val room = detail.room ?: return
        if (room.status == ClarificationRoomStatus.CLOSED) return
        val text = detail.draftMessage.trim()
        val attachmentUrl = detail.draftAttachmentUrl
        if (text.isBlank() && attachmentUrl == null) return
        // Clear the draft + dismiss the sheet locally; the sent message itself arrives back
        // through the live observeMessages() collection started in openDetail().
        updateDetail { copy(draftMessage = "", draftAttachmentUrl = null, showClarificationSheet = false) }
        viewModelScope.launch {
            clarificationRepository.sendMessage(
                room.roomId,
                senderId = APPROVER_SENDER_ID,
                isFromRequester = false,
                text = text,
                senderName = "You",
                senderRole = "Approver",
                attachmentUrl = attachmentUrl,
            )
        }
    }

    private fun confirmCloseRoom() {
        val detail = currentDetail() ?: return
        val room = detail.room ?: return
        updateDetail { copy(showCloseRoomConfirmation = false) }
        viewModelScope.launch { clarificationRepository.closeRoom(room.roomId) }
    }
}
