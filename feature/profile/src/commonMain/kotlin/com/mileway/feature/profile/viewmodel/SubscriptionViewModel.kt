package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.subscription.ActiveSubscription
import com.mileway.core.data.subscription.SubscriptionPlan
import com.mileway.core.data.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val MONTH_MS = 30L * 86_400_000L

/**
 * PLAN_V24 P6.2: subscription plans + the single active subscription. Seeds the plan catalogue on
 * first observe. [savingsSoFar] is the "savings so far" counter — the active plan's per-month
 * savings times the number of active periods (computed at emit time; a demo snapshot, not a live
 * ticker). All mutations are mock (NO payment integration).
 */
data class SubscriptionUiState(
    val plans: List<SubscriptionPlan> = emptyList(),
    val active: ActiveSubscription? = null,
    val activePlan: SubscriptionPlan? = null,
    val savingsSoFar: Double = 0.0,
)

class SubscriptionViewModel(
    private val repository: SubscriptionRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    val state: StateFlow<SubscriptionUiState> =
        combine(repository.observePlans(), repository.observeActive()) { plans, active ->
            val activePlan = active?.let { a -> plans.firstOrNull { it.id == a.planId } }
            SubscriptionUiState(
                plans = plans,
                active = active,
                activePlan = activePlan,
                savingsSoFar = savingsFor(active, activePlan),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SubscriptionUiState())

    /** Mock purchase of [planId] (creates the active subscription). */
    fun purchase(planId: String) {
        viewModelScope.launch { repository.purchase(planId) }
    }

    /** Switch to [planId] on the existing subscription. */
    fun upgrade(planId: String) {
        viewModelScope.launch { repository.upgrade(planId) }
    }

    /** Cancel — access stays until the period end. */
    fun cancel() {
        viewModelScope.launch { repository.cancel() }
    }

    /** Renew — extends the window by one period. */
    fun renew() {
        viewModelScope.launch { repository.renew() }
    }

    private fun savingsFor(
        active: ActiveSubscription?,
        plan: SubscriptionPlan?,
    ): Double {
        if (active == null || plan == null) return 0.0
        val elapsed = clock.now().toEpochMilliseconds() - active.startedAtMs
        val periods = (elapsed / MONTH_MS).toInt().coerceAtLeast(0) + 1
        return plan.monthlySavingsAmount * periods
    }
}
