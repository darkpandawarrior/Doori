package com.mileway.core.data.subscription

import com.mileway.core.data.dao.SubscriptionDao
import com.mileway.core.data.model.db.ACTIVE_SUBSCRIPTION_ID
import com.mileway.core.data.model.db.ActiveSubscriptionEntity
import com.mileway.core.data.model.db.SubscriptionPlanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

private const val DAY_MS = 86_400_000L
private const val MONTH_MS = 30L * DAY_MS
private const val YEAR_MS = 365L * DAY_MS

/**
 * PLAN_V24 P6.2: Room-backed subscription store. Plans are seeded once; there is at most one active
 * subscription row (mock purchase — NO payment integration). [cancel] keeps access until the period
 * end (source semantics); [upgrade] swaps the plan on the existing row; [renew] extends the window.
 */
class SubscriptionRepository(private val dao: SubscriptionDao, private val clock: Clock = Clock.System) {
    /** Live plan tiers, low → high (source: plan cards). */
    fun observePlans(): Flow<List<SubscriptionPlan>> = dao.observePlans().map { rows -> rows.map { it.toPlan() } }

    /** The single active subscription, or null when none. */
    fun observeActive(): Flow<ActiveSubscription?> = dao.observeActive().map { it?.toActive() }

    /** Seeds [SubscriptionMockData.plans] on first run only. */
    suspend fun seedIfEmpty() {
        if (dao.planCount() > 0) return
        dao.upsertPlans(SubscriptionMockData.plans.map { it.toEntity() })
    }

    /** Mock purchase of [planId] — creates/replaces the active row as [SubscriptionStatus.ACTIVE]. */
    suspend fun purchase(planId: String) {
        val plan = dao.getPlan(planId) ?: return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertActive(
            ActiveSubscriptionEntity(
                id = ACTIVE_SUBSCRIPTION_ID,
                planId = planId,
                status = SubscriptionStatus.ACTIVE.name,
                startedAtMs = now,
                renewsAtMs = now + windowMsFor(plan.period),
                cancelAtPeriodEnd = false,
            ),
        )
    }

    /** Switch the active subscription to [planId] (keeps the current window; clears any pending cancel). */
    suspend fun upgrade(planId: String) {
        val current = dao.getActive() ?: return purchase(planId)
        dao.getPlan(planId) ?: return
        dao.upsertActive(current.copy(planId = planId, status = SubscriptionStatus.ACTIVE.name, cancelAtPeriodEnd = false))
    }

    /** Cancel — access stays until [ActiveSubscription.renewsAtMs]; only the auto-renew is suppressed. */
    suspend fun cancel() {
        val current = dao.getActive() ?: return
        dao.upsertActive(current.copy(cancelAtPeriodEnd = true))
    }

    /** Renew — extends the window by one period and clears any pending cancel. */
    suspend fun renew() {
        val current = dao.getActive() ?: return
        val plan = dao.getPlan(current.planId) ?: return
        dao.upsertActive(
            current.copy(
                renewsAtMs = current.renewsAtMs + windowMsFor(plan.period),
                status = SubscriptionStatus.ACTIVE.name,
                cancelAtPeriodEnd = false,
            ),
        )
    }

    private fun windowMsFor(periodName: String): Long = if (periodName == SubscriptionPeriod.YEARLY.name) YEAR_MS else MONTH_MS

    private fun SubscriptionPlanEntity.toPlan(): SubscriptionPlan =
        SubscriptionPlan(
            id = id,
            name = name,
            priceAmount = priceAmount,
            period = SubscriptionPeriod.entries.firstOrNull { it.name == period } ?: SubscriptionPeriod.MONTHLY,
            savingsCopy = savingsCopy,
            monthlySavingsAmount = monthlySavingsAmount,
            features = if (featuresCsv.isBlank()) emptyList() else featuresCsv.split("|"),
            tierRank = tierRank,
        )

    private fun SubscriptionPlan.toEntity(): SubscriptionPlanEntity =
        SubscriptionPlanEntity(
            id = id,
            name = name,
            priceAmount = priceAmount,
            period = period.name,
            savingsCopy = savingsCopy,
            monthlySavingsAmount = monthlySavingsAmount,
            featuresCsv = features.joinToString("|"),
            tierRank = tierRank,
        )

    private fun ActiveSubscriptionEntity.toActive(): ActiveSubscription =
        ActiveSubscription(
            planId = planId,
            status = SubscriptionStatus.entries.firstOrNull { it.name == status } ?: SubscriptionStatus.ACTIVE,
            startedAtMs = startedAtMs,
            renewsAtMs = renewsAtMs,
            cancelAtPeriodEnd = cancelAtPeriodEnd,
        )
}
