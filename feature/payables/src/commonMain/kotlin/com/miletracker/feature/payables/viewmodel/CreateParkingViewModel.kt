package com.miletracker.feature.payables.viewmodel

import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.payables.repository.ParkMode
import com.miletracker.feature.payables.repository.ParkingDraft
import com.miletracker.feature.payables.repository.ParkingRepository
import com.miletracker.feature.payables.repository.PayablesSubmissionResult

data class CreateParkingUiState(
    val mode: ParkMode = ParkMode.IN,
    val vehicleNumber: String = "",
    val driverName: String = "",
    val gate: String = "",
    val poReference: String = "",
    val remarks: String = "",
    val isSubmitting: Boolean = false,
    val lastResult: PayablesSubmissionResult? = null,
) {
    /** Submit is gated on the vehicle number and the gate (PB.3 validation). */
    val canSubmit: Boolean
        get() = vehicleNumber.isNotBlank() && gate.isNotBlank()
}

sealed interface CreateParkingAction {
    data class SetMode(val value: ParkMode) : CreateParkingAction

    data class SetVehicleNumber(val value: String) : CreateParkingAction

    data class SetDriverName(val value: String) : CreateParkingAction

    data class SetGate(val value: String) : CreateParkingAction

    data class SetPoReference(val value: String) : CreateParkingAction

    data class SetRemarks(val value: String) : CreateParkingAction

    data object Submit : CreateParkingAction
}

sealed interface CreateParkingEffect {
    data class Success(val id: String) : CreateParkingEffect

    data class NeedsApproval(val id: String) : CreateParkingEffect

    data class Violation(val messages: List<String>) : CreateParkingEffect
}

/**
 * PB.3: Create Park In / Park Out reducer. A single create flow with a [ParkMode] segment, driving the shared
 * [com.miletracker.core.ui.components.scaffold.FormSubmissionScaffold]: field setters,
 * [CreateParkingUiState.canSubmit] gating, and a submit that runs the rotating-status fake and emits a one-shot
 * effect (logged / approval / security hold) for the screen to route or toast.
 */
class CreateParkingViewModel(
    private val repository: ParkingRepository,
) : BaseViewModel<CreateParkingUiState, CreateParkingEffect, CreateParkingAction>(CreateParkingUiState()) {
    override fun onAction(action: CreateParkingAction) {
        when (action) {
            is CreateParkingAction.SetMode -> setState { copy(mode = action.value) }
            is CreateParkingAction.SetVehicleNumber -> setState { copy(vehicleNumber = action.value) }
            is CreateParkingAction.SetDriverName -> setState { copy(driverName = action.value) }
            is CreateParkingAction.SetGate -> setState { copy(gate = action.value) }
            is CreateParkingAction.SetPoReference -> setState { copy(poReference = action.value) }
            is CreateParkingAction.SetRemarks -> setState { copy(remarks = action.value) }
            CreateParkingAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submit(
                ParkingDraft(
                    mode = s.mode,
                    vehicleNumber = s.vehicleNumber.trim(),
                    driverName = s.driverName.trim(),
                    gate = s.gate.trim(),
                    poReference = s.poReference.trim(),
                    remarks = s.remarks.trim(),
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        when (result) {
            is PayablesSubmissionResult.Submitted -> emitEffect(CreateParkingEffect.Success(result.id))
            is PayablesSubmissionResult.NeedsApproval -> emitEffect(CreateParkingEffect.NeedsApproval(result.id))
            is PayablesSubmissionResult.PolicyViolation -> emitEffect(CreateParkingEffect.Violation(result.messages))
        }
    }
}
