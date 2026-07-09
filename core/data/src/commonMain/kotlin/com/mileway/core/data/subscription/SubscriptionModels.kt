package com.mileway.core.data.subscription

/*
 * PLAN_V24 P6.2: subscription-plan models (per the reference app's subscription plans).
 * Offline/mock — Room-backed via [SubscriptionRepository], purchase is a mock
 * confirm (NO payment integration). Closes part of Phase 6.
 */

/** Billing cadence for a plan. Drives the renewal window and the per-month savings math. */
enum class SubscriptionPeriod { MONTHLY, YEARLY }

/** Lifecycle of the single active subscription row (source: ACTIVE / EXPIRED / CANCELLED). */
enum class SubscriptionStatus { ACTIVE, EXPIRED, CANCELLED }

/**
 * A purchasable plan tier. [priceAmount] is in major currency units (fed straight to
 * `CommonUtils.formatCurrencyAmount`). [monthlySavingsAmount] powers the "savings so far" counter on
 * the active-subscription screen; [savingsCopy] is the marketing line on the plan card.
 */
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val priceAmount: Double,
    val period: SubscriptionPeriod,
    val savingsCopy: String,
    val monthlySavingsAmount: Double,
    val features: List<String>,
    val tierRank: Int,
)

/**
 * The user's single active subscription. [cancelAtPeriodEnd] models the source's "cancel keeps
 * access until period end" semantics — status stays [SubscriptionStatus.ACTIVE] until [renewsAtMs],
 * the flag just suppresses the next auto-renew and drives the "cancels on …" copy.
 */
data class ActiveSubscription(
    val planId: String,
    val status: SubscriptionStatus,
    val startedAtMs: Long,
    val renewsAtMs: Long,
    val cancelAtPeriodEnd: Boolean,
)
