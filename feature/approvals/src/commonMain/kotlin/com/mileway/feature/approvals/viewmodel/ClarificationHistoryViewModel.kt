package com.mileway.feature.approvals.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.approvals.repository.ClarificationRepository
import kotlinx.coroutines.launch

/** One row in the room-history list — the room plus just enough of its approval to label the row. */
data class ClarificationRoomListItem(
    val room: ClarificationRoom,
    val requesterName: String,
    val summary: String,
)

data class ClarificationHistoryUiState(
    val rooms: ScreenState<List<ClarificationRoomListItem>> = ScreenState.Loading,
)

sealed interface ClarificationHistoryAction {
    data class OpenRoom(val approvalId: String) : ClarificationHistoryAction
}

sealed interface ClarificationHistoryEffect {
    data class NavigateToApproval(val approvalId: String) : ClarificationHistoryEffect
}

/**
 * PLAN_V28 P28.2: the top-level entry point for browsing clarification rooms independent of any
 * single approval's detail screen lifecycle — see `ClarificationRepository.observeAllRooms`.
 * Room-history tabs/search/date-range (P28.5) are a deferred follow-up; this is the minimal list.
 */
class ClarificationHistoryViewModel(
    private val repository: ClarificationRepository,
) : BaseViewModel<ClarificationHistoryUiState, ClarificationHistoryEffect, ClarificationHistoryAction>(ClarificationHistoryUiState()) {
    init {
        viewModelScope.launch {
            repository.observeAllRooms().collect { rooms ->
                val items =
                    rooms.mapNotNull { room ->
                        val approval = ApprovalsRepository.getById(room.approvalId) ?: return@mapNotNull null
                        ClarificationRoomListItem(room, approval.requesterName, approval.summary)
                    }
                setState { copy(rooms = ScreenState.Content(items)) }
            }
        }
    }

    override fun onAction(action: ClarificationHistoryAction) {
        when (action) {
            is ClarificationHistoryAction.OpenRoom -> emitEffect(ClarificationHistoryEffect.NavigateToApproval(action.approvalId))
        }
    }
}
