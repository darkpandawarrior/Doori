package com.mileway.feature.events.viewmodel

import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.events.repository.EventDraft
import com.mileway.feature.events.repository.EventResult
import com.mileway.feature.events.repository.EventsRepository

data class CreateEventUiState(
    val title: String = "",
    val venue: String = "",
    val date: String = "",
    val capacityText: String = "",
    val category: String = "",
    val isSubmitting: Boolean = false,
    val lastResult: EventResult? = null,
) {
    val capacity: Int? get() = capacityText.toIntOrNull()

    /** Submit is gated on a title, a venue, and a positive capacity (EV validation). */
    val canSubmit: Boolean
        get() = title.isNotBlank() && venue.isNotBlank() && (capacity ?: 0) > 0
}

sealed interface CreateEventAction {
    data class SetTitle(val value: String) : CreateEventAction

    data class SetVenue(val value: String) : CreateEventAction

    data class SetDate(val value: String) : CreateEventAction

    data class SetCapacity(val value: String) : CreateEventAction

    data class SetCategory(val value: String) : CreateEventAction

    data object Submit : CreateEventAction
}

sealed interface CreateEventEffect {
    data class Success(val id: String) : CreateEventEffect

    data class NeedsApproval(val id: String) : CreateEventEffect

    data class Violation(val messages: List<String>) : CreateEventEffect
}

/** EV: create-event reducer on the shared `FormSubmissionScaffold`. */
class CreateEventViewModel(
    private val repository: EventsRepository,
) : BaseViewModel<CreateEventUiState, CreateEventEffect, CreateEventAction>(CreateEventUiState()) {
    override fun onAction(action: CreateEventAction) {
        when (action) {
            is CreateEventAction.SetTitle -> setState { copy(title = action.value) }
            is CreateEventAction.SetVenue -> setState { copy(venue = action.value) }
            is CreateEventAction.SetDate -> setState { copy(date = action.value) }
            is CreateEventAction.SetCapacity -> setState { copy(capacityText = action.value) }
            is CreateEventAction.SetCategory -> setState { copy(category = action.value) }
            CreateEventAction.Submit -> submit()
        }
    }

    private fun submit() {
        val s = currentState
        if (!s.canSubmit || s.isSubmitting) return
        setState { copy(isSubmitting = true) }
        val result =
            repository.submit(
                EventDraft(
                    title = s.title.trim(),
                    venue = s.venue.trim(),
                    date = s.date.trim(),
                    capacity = s.capacity ?: 0,
                    category = s.category.trim(),
                ),
            )
        setState { copy(isSubmitting = false, lastResult = result) }
        when (result) {
            is EventResult.Submitted -> emitEffect(CreateEventEffect.Success(result.id))
            is EventResult.NeedsApproval -> emitEffect(CreateEventEffect.NeedsApproval(result.id))
            is EventResult.PolicyViolation -> emitEffect(CreateEventEffect.Violation(result.messages))
        }
    }
}
