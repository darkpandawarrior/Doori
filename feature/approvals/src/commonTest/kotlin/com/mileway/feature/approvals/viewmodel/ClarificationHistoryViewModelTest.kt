package com.mileway.feature.approvals.viewmodel

import com.mileway.core.ui.mvi.dataOrNull
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

/** PLAN_V28 P28.2: the top-level clarification-history entry point — rooms across every approval. */
@OptIn(ExperimentalCoroutinesApi::class)
class ClarificationHistoryViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lists a room for each approval that has one, labelled from ApprovalsRepository`() =
        runTest {
            val repo = FakeClarificationRepository()
            repo.getOrCreateRoom("A001", participants = listOf("Priya Sharma", "approver"))
            repo.getOrCreateRoom("A002", participants = listOf("Rahul Mehra", "approver"))

            val vm = ClarificationHistoryViewModel(repo)
            advanceUntilIdle()

            val items = vm.state.value.rooms.dataOrNull.orEmpty()
            assertEquals(2, items.size)
            assertEquals(setOf("Priya Sharma", "Rahul Mehra"), items.map { it.requesterName }.toSet())
        }

    @Test
    fun `an unknown approval id is skipped rather than crashing`() =
        runTest {
            val repo = FakeClarificationRepository()
            repo.getOrCreateRoom("does-not-exist", participants = emptyList())

            val vm = ClarificationHistoryViewModel(repo)
            advanceUntilIdle()

            assertEquals(emptyList(), vm.state.value.rooms.dataOrNull)
        }
}
