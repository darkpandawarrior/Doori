package com.mileway.feature.approvals.viewmodel

import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ClarificationRoomStatus
import com.mileway.feature.approvals.repository.ApprovalsRepository
import com.mileway.feature.approvals.repository.FakeApprovalCommentRepository
import com.mileway.feature.approvals.repository.FakeClarificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

    // PLAN_V33: resolve() used to re-derive from ApprovalsRepository.approve/reject, which always
    // maps the static seed list — so a second resolve() in the same session silently reverted
    // whatever the first one had just changed. This is the regression test for that fix.
    @Test
    fun `resolving a second approval in the same session keeps the first one's change`() =
        runTest {
            val vm = newViewModel()

            vm.onAction(ApprovalsAction.OpenDetail("A001"))
            advanceUntilIdle()
            vm.onAction(ApprovalsAction.Approve)
            advanceUntilIdle()

            vm.onAction(ApprovalsAction.OpenDetail("A002"))
            advanceUntilIdle()
            vm.onAction(ApprovalsAction.RejectWithReason("Missing receipt"))
            advanceUntilIdle()

            val list = vm.state.value.listState.dataOrNull.orEmpty()
            assertEquals(ApprovalStatus.APPROVED, list.first { it.id == "A001" }.status)
            assertEquals(ApprovalStatus.REJECTED, list.first { it.id == "A002" }.status)
        }

    @Test
    fun `reject with reason posts the reason to the approval's comment thread`() =
        runTest {
            val commentRepo = FakeApprovalCommentRepository()
            val vm = ApprovalsViewModel(FakeClarificationRepository(), commentRepo)

            vm.onAction(ApprovalsAction.OpenDetail("A001"))
            advanceUntilIdle()
            vm.onAction(ApprovalsAction.RejectWithReason("Exceeds policy limit"))
            advanceUntilIdle()

            val comments = commentRepo.observeComments("A001").first()
            assertEquals(1, comments.size)
            assertTrue(comments.single().message.contains("Exceeds policy limit"))
        }

    @Test
    fun `bulk approve resolves every selected id and leaves the rest untouched`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(ApprovalsAction.BulkApprove(setOf("A001", "A002")))
            advanceUntilIdle()

            val list = vm.state.value.listState.dataOrNull.orEmpty()
            assertEquals(ApprovalStatus.APPROVED, list.first { it.id == "A001" }.status)
            assertEquals(ApprovalStatus.APPROVED, list.first { it.id == "A002" }.status)
            // A004 started APPROVED in the seed data — untouched by a bulk action that didn't select it.
            assertEquals(ApprovalStatus.APPROVED, list.first { it.id == "A004" }.status)
        }

    @Test
    fun `bulk reject posts the reason once per selected id`() =
        runTest {
            val commentRepo = FakeApprovalCommentRepository()
            val vm = ApprovalsViewModel(FakeClarificationRepository(), commentRepo)

            vm.onAction(ApprovalsAction.BulkReject(setOf("A001", "A002"), "Duplicate submission"))
            advanceUntilIdle()

            val list = vm.state.value.listState.dataOrNull.orEmpty()
            assertEquals(ApprovalStatus.REJECTED, list.first { it.id == "A001" }.status)
            assertEquals(ApprovalStatus.REJECTED, list.first { it.id == "A002" }.status)
            assertTrue(commentRepo.observeComments("A001").first().single().message.contains("Duplicate submission"))
            assertTrue(commentRepo.observeComments("A002").first().single().message.contains("Duplicate submission"))
        }

    @Test
    fun `bulk resolve never mutates the underlying repository seed`() =
        runTest {
            val vm = newViewModel()
            vm.onAction(ApprovalsAction.BulkApprove(setOf("A001")))
            advanceUntilIdle()

            // The seed itself (read by every other tab/screen) stays PENDING — only this
            // ViewModel's own listState reflects the in-session change.
            assertEquals(ApprovalStatus.PENDING, ApprovalsRepository.getById("A001")?.status)
        }
}
