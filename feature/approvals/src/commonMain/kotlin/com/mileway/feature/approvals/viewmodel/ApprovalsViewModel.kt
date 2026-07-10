package com.mileway.feature.approvals.viewmodel

import com.mileway.core.common.UiText
import com.mileway.core.data.ledger.ApprovalTransitions
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.approvals_toast_request_approved
import com.mileway.core.ui.resources.approvals_toast_request_rejected
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.core.data.ledger.ApprovalStatus as FsmStatus

enum class ApprovalTabFilter { ALL, PENDING, APPROVED, REJECTED }

sealed interface ApprovalsAction {
    data object Refresh : ApprovalsAction

    data class SetTab(val tab: ApprovalTabFilter) : ApprovalsAction

    data class OpenDetail(val id: String) : ApprovalsAction

    data object Approve : ApprovalsAction

    data object Reject : ApprovalsAction

    data object OpenClarificationSheet : ApprovalsAction

    data object CloseClarificationSheet : ApprovalsAction

    data class UpdateDraftMessage(val text: String) : ApprovalsAction

    data object SendClarification : ApprovalsAction
}

sealed interface ApprovalsEffect {
    data class ShowToast(val message: UiText) : ApprovalsEffect

    data class NavigateToDetail(val id: String) : ApprovalsEffect

    data object NavigateBack : ApprovalsEffect
}

data class ApprovalDetailState(
    val item: ApprovalItem,
    val thread: List<ClarificationMessage> = emptyList(),
    val draftMessage: String = "",
    val localStatus: ApprovalStatus? = null,
    val showClarificationSheet: Boolean = false,
)

data class ApprovalsUiState(
    val listState: ScreenState<List<ApprovalItem>> = ScreenState.Content(ApprovalsRepository.all),
    val detailState: ScreenState<ApprovalDetailState> = ScreenState.Empty,
    val activeTab: ApprovalTabFilter = ApprovalTabFilter.ALL,
)

/** Maps the UI-local [ApprovalStatus] onto the pure FSM's [FsmStatus] for [ApprovalTransitions] checks. */
private fun ApprovalStatus.toFsmStatus(): FsmStatus =
    when (this) {
        ApprovalStatus.PENDING -> FsmStatus.PENDING
        ApprovalStatus.APPROVED -> FsmStatus.APPROVED
        ApprovalStatus.REJECTED -> FsmStatus.REJECTED
    }

class ApprovalsViewModel :
    BaseViewModel<ApprovalsUiState, ApprovalsEffect, ApprovalsAction>(ApprovalsUiState()) {
    // ponytail: in-flight id guard debounces a double-tapped approve/reject/pay; cleared once the
    // resolve completes (synchronous today — this local mock data has no suspend/network hop).
    private val inFlightIds = mutableSetOf<String>()

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
            ApprovalsAction.OpenClarificationSheet -> updateDetail { copy(showClarificationSheet = true) }
            ApprovalsAction.CloseClarificationSheet -> updateDetail { copy(showClarificationSheet = false) }
            is ApprovalsAction.UpdateDraftMessage -> updateDetail { copy(draftMessage = action.text) }
            ApprovalsAction.SendClarification -> sendClarification()
        }
    }

    private fun openDetail(id: String) {
        val item = ApprovalsRepository.getById(id) ?: return
        setState {
            copy(
                detailState =
                    ScreenState.Content(
                        ApprovalDetailState(
                            item = item,
                            thread = ApprovalsRepository.clarificationThread(id),
                        ),
                    ),
            )
        }
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
        val text = detail.draftMessage.trim()
        if (text.isBlank()) return
        val newMsg =
            ClarificationMessage(
                text,
                isFromRequester = false,
                timestampMs = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            )
        setState {
            copy(
                detailState =
                    ScreenState.Content(
                        detail.copy(
                            thread = detail.thread + newMsg,
                            draftMessage = "",
                            showClarificationSheet = false,
                        ),
                    ),
            )
        }
    }
}
