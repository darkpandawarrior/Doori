package com.mileway.core.data.subscription

/**
 * PLAN_V24 P6.2: seed set for the subscription plans — 3 tiers (source: the reference app's plan cards with
 * price / period / savings copy). Seeded once by [SubscriptionRepository.seedIfEmpty].
 */
internal object SubscriptionMockData {
    val plans: List<SubscriptionPlan> =
        listOf(
            SubscriptionPlan(
                id = "commuter",
                name = "Commuter",
                priceAmount = 299.0,
                period = SubscriptionPeriod.MONTHLY,
                savingsCopy = "Save on every logged trip",
                monthlySavingsAmount = 120.0,
                features =
                    listOf(
                        "Unlimited trip tracking",
                        "Automatic mileage logs",
                        "Monthly expense summary",
                    ),
                tierRank = 0,
            ),
            SubscriptionPlan(
                id = "pro",
                name = "Pro",
                priceAmount = 2999.0,
                period = SubscriptionPeriod.YEARLY,
                savingsCopy = "Save 16% vs monthly",
                monthlySavingsAmount = 260.0,
                features =
                    listOf(
                        "Everything in Commuter",
                        "Priority approvals",
                        "Advanced reports & exports",
                        "Member-only pricing on add-ons",
                    ),
                tierRank = 1,
            ),
            SubscriptionPlan(
                id = "fleet",
                name = "Fleet",
                priceAmount = 5999.0,
                period = SubscriptionPeriod.YEARLY,
                savingsCopy = "Best value for teams",
                monthlySavingsAmount = 540.0,
                features =
                    listOf(
                        "Everything in Pro",
                        "Up to 10 driver seats",
                        "Delegated approvals & payables",
                        "Dedicated support",
                    ),
                tierRank = 2,
            ),
        )
}
