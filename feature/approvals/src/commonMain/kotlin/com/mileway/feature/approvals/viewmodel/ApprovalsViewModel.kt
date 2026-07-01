package com.mileway.feature.approvals.viewmodel

import com.mileway.core.common.UiText
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ClarificationMessage
import com.mileway.feature.approvals.repository.ApprovalsRepository

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

class ApprovalsViewModel :
    BaseViewModel<ApprovalsUiState, ApprovalsEffect, ApprovalsAction>(ApprovalsUiState()) {
    override fun onAction(action: ApprovalsAction) {
        when (action) {
            ApprovalsAction.Refresh ->
                setState { copy(listState = ScreenState.Content(ApprovalsRepository.all)) }
            is ApprovalsAction.SetTab -> setState { copy(activeTab = action.tab) }
            is ApprovalsAction.OpenDetail -> openDetail(action.id)
            ApprovalsAction.Approve -> resolve(ApprovalStatus.APPROVED, "Request approved")
            ApprovalsAction.Reject -> resolve(ApprovalStatus.REJECTED, "Request rejected")
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
        toast: String,
    ) {
        val detail = currentDetail() ?: return
        val updatedList =
            if (status == ApprovalStatus.APPROVED) {
                ApprovalsRepository.approve(detail.item.id)
            } else {
                ApprovalsRepository.reject(detail.item.id)
            }
        setState {
            copy(
                listState = ScreenState.Content(updatedList),
                detailState = ScreenState.Content(detail.copy(localStatus = status)),
            )
        }
        emitEffect(ApprovalsEffect.ShowToast(UiText.Static(toast)))
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
