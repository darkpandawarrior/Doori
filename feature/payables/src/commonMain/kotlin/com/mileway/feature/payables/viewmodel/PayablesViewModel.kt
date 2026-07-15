package com.mileway.feature.payables.viewmodel

import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.payables.model.Invoice
import com.mileway.feature.payables.model.NewLineItemDraft
import com.mileway.feature.payables.model.PurchaseOrder
import com.mileway.feature.payables.repository.PayablesRepository
import com.siddharth.kmp.common.UiText
import com.siddharth.kmp.mvi.BaseViewModel

data class PayablesHomeData(
    val purchaseOrders: List<PurchaseOrder> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
)

data class CreatePoFormState(
    val step: Int = 1,
    val vendorName: String = "",
    val deliveryDate: String = "",
    val officeLocation: String = "Head Office – Pune",
    val lineItems: List<NewLineItemDraft> = listOf(NewLineItemDraft()),
)

data class PayablesUiState(
    val homeState: ScreenState<PayablesHomeData> = ScreenState.Loading,
    val form: CreatePoFormState = CreatePoFormState(),
    val lastSubmittedId: String = "",
    val detailState: ScreenState<PurchaseOrder> = ScreenState.Empty,
)

sealed interface PayablesAction {
    data object Refresh : PayablesAction

    data class SetVendorName(val name: String) : PayablesAction

    data class SetDeliveryDate(val date: String) : PayablesAction

    data class SetOfficeLocation(val location: String) : PayablesAction

    data object GoToStep1 : PayablesAction

    data object GoToStep2 : PayablesAction

    data object AddLineItem : PayablesAction

    data class RemoveLineItem(val index: Int) : PayablesAction

    data class UpdateLineItem(val index: Int, val item: NewLineItemDraft) : PayablesAction

    data object SubmitPo : PayablesAction

    data object ResetForm : PayablesAction

    data class OpenDetail(val id: String) : PayablesAction

    data class ShowMessage(val message: String) : PayablesAction
}

sealed interface PayablesEffect {
    data class ShowToast(val message: UiText) : PayablesEffect

    data class NavigateToSuccess(val poId: String) : PayablesEffect

    data object NavigateBack : PayablesEffect
}

class PayablesViewModel(
    private val repository: PayablesRepository,
) : BaseViewModel<PayablesUiState, PayablesEffect, PayablesAction>(
        PayablesUiState(
            homeState =
                ScreenState.Content(
                    PayablesHomeData(repository.purchaseOrders, repository.invoices),
                ),
        ),
    ) {
    val officeLocations =
        listOf(
            "Head Office – Pune",
            "North Branch – Mumbai",
            "South Branch – Bengaluru",
            "East Branch – Kolkata",
        )

    override fun onAction(action: PayablesAction) {
        when (action) {
            PayablesAction.Refresh ->
                setState {
                    copy(
                        homeState =
                            ScreenState.Content(
                                PayablesHomeData(repository.purchaseOrders, repository.invoices),
                            ),
                    )
                }
            is PayablesAction.SetVendorName -> updateForm { copy(vendorName = action.name) }
            is PayablesAction.SetDeliveryDate -> updateForm { copy(deliveryDate = action.date) }
            is PayablesAction.SetOfficeLocation -> updateForm { copy(officeLocation = action.location) }
            PayablesAction.GoToStep1 -> updateForm { copy(step = 1) }
            PayablesAction.GoToStep2 -> updateForm { copy(step = 2) }
            PayablesAction.AddLineItem -> updateForm { copy(lineItems = lineItems + NewLineItemDraft()) }
            is PayablesAction.RemoveLineItem ->
                updateForm {
                    copy(lineItems = lineItems.toMutableList().also { it.removeAt(action.index) })
                }
            is PayablesAction.UpdateLineItem ->
                updateForm {
                    copy(lineItems = lineItems.toMutableList().also { it[action.index] = action.item })
                }
            PayablesAction.SubmitPo -> submitPo()
            PayablesAction.ResetForm -> setState { copy(form = CreatePoFormState(), lastSubmittedId = "") }
            is PayablesAction.OpenDetail -> openDetail(action.id)
            is PayablesAction.ShowMessage -> emitEffect(PayablesEffect.ShowToast(UiText.Dynamic(action.message)))
        }
    }

    private fun updateForm(reducer: CreatePoFormState.() -> CreatePoFormState) {
        setState { copy(form = form.reducer()) }
    }

    private fun submitPo() {
        val form = currentState.form
        val id = "PO-NEW-${(form.vendorName.hashCode() and 0x7FFF_FFFF) % 9000 + 1000}"
        setState { copy(lastSubmittedId = id) }
        emitEffect(PayablesEffect.NavigateToSuccess(id))
    }

    private fun openDetail(id: String) {
        val po = repository.getPoById(id)
        setState {
            copy(detailState = po?.let { ScreenState.Content(it) } ?: ScreenState.Empty)
        }
    }
}
