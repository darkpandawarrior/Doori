package com.mileway.feature.cards.viewmodel

import com.mileway.core.common.UiText
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.core.ui.mvi.asContent
import com.mileway.feature.cards.data.CardsMockDataProvider
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.cards.model.CardModel
import com.mileway.feature.cards.model.CardShippingAddress
import com.mileway.feature.cards.model.CardStatus
import com.mileway.feature.cards.model.CardTransactionModel
import com.mileway.feature.cards.model.CardTxnClaimStatus

/** Q.3 / Q+.1 / Q+.3, card detail: claim-status transaction tabs + card controls. */
data class CardDetailUiState(
    val card: ScreenState<CardModel> = ScreenState.Loading,
    val transactions: ScreenState<List<CardTransactionModel>> = ScreenState.Loading,
    val claimTab: CardTxnClaimStatus = CardTxnClaimStatus.UNCLAIMED,
    val selectedTransaction: CardTransactionModel? = null,
    val showMonthlyLimitDialog: Boolean = false,
    val showPhysicalCardDialog: Boolean = false,
)

sealed interface CardDetailAction {
    data class Load(val cardId: Long) : CardDetailAction

    data class SelectClaimTab(val status: CardTxnClaimStatus) : CardDetailAction

    data class OpenTransaction(val transaction: CardTransactionModel) : CardDetailAction

    data object DismissTransaction : CardDetailAction

    data class ClaimTransaction(val transactionId: Long) : CardDetailAction

    data object ToggleBlock : CardDetailAction

    data object ToggleFreeze : CardDetailAction

    data object OpenMonthlyLimit : CardDetailAction

    data object DismissMonthlyLimit : CardDetailAction

    data class SetMonthlyLimit(val limit: Double) : CardDetailAction

    data object OpenPhysicalCard : CardDetailAction

    data object DismissPhysicalCard : CardDetailAction

    data class IssuePhysicalCard(val address: CardShippingAddress) : CardDetailAction

    /**
     * P29.C.1: fired when [com.mileway.feature.cards.viewmodel.CardKycViewModel]'s wizard
     * finishes (replaces the old toast-only `ResendKyc` stub — `KycPendingBanner`'s button now
     * navigates straight to the real wizard instead of dispatching a ViewModel action).
     */
    data object KycVerified : CardDetailAction
}

sealed interface CardDetailEffect {
    data class ShowToast(val message: UiText) : CardDetailEffect

    /**
     * P27.E.7: replaces the old claim-transaction toast-stub with real navigation into the
     * expense-entry flow, carrying an [ExpenseSourceContext.Card] built here (not by the nav
     * layer) so feature:cards never depends on feature:logging directly.
     */
    data class NavigateToExpenseEntry(val context: ExpenseSourceContext) : CardDetailEffect
}

