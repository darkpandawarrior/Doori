package com.mileway.feature.approvals.viewmodel

import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import com.mileway.feature.approvals.repository.FakeApprovalCommentRepository
import com.mileway.feature.approvals.repository.FakeClarificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PLAN_V28 P28.2/P28.3: covers the fix for "the clarification thread resets to a hardcoded seed
 * every time the detail screen reopens" — a room is now created once (via [FakeClarificationRepository],
 * same shape as [com.mileway.feature.approvals.repository.RoomClarificationRepository]'s real
 * Room-backed store) and re-opening the same approval reads the same persisted room. Also covers
 * the P28.3 close-room lifecycle gate.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ApprovalsViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repo: FakeClarificationRepository = FakeClarificationRepository()) = ApprovalsViewModel(repo, FakeApprovalCommentRepository())

    @Test
    fun `opening a detail twice reuses the same persisted room instead of reseeding`() =
        runTest {
            val repo = FakeClarificationRepository()
            val vm = newViewModel(repo)

            vm.onAction(ApprovalsAction.OpenDetail("A001"))
            advanceUntilIdle()
            val firstRoomId = vm.state.value.detailState.dataOrNull?.room?.roomId

            vm.onAction(ApprovalsAction.OpenDetail("A002"))
            advanceUntilIdle()
            vm.onAction(ApprovalsAction.OpenDetail("A001"))
            advanceUntilIdle()
            val secondRoomId = vm.state.value.detailState.dataOrNull?.room?.roomId

            assertTrue(firstRoomId != null)
            assertEquals(firstRoomId, secondRoomId)
        }

    @Test
    fun `sending a message persists it and clears the draft`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(ApprovalsAction.OpenDetail("A001"))
            advanceUntilIdle()

            vm.onAction(ApprovalsAction.UpdateDraftMessage("Can you attach the invoice?"))
            vm.onAction(ApprovalsAction.SendClarification)
            advanceUntilIdle()

            val detail = vm.state.value.detailState.dataOrNull
            assertEquals(1, detail?.thread?.size)
            assertEquals("Can you attach the invoice?", detail?.thread?.single()?.text)
            assertEquals("", detail?.draftMessage)
        }

    @Test
    fun `closing a room sets it CLOSED and blocks a further send`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(ApprovalsAction.OpenDetail("A001"))
            advanceUntilIdle()

            vm.onAction(ApprovalsAction.RequestCloseRoom)
            assertTrue(vm.state.value.detailState.dataOrNull?.showCloseRoomConfirmation == true)

            vm.onAction(ApprovalsAction.ConfirmCloseRoom)
            advanceUntilIdle()
            assertEquals(ClarificationRoomStatus.CLOSED, vm.state.value.detailState.dataOrNull?.room?.status)
            assertTrue(vm.state.value.detailState.dataOrNull?.showCloseRoomConfirmation == false)

            vm.onAction(ApprovalsAction.UpdateDraftMessage("still trying to send"))
            vm.onAction(ApprovalsAction.SendClarification)
            advanceUntilIdle()

            // Guarded in the ViewModel: a CLOSED room's SendClarification is a no-op.
            assertTrue(vm.state.value.detailState.dataOrNull?.thread.isNullOrEmpty())
        }

    @Test
    fun `toggling saved flips the room's meta and feeds the SAVED filter's candidate set`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(ApprovalsAction.OpenDetail("A001"))
            advanceUntilIdle()

            assertTrue(vm.state.value.detailState.dataOrNull?.roomMeta?.isSaved != true)
            assertTrue("A001" !in vm.state.value.savedApprovalIds)

            vm.onAction(ApprovalsAction.ToggleRoomSaved)
            advanceUntilIdle()

            assertTrue(vm.state.value.detailState.dataOrNull?.roomMeta?.isSaved == true)
            assertTrue("A001" in vm.state.value.savedApprovalIds)
        }
}
