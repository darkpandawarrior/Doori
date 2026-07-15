package com.mileway.feature.approvals.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.approvals.model.ClarificationRoom
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.approvals.repository.ClarificationRepository
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** One row in the room-history list — the room plus just enough of its approval to label the row, plus its saved flag for the SAVED tab. */
data class ClarificationRoomListItem(
    val room: ClarificationRoom,
    val requesterName: String,
    val summary: String,
    val isSaved: Boolean = false,
)

/** P28.5: Active/Closed/Saved — Saved cuts across status, same as the approvals-list SAVED chip. */
enum class ClarificationHistoryTab { ACTIVE, CLOSED, SAVED }

/** P28.5: a coarse date-range filter on `updatedAtMs`, applied via the scaffold's filter-chip row (no new date-picker dependency needed for four fixed buckets). */
enum class ClarificationDateRange(val windowMs: Long?) {
    ALL(null),
    TODAY(24 * 3_600_000L),
    WEEK(7 * 24 * 3_600_000L),
    MONTH(30 * 24 * 3_600_000L),
}

data class ClarificationHistoryUiState(
    val rooms: ScreenState<List<ClarificationRoomListItem>> = ScreenState.Loading,
    val tab: ClarificationHistoryTab = ClarificationHistoryTab.ACTIVE,
    val query: String = "",
    val dateRange: ClarificationDateRange = ClarificationDateRange.ALL,
)

sealed interface ClarificationHistoryAction {
    data class OpenRoom(val approvalId: String) : ClarificationHistoryAction

    data class SelectTab(val tab: ClarificationHistoryTab) : ClarificationHistoryAction

    data class SetQuery(val query: String) : ClarificationHistoryAction

    data class SetDateRange(val range: ClarificationDateRange) : ClarificationHistoryAction
}

sealed interface ClarificationHistoryEffect {
    data class NavigateToApproval(val approvalId: String) : ClarificationHistoryEffect
}

/**
 * PLAN_V28 P28.2/P28.5: the top-level entry point for browsing clarification rooms independent of
 * any single approval's detail screen lifecycle — see `ClarificationRepository.observeAllRooms`.
 * P28.5 adds Active/Closed/Saved tabs, a requester/summary search, and a coarse date-range filter,
 * all applied client-side over the same live room list (the dataset is small — a handful of rooms
 * per user — so no need for a filtered SQL query per combination).
 */
class ClarificationHistoryViewModel(
    private val repository: ClarificationRepository,
) : BaseViewModel<ClarificationHistoryUiState, ClarificationHistoryEffect, ClarificationHistoryAction>(ClarificationHistoryUiState()) {
    private var allItems: List<ClarificationRoomListItem> = emptyList()

    init {
        viewModelScope.launch {
            combine(repository.observeAllRooms(), repository.observeSavedApprovalIds()) { rooms, savedIds ->
                rooms.mapNotNull { room ->
                    val approval = ApprovalsRepository.getById(room.approvalId) ?: return@mapNotNull null
                    ClarificationRoomListItem(room, approval.requesterName, approval.summary, isSaved = room.approvalId in savedIds)
                }
            }.collect { items ->
                allItems = items
                setState { copy(rooms = ScreenState.Content(applyFilters(items, tab, query, dateRange))) }
            }
        }
    }

    override fun onAction(action: ClarificationHistoryAction) {
        when (action) {
            is ClarificationHistoryAction.OpenRoom -> emitEffect(ClarificationHistoryEffect.NavigateToApproval(action.approvalId))
            is ClarificationHistoryAction.SelectTab -> refilter { copy(tab = action.tab) }
            is ClarificationHistoryAction.SetQuery -> refilter { copy(query = action.query) }
            is ClarificationHistoryAction.SetDateRange -> refilter { copy(dateRange = action.range) }
        }
    }

    private fun refilter(reducer: ClarificationHistoryUiState.() -> ClarificationHistoryUiState) {
        setState {
            val next = reducer()
            next.copy(rooms = ScreenState.Content(applyFilters(allItems, next.tab, next.query, next.dateRange)))
        }
    }

    private fun applyFilters(
        items: List<ClarificationRoomListItem>,
        tab: ClarificationHistoryTab,
        query: String,
        dateRange: ClarificationDateRange,
    ): List<ClarificationRoomListItem> {
        val byTab =
            when (tab) {
                ClarificationHistoryTab.ACTIVE -> items.filter { it.room.status == ClarificationRoomStatus.ACTIVE }
                ClarificationHistoryTab.CLOSED -> items.filter { it.room.status == ClarificationRoomStatus.CLOSED }
                ClarificationHistoryTab.SAVED -> items.filter { it.isSaved }
            }
        val byQuery =
            if (query.isBlank()) {
                byTab
            } else {
                byTab.filter { it.requesterName.contains(query, ignoreCase = true) || it.summary.contains(query, ignoreCase = true) }
            }
        val windowMs = dateRange.windowMs ?: return byQuery
        val cutoff = kotlin.time.Clock.System.now().toEpochMilliseconds() - windowMs
        return byQuery.filter { it.room.updatedAtMs >= cutoff }
    }
}
