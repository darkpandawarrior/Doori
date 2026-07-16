package com.mileway.feature.advances.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.feature.advances.data.QrCardsRepository
import com.mileway.feature.advances.model.AdvanceType
import com.mileway.feature.advances.model.QrCard
import com.mileway.feature.advances.validation.QrRequestError
import com.mileway.feature.advances.validation.QrRequestValidator
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * PLAN_V35.P4: QR-card request form. [mandatoryCardSelection] mirrors the reference app's plugin
 * flag of the same name — a constructor default until PluginRegistry wiring lands (separate task),
 * same deferral as [AskAdvanceViewModel.requireDateRange].
 */
data class QrRequestUiState(
    val typeEnabled: Boolean = false,
    val types: List<AdvanceType> = emptyList(),
    val selectedType: String? = null,
    val amountText: String = "",
    val title: String = "",
    val description: String = "",
    val mandatoryCardSelection: Boolean = true,
    val cards: List<QrCard> = emptyList(),
    val selectedCardId: Long? = null,
    val declarationAccepted: Boolean = false,
    val errors: List<QrRequestError> = emptyList(),
    val isSubmitting: Boolean = false,
    val submittedPermissionId: Long? = null,
) {
    val isSuccess: Boolean get() = (submittedPermissionId ?: 0L) > 0L
}

sealed interface QrRequestAction {
    data class SelectType(val type: String) : QrRequestAction

    data class SetAmount(val text: String) : QrRequestAction

    data class SetTitle(val value: String) : QrRequestAction

    data class SetDescription(val value: String) : QrRequestAction

    data class SelectCard(val cardId: Long) : QrRequestAction

    data class SetDeclaration(val accepted: Boolean) : QrRequestAction

    data object Submit : QrRequestAction
}

sealed interface QrRequestEffect

class QrRequestViewModel(
    private val repository: QrCardsRepository,
    mandatoryCardSelection: Boolean = true,
) : BaseViewModel<QrRequestUiState, QrRequestEffect, QrRequestAction>(
        run {
            val (typeEnabled, types) = repository.qrTypes()
            QrRequestUiState(typeEnabled = typeEnabled, types = types, mandatoryCardSelection = mandatoryCardSelection)
        },
    ) {
    init {
        viewModelScope.launch {
            repository.activeQrCards().collectLatest { cards -> setState { copy(cards = cards) } }
        }
    }

    override fun onAction(action: QrRequestAction) {
        when (action) {
            is QrRequestAction.SelectType -> setState { copy(selectedType = action.type) }
            is QrRequestAction.SetAmount -> setState { copy(amountText = action.text) }
            is QrRequestAction.SetTitle -> setState { copy(title = action.value) }
            is QrRequestAction.SetDescription -> setState { copy(description = action.value) }
            is QrRequestAction.SelectCard -> setState { copy(selectedCardId = action.cardId) }
            is QrRequestAction.SetDeclaration -> setState { copy(declarationAccepted = action.accepted) }
            QrRequestAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (s.isSubmitting) return
        val amount = s.amountText.toDoubleOrNull() ?: 0.0
        val errors =
            QrRequestValidator.validate(
                amount = amount,
                title = s.title,
                description = s.description,
                type = s.selectedType,
                typeEnabled = s.typeEnabled,
                cardSelected = s.selectedCardId != null,
                mandatoryCardSelection = s.mandatoryCardSelection,
                cardsExist = s.cards.isNotEmpty(),
                declarationAccepted = s.declarationAccepted,
            )
        setState { copy(errors = errors) }
        if (errors.isNotEmpty()) return

        setState { copy(isSubmitting = true) }
        viewModelScope.launch {
            val result =
                repository.submitQrRequest(
                    type = s.selectedType,
                    amount = amount,
                    title = s.title.trim(),
                    description = s.description.trim(),
                    cardId = s.selectedCardId?.toString(),
                    declarationAccepted = s.declarationAccepted,
                )
            setState {
                copy(
                    isSubmitting = false,
                    submittedPermissionId = result.getOrNull()?.permissionId,
                )
            }
        }
    }
}
