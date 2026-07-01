package com.mileway.feature.travel.viewmodel

import com.mileway.feature.travel.repository.TravelSubmissionResult

/**
 * One-shot effect shared by every TR create flow (Trip / Flight / Bus / Hotel / MJP / Visa), the screen routes
 * or toasts on it. Shared because the three rotating outcomes are identical across the travel create suite.
 */
sealed interface TravelCreateEffect {
    data class Success(val id: String) : TravelCreateEffect

    data class NeedsApproval(val id: String) : TravelCreateEffect

    data class Violation(val messages: List<String>) : TravelCreateEffect
}

/** Maps a repository [TravelSubmissionResult] to the shared one-shot [TravelCreateEffect]. */
fun TravelSubmissionResult.toEffect(): TravelCreateEffect =
    when (this) {
        is TravelSubmissionResult.Submitted -> TravelCreateEffect.Success(id)
        is TravelSubmissionResult.NeedsApproval -> TravelCreateEffect.NeedsApproval(id)
        is TravelSubmissionResult.PolicyViolation -> TravelCreateEffect.Violation(messages)
    }
