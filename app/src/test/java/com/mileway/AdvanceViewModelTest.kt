package com.mileway

import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.profile.model.AdvanceMode
import com.mileway.feature.profile.model.AdvanceType
import com.mileway.feature.profile.repository.AdvanceRepository
import com.mileway.feature.profile.viewmodel.AdvanceAction
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** P4.1: AdvanceMode (cash vs card-linked) step-0 selector. */
class AdvanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun vm() = AdvanceViewModel(AdvanceRepository())

    @Test
    fun `form starts on step 0 with CASH mode and no selected card`() =
        runTest {
            val vm = vm()
            assertEquals(0, vm.state.value.form.step)
            assertEquals(AdvanceMode.CASH, vm.state.value.form.mode)
            assertNull(vm.state.value.form.selectedCardId)
        }

    @Test
    fun `cash path advances past step 0 without requiring a card`() =
        runTest {
            val vm = vm()
            vm.onAction(AdvanceAction.SetMode(AdvanceMode.CASH))
            vm.onAction(AdvanceAction.GoToStep(1))
            assertEquals(1, vm.state.value.form.step)
            assertNull(vm.state.value.form.selectedCardId)
        }

    @Test
    fun `card-linked path exposes the stub card list and records the selection`() =
        runTest {
            val vm = vm()
            vm.onAction(AdvanceAction.SetMode(AdvanceMode.CARD_LINKED))
            assertEquals(AdvanceMode.CARD_LINKED, vm.state.value.form.mode)
            assertTrue(
                vm.state.value.cards
                    .isNotEmpty(),
                "Expected stub cards to be available for the picker",
            )

            val firstCard =
                vm.state.value.cards
                    .first()
            vm.onAction(AdvanceAction.SelectCard(firstCard.id))
            assertEquals(firstCard.id, vm.state.value.form.selectedCardId)

            vm.onAction(AdvanceAction.GoToStep(1))
            assertEquals(1, vm.state.value.form.step)
        }

    @Test
    fun `switching back to CASH clears a previously selected card`() =
        runTest {
            val vm = vm()
            vm.onAction(AdvanceAction.SetMode(AdvanceMode.CARD_LINKED))
            val firstCard =
                vm.state.value.cards
                    .first()
            vm.onAction(AdvanceAction.SelectCard(firstCard.id))
            assertEquals(firstCard.id, vm.state.value.form.selectedCardId)

            vm.onAction(AdvanceAction.SetMode(AdvanceMode.CASH))
            assertNull(vm.state.value.form.selectedCardId)
        }

    @Test
    fun `resetting the form returns to step 0 with CASH mode`() =
        runTest {
            val vm = vm()
            vm.onAction(AdvanceAction.SetMode(AdvanceMode.CARD_LINKED))
            vm.onAction(
                AdvanceAction.SelectCard(
                    vm.state.value.cards
                        .first()
                        .id,
                ),
            )
            vm.onAction(AdvanceAction.GoToStep(3))

            vm.onAction(AdvanceAction.ResetForm)

            assertEquals(0, vm.state.value.form.step)
            assertEquals(AdvanceMode.CASH, vm.state.value.form.mode)
            assertNull(vm.state.value.form.selectedCardId)
        }

    // P4.3: AdvanceType taxonomy (optional selector).
    @Test
    fun `form starts with no type selected`() =
        runTest {
            val vm = vm()
            assertNull(vm.state.value.form.type)
        }

    @Test
    fun `selecting a type records it on the form`() =
        runTest {
            val vm = vm()
            vm.onAction(AdvanceAction.SetType(AdvanceType.TRAINING))
            assertEquals(AdvanceType.TRAINING, vm.state.value.form.type)
        }

    @Test
    fun `type selection does not block advancing past step 1 when left unset`() =
        runTest {
            val vm = vm()
            vm.onAction(AdvanceAction.SetAmount("500"))
            vm.onAction(AdvanceAction.SetPurpose("Local travel"))
            vm.onAction(AdvanceAction.SetRequiredByDate("2024-03-01"))
            assertNull(vm.state.value.form.type)

            vm.onAction(AdvanceAction.GoToStep(2))

            assertEquals(2, vm.state.value.form.step)
        }

    @Test
    fun `seeded advance record exposes its type for the history card`() =
        runTest {
            val vm = vm()
            vm.onAction(AdvanceAction.LoadDetail("ADV-001"))
            assertEquals(
                AdvanceType.FIELD_VISIT,
                vm.state.value.detail.dataOrNull
                    ?.type,
            )
        }
}
