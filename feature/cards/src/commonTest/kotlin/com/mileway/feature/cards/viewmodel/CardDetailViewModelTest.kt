package com.mileway.feature.cards.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.cards.model.ApprovalStepStatus
import com.mileway.feature.cards.model.CardStatus
import com.mileway.feature.cards.model.CardTxnClaimStatus
import com.mileway.feature.cards.model.LimitKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P29.C.1: `KycVerified` is what `CardsNavigation` dispatches once `CardKycScreen`'s wizard finishes. */
class CardDetailViewModelTest {
    @Test
    fun `KycVerified flips isKycPending and moves a KYC_PENDING card to ACTIVE`() {
        val vm = CardDetailViewModel(CardsMockDataProviderFactory.provider("en"))
        // Card id 4 in CardsMockData is the seeded KYC_PENDING card.
        vm.onAction(CardDetailAction.Load(4L))
        val loaded = vm.state.value.card.dataOrNull!!
        assertEquals(CardStatus.KYC_PENDING, loaded.status)

        vm.onAction(CardDetailAction.KycVerified)

        val verified = vm.state.value.card.dataOrNull!!
        assertFalse(verified.isKycPending)
        assertEquals(CardStatus.ACTIVE, verified.status)
    }

    @Test
    fun `KycVerified on an already-verified card is a no-op`() {
        val vm = CardDetailViewModel(CardsMockDataProviderFactory.provider("en"))
        // Card id 1 is already ACTIVE / not KYC-pending.
        vm.onAction(CardDetailAction.Load(1L))
        val before = vm.state.value.card.dataOrNull!!

        vm.onAction(CardDetailAction.KycVerified)

        val after = vm.state.value.card.dataOrNull!!
        assertEquals(before.status, after.status)
    }

    // P29.C.2.
    @Test
    fun `submitting a dispute moves the transaction to REJECTED with the chosen reason`() {
        val vm = CardDetailViewModel(CardsMockDataProviderFactory.provider("en"))
        vm.onAction(CardDetailAction.Load(1L))
        val unclaimed = (vm.state.value.transactions as ScreenState.Content).data.first()

        vm.onAction(CardDetailAction.OpenDispute(unclaimed.id))
        assertEquals(unclaimed.id, vm.state.value.disputingTransactionId)
        vm.onAction(CardDetailAction.SubmitDispute("Suspected fraud"))

        assertEquals(null, vm.state.value.disputingTransactionId)
        vm.onAction(CardDetailAction.SelectClaimTab(CardTxnClaimStatus.REJECTED))
        val rejected = (vm.state.value.transactions as ScreenState.Content).data
        assertTrue(rejected.any { it.id == unclaimed.id && it.disputeReason == "Suspected fraud" })
    }

    // P29.C.3.
    @Test
    fun `SetLimit updates the single-transaction and daily limits independently`() {
        val vm = CardDetailViewModel(CardsMockDataProviderFactory.provider("en"))
        vm.onAction(CardDetailAction.Load(1L))

        vm.onAction(CardDetailAction.SetLimit(LimitKind.SINGLE_TRANSACTION, 500.0))
        vm.onAction(CardDetailAction.SetLimit(LimitKind.DAILY, 1500.0))

        val card = card(vm)
        assertEquals(500.0, card.singleTransactionLimit)
        assertEquals(1500.0, card.dailyTransactionLimit)
        assertEquals(null, vm.state.value.limitSheetKind)
    }

    // P29.C.4: card 1 (limit 10000.0) lands in the corporate matrix's upper tier -> 2 steps.
    @Test
    fun `Load derives the approval timeline from the matching ApprovalMatrixModel tier`() {
        val vm = CardDetailViewModel(CardsMockDataProviderFactory.provider("en"))
        vm.onAction(CardDetailAction.Load(1L))

        val steps = vm.state.value.approvalSteps
        assertEquals(2, steps.size)
        assertEquals(ApprovalStepStatus.APPROVED, steps.first().status)
        assertEquals(ApprovalStepStatus.PENDING, steps.last().status)
    }

    // P29.C.5.
    @Test
    fun `card actions accumulate into the local audit log`() {
        val vm = CardDetailViewModel(CardsMockDataProviderFactory.provider("en"))
        vm.onAction(CardDetailAction.Load(1L))
        assertTrue(vm.state.value.auditLog.isEmpty())

        vm.onAction(CardDetailAction.ToggleFreeze)
        vm.onAction(CardDetailAction.SetLimit(LimitKind.DAILY, 2000.0))

        val log = vm.state.value.auditLog
        assertEquals(2, log.size)
        assertEquals("Card frozen", log[0].action)
        assertTrue(log[1].action.contains("daily", ignoreCase = true))
    }

    private fun card(vm: CardDetailViewModel) = (vm.state.value.card as ScreenState.Content).data
}