class CardDetailViewModel(
    private val provider: CardsMockDataProvider = CardsMockDataProviderFactory.provider(),
) : BaseViewModel<CardDetailUiState, CardDetailEffect, CardDetailAction>(CardDetailUiState()) {
    private var allTransactions: List<CardTransactionModel> = emptyList()

    override fun onAction(action: CardDetailAction) {
        when (action) {
            is CardDetailAction.Load -> load(action.cardId)
            is CardDetailAction.SelectClaimTab ->
                setState { copy(claimTab = action.status, transactions = filtered(action.status)) }
            is CardDetailAction.OpenTransaction -> setState { copy(selectedTransaction = action.transaction) }
            CardDetailAction.DismissTransaction -> setState { copy(selectedTransaction = null) }
            is CardDetailAction.ClaimTransaction -> claimTransaction(action.transactionId)
            CardDetailAction.ToggleBlock -> toggleBlock()
            CardDetailAction.ToggleFreeze -> toggleFreeze()
            CardDetailAction.OpenMonthlyLimit -> setState { copy(showMonthlyLimitDialog = true) }
            CardDetailAction.DismissMonthlyLimit -> setState { copy(showMonthlyLimitDialog = false) }
            is CardDetailAction.SetMonthlyLimit -> setMonthlyLimit(action.limit)
            CardDetailAction.OpenPhysicalCard -> setState { copy(showPhysicalCardDialog = true) }
            CardDetailAction.DismissPhysicalCard -> setState { copy(showPhysicalCardDialog = false) }
            is CardDetailAction.IssuePhysicalCard -> issuePhysicalCard()
            CardDetailAction.KycVerified -> kycVerified()
        }
    }

    /**
     * P29.C.1: the wizard (`CardKycScreen`) reports completion back through nav (see
     * `CardsNavigation`'s savedStateHandle result). Flips [CardModel.isKycPending] the same way
     * [toggleFreeze]/[toggleBlock] mutate the locally-held card — this feature has no Room-backed
     * persistence, so every control here is a session-scoped mutation of the loaded card, not a
     * durable write (consistent with the rest of this ViewModel).
     */
    private fun kycVerified() {
        val current = (currentState.card as? ScreenState.Content)?.data ?: return
        if (!current.isKycPending) return
        mutateCard {
            it.copy(
                isKycPending = false,
                status = if (it.status == CardStatus.KYC_PENDING) CardStatus.ACTIVE else it.status,
            )
        }
        emitEffect(CardDetailEffect.ShowToast(UiText.Static("KYC verified successfully")))
    }

    private fun load(cardId: Long) {
        val card = provider.cardById(cardId)
        allTransactions = provider.transactions(cardId)
        setState {
            copy(
                card = card?.asContent() ?: ScreenState.Error(UiText.Static("Card not found")),
                transactions = filtered(claimTab),
            )
        }
    }

    private fun claimTransaction(transactionId: Long) {
        val txn = allTransactions.find { it.id == transactionId } ?: return
        allTransactions =
            allTransactions.map {
                if (it.id == transactionId) it.copy(claimStatus = CardTxnClaimStatus.CLAIMED) else it
            }
        setState { copy(transactions = filtered(claimTab), selectedTransaction = null) }
        // P27.E.7: real nav to the expense-entry flow, pre-filled/locked from this transaction —
        // replaces the old toast-only stub.
        emitEffect(
            CardDetailEffect.NavigateToExpenseEntry(
                ExpenseSourceContext.Card(
                    cardId = txn.cardId.toString(),
                    transactionId = txn.id.toString(),
                    merchantName = txn.merchantName,
                    transactionAmountRupees = txn.amount,
                ),
            ),
        )
    }

    private fun filtered(status: CardTxnClaimStatus): ScreenState<List<CardTransactionModel>> {
        val list = allTransactions.filter { it.claimStatus == status }
        return if (list.isEmpty()) ScreenState.Empty else list.asContent()
    }

    private fun mutateCard(transform: (CardModel) -> CardModel) {
        val current = (currentState.card as? ScreenState.Content)?.data ?: return
        setState { copy(card = transform(current).asContent()) }
    }

    private fun toggleBlock() {
        val current = (currentState.card as? ScreenState.Content)?.data ?: return
        val blocked = current.status == CardStatus.BLOCKED
        mutateCard { it.copy(status = if (blocked) CardStatus.ACTIVE else CardStatus.BLOCKED) }
        emitEffect(CardDetailEffect.ShowToast(UiText.Static("Card status updated")))
    }

    private fun toggleFreeze() {
        val current = (currentState.card as? ScreenState.Content)?.data ?: return
        val frozen = current.isFrozen
        mutateCard {
            it.copy(
                isFrozen = !frozen,
                status = if (frozen) CardStatus.ACTIVE else CardStatus.FROZEN,
            )
        }
        emitEffect(CardDetailEffect.ShowToast(UiText.Static(if (frozen) "Card unfrozen" else "Card frozen")))
    }

    private fun setMonthlyLimit(limit: Double) {
        mutateCard { it.copy(monthlyLimit = limit, limit = limit) }
        setState { copy(showMonthlyLimitDialog = false) }
        emitEffect(CardDetailEffect.ShowToast(UiText.Static("Limit set successfully")))
    }

    private fun issuePhysicalCard() {
        mutateCard { it.copy(status = CardStatus.PHYSICAL_ISSUED, cardFormat = com.mileway.feature.cards.model.CardFormat.PHYSICAL) }
        setState { copy(showPhysicalCardDialog = false) }
        emitEffect(CardDetailEffect.ShowToast(UiText.Static("Physical Card Request Sent Successfully")))
    }
}
