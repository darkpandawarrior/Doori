package com.miletracker.feature.tracking.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * P-C.4: singleton that bridges app-startup reconciliation to the active ViewModel.
 * Application/scene hook writes here; TrackMilesViewModel observes and presents the
 * restore sheet (P-C.5) when the outcome requires user input.
 */
class ReconciliationResultHolder {
    private val _outcome = MutableStateFlow<SessionReconciliationPolicy.Outcome?>(null)
    val outcome: StateFlow<SessionReconciliationPolicy.Outcome?> = _outcome.asStateFlow()

    fun post(result: SessionReconciliationPolicy.Outcome) {
        _outcome.value = result
    }

    /** Call after the ViewModel has acted on the outcome so it isn't replayed. */
    fun consume() {
        _outcome.value = null
    }
}
