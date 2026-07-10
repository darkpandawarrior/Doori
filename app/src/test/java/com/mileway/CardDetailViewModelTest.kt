package com.mileway

import app.cash.turbine.test
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.cards.model.CardModel
import com.mileway.feature.cards.model.CardShippingAddress
import com.mileway.feature.cards.model.CardStatus
import com.mileway.feature.cards.model.CardTxnClaimStatus
import com.mileway.feature.cards.viewmodel.CardDetailAction
import com.mileway.feature.cards.viewmodel.CardDetailEffect
import com.mileway.feature.cards.viewmodel.CardDetailViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Q.3 / Q+.1 / Q+.3, card detail reducers (claim tabs + controls). */
class CardDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun vm() = CardDetailViewModel(CardsMockDataProviderFactory.provider("en"))

    private fun card(vm: CardDetailViewModel): CardModel = (vm.state.value.card as ScreenState.Content).data

    @Test
    fun `load resolves the card`() =
        runTest {
            val vm = vm()
            vm.onAction(CardDetailAction.Load(1L))
            assertEquals(1L, card(vm).id)
        }

    @Test
    fun `select claim tab filters transactions`() =
        runTest {
            val vm = vm()
            vm.onAction(CardDetailAction.Load(1L))
            vm.onAction(CardDetailAction.SelectClaimTab(CardTxnClaimStatus.PERSONAL))
            assertEquals(CardTxnClaimStatus.PERSONAL, vm.state.value.claimTab)
        }

    @Test
    fun `toggle block flips status and emits a toast`() =
        runTest {
            val vm = vm()
            vm.onAction(CardDetailAction.Load(1L))
            vm.effect.test {
                vm.onAction(CardDetailAction.ToggleBlock)
                assertTrue(awaitItem() is CardDetailEffect.ShowToast)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(CardStatus.BLOCKED, card(vm).status)
        }

    @Test
    fun `set monthly limit updates the card and dismisses the dialog`() =
        runTest {
            val vm = vm()
            vm.onAction(CardDetailAction.Load(1L))
            vm.onAction(CardDetailAction.OpenMonthlyLimit)
            vm.onAction(CardDetailAction.SetMonthlyLimit(7777.0))
            assertEquals(7777.0, card(vm).monthlyLimit)
            assertFalse(vm.state.value.showMonthlyLimitDialog)
        }

    @Test
    fun `claim transaction moves it out of the unclaimed tab`() =
        runTest {
            val vm = vm()
            vm.onAction(CardDetailAction.Load(1L))
            val unclaimed = (vm.state.value.transactions as ScreenState.Content).data
            val target = unclaimed.first()
            vm.onAction(CardDetailAction.ClaimTransaction(target.id))
            val remaining = (vm.state.value.transactions as? ScreenState.Content)?.data ?: emptyList()
            assertTrue(remaining.none { it.id == target.id })
        }

    // P27.E.7: claiming a transaction now navigates into the expense-entry flow (real nav,
    // replacing the old toast-only stub) carrying that exact transaction's Card context.
    @Test
    fun `claim transaction emits NavigateToExpenseEntry carrying the transaction's Card context`() =
        runTest {
            val vm = vm()
            vm.onAction(CardDetailAction.Load(1L))
            val target = (vm.state.value.transactions as ScreenState.Content).data.first()

            vm.effect.test {
                vm.onAction(CardDetailAction.ClaimTransaction(target.id))
                val effect = awaitItem()
                assertIs<CardDetailEffect.NavigateToExpenseEntry>(effect)
                val context = effect.context
                assertIs<ExpenseSourceContext.Card>(context)
                assertEquals(target.cardId.toString(), context.cardId)
                assertEquals(target.id.toString(), context.transactionId)
                assertEquals(target.merchantName, context.merchantName)
                assertEquals(target.amount, context.transactionAmountRupees)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `issue physical card sets the status`() =
        runTest {
            val vm = vm()
            vm.onAction(CardDetailAction.Load(1L))
            vm.onAction(
                CardDetailAction.IssuePhysicalCard(
                    CardShippingAddress("12 St", "Marina", "Dubai", "DXB", "00000"),
                ),
            )
            assertEquals(CardStatus.PHYSICAL_ISSUED, card(vm).status)
        }
}
