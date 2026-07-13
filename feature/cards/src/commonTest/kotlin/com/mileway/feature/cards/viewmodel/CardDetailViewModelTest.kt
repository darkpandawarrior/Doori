package com.mileway.feature.cards.viewmodel

import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.cards.model.CardStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
}
