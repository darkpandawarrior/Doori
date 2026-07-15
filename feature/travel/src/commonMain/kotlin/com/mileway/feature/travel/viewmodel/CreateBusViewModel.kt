package com.mileway.feature.travel.viewmodel

import com.mileway.feature.travel.repository.BusDraft
import com.mileway.feature.travel.repository.TravelCreateRepository
import com.mileway.feature.travel.repository.TravelSubmissionResult
import com.siddharth.kmp.mvi.BaseViewModel

data class CreateBusUiState(
    val fromCity: String = "",
    val toCity: String = "",
    val travelDate: String = "",
    val operator: String = "",
    val seatPreference: String = "Seater",
    val isSubmitting: Boolean = false,
    val lastResult: TravelSubmissionResult? = null,
) {
    /** Submit is gated on both cities and a travel date (TR.4 validation). */
    val canSubmit: Boolean
        get() = fromCity.isNotBlank() && toCity.isNotBlank() && travelDate.isNotBlank()
}

sealed interface CreateBusAction {
    data class SetFromCity(val value: String) : CreateBusAction

    data class SetToCity(val value: String) : CreateBusAction

    data class SetTravelDate(val value: String) : CreateBusAction

    data class SetOperator(val value: String) : CreateBusAction

    data class SetSeatPreference(val value: String) : CreateBusAction

    data object Submit : CreateBusAction
}

/** TR.4: Add-Bus reducer on the shared `FormSubmissionScaffold` + [TravelCreateEffect]. */
class CreateBusViewModel(
    private val repository: TravelCreateRepository,
) : BaseViewModel<CreateBusUiState, TravelCreateEffect, CreateBusAction>(CreateBusUiState()) {
    override fun onAction(action: CreateBusAction) {
        when (action) {
            is CreateBusAction.SetFromCity -> setState { copy(fromCity = action.value) }
            is CreateBusAction.SetToCity -> setState { copy(toCity = action.value) }
            is CreateBusAction.SetTravelDate -> setState { copy(travelDate = action.value) }
            is CreateBusAction.SetOperator -> setState { copy(operator = action.value) }
            is CreateBusAction.SetSeatPreference -> setState { copy(seatPreference = action.value) }
            CreateBusAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submitBus(
                BusDraft(
                    fromCity = s.fromCity.trim(),
                    toCity = s.toCity.trim(),
                    travelDate = s.travelDate.trim(),
                    operator = s.operator.trim(),
                    seatPreference = s.seatPreference,
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        emitEffect(result.toEffect())
    }
}
